use std::collections::HashMap;
use std::path::Path;

use anyhow::{Context, Result};
use glam::Mat4;
use thiserror::Error;
use wgpu::util::DeviceExt;
use wgpu::{CompositeAlphaMode, PresentMode, SurfaceError};
use winit::dpi::PhysicalSize;
use winit::window::Window;

use crate::hud::{HudUniform, HudVertex};
use crate::mesh::{ChunkMesh, MeshVertex};
use crate::streaming::{ChunkDebugState, ChunkResidencyState};
use crate::world::ChunkPos;

const DEPTH_FORMAT: wgpu::TextureFormat = wgpu::TextureFormat::Depth32Float;

pub struct Renderer<'w> {
    surface: wgpu::Surface<'w>,
    device: wgpu::Device,
    queue: wgpu::Queue,
    config: wgpu::SurfaceConfiguration,
    size: PhysicalSize<u32>,
    render_pipeline: wgpu::RenderPipeline,
    debug_line_pipeline: wgpu::RenderPipeline,
    camera_buffer: wgpu::Buffer,
    camera_bind_group: wgpu::BindGroup,
    terrain_bind_group: wgpu::BindGroup,
    depth_texture: wgpu::Texture,
    depth_view: wgpu::TextureView,
    scene_mesh: Option<SceneMeshGpu>,
    chunk_meshes: HashMap<ChunkPos, SceneMeshGpu>,
    chunk_debug_states: HashMap<ChunkPos, ChunkDebugState>,
    chunk_border_mesh: Option<DebugLineMeshGpu>,
    chunk_border_mesh_dirty: bool,
    chunk_border_debug_enabled: bool,
    camera_frustum: Option<FrustumPlanes>,
    visible_chunk_meshes: usize,
    hud_pipeline: wgpu::RenderPipeline,
    hud_uniform_buffer: wgpu::Buffer,
    hud_bind_group: wgpu::BindGroup,
    hud_vertex_buffer: Option<wgpu::Buffer>,
    hud_vertex_count: u32,
}

struct SceneMeshGpu {
    vertex_buffer: wgpu::Buffer,
    index_buffer: wgpu::Buffer,
    index_count: u32,
}

struct DebugLineMeshGpu {
    vertex_buffer: wgpu::Buffer,
    vertex_count: u32,
}

#[derive(Debug, Clone, Copy)]
struct FrustumPlanes {
    planes: [[f32; 4]; 6],
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct DebugLineVertex {
    position: [f32; 3],
    color: [f32; 3],
}

impl DebugLineVertex {
    const ATTRS: [wgpu::VertexAttribute; 2] =
        wgpu::vertex_attr_array![0 => Float32x3, 1 => Float32x3];

    #[must_use]
    fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct CameraUniform {
    view_proj: [[f32; 4]; 4],
}

#[derive(Debug, Error)]
pub enum RenderError {
    #[error("surface is out of memory")]
    OutOfMemory,
    #[error("surface timeout")]
    Timeout,
}

impl<'w> Renderer<'w> {
    pub async fn new(window: &'w Window) -> Result<Self> {
        let size = window.inner_size();

        let instance = wgpu::Instance::default();
        let surface = instance
            .create_surface(window)
            .context("failed to create WGPU surface")?;

        let adapter = instance
            .request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: Some(&surface),
                force_fallback_adapter: false,
            })
            .await
            .context("failed to find a GPU adapter")?;

        let (device, queue) = adapter
            .request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("thingcraft-device"),
                    required_features: wgpu::Features::empty(),
                    required_limits: wgpu::Limits::default(),
                },
                None,
            )
            .await
            .context("failed to request logical device")?;

        let surface_caps = surface.get_capabilities(&adapter);
        let surface_format = surface_caps
            .formats
            .iter()
            .copied()
            .find(wgpu::TextureFormat::is_srgb)
            .unwrap_or(surface_caps.formats[0]);

        let present_mode = surface_caps
            .present_modes
            .iter()
            .copied()
            .find(|mode| *mode == PresentMode::Mailbox)
            .unwrap_or(PresentMode::Fifo);

        let alpha_mode = surface_caps
            .alpha_modes
            .iter()
            .copied()
            .find(|mode| *mode == CompositeAlphaMode::Opaque)
            .unwrap_or(surface_caps.alpha_modes[0]);

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format: surface_format,
            width: size.width.max(1),
            height: size.height.max(1),
            present_mode,
            alpha_mode,
            view_formats: vec![],
            desired_maximum_frame_latency: 2,
        };

        surface.configure(&device, &config);

        let (depth_texture, depth_view) = create_depth_resources(&device, &config);

        let camera_uniform = CameraUniform {
            view_proj: glam::Mat4::IDENTITY.to_cols_array_2d(),
        };
        let camera_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("thingcraft-camera-buffer"),
            contents: bytemuck::bytes_of(&camera_uniform),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });

        let camera_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-camera-bind-group-layout"),
                entries: &[wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::VERTEX,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                }],
            });

        let camera_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("thingcraft-camera-bind-group"),
            layout: &camera_bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: camera_buffer.as_entire_binding(),
            }],
        });

        let terrain_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-terrain-bind-group-layout"),
                entries: &[
                    wgpu::BindGroupLayoutEntry {
                        binding: 0,
                        visibility: wgpu::ShaderStages::FRAGMENT,
                        ty: wgpu::BindingType::Texture {
                            sample_type: wgpu::TextureSampleType::Float { filterable: true },
                            view_dimension: wgpu::TextureViewDimension::D2,
                            multisampled: false,
                        },
                        count: None,
                    },
                    wgpu::BindGroupLayoutEntry {
                        binding: 1,
                        visibility: wgpu::ShaderStages::FRAGMENT,
                        ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                        count: None,
                    },
                ],
            });

        let terrain_bind_group = create_terrain_bind_group(
            &device,
            &queue,
            &terrain_bind_group_layout,
            Path::new("resources/minecraft-a1.2.6-client/terrain.png"),
        );

        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-chunk-shader"),
            source: wgpu::ShaderSource::Wgsl(CHUNK_SHADER.into()),
        });

        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("thingcraft-pipeline-layout"),
            bind_group_layouts: &[&camera_bind_group_layout, &terrain_bind_group_layout],
            push_constant_ranges: &[],
        });

        let render_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-render-pipeline"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: "vs_main",
                buffers: &[MeshVertex::layout()],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            },
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                strip_index_format: None,
                front_face: wgpu::FrontFace::Ccw,
                cull_mode: Some(wgpu::Face::Back),
                polygon_mode: wgpu::PolygonMode::Fill,
                unclipped_depth: false,
                conservative: false,
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: DEPTH_FORMAT,
                depth_write_enabled: true,
                depth_compare: wgpu::CompareFunction::Less,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState {
                count: 1,
                mask: !0,
                alpha_to_coverage_enabled: false,
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format: config.format,
                    blend: Some(wgpu::BlendState::REPLACE),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            }),
            multiview: None,
        });

        let debug_line_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-debug-line-shader"),
            source: wgpu::ShaderSource::Wgsl(DEBUG_LINE_SHADER.into()),
        });

        let debug_line_pipeline_layout =
            device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                label: Some("thingcraft-debug-line-pipeline-layout"),
                bind_group_layouts: &[&camera_bind_group_layout],
                push_constant_ranges: &[],
            });

        let debug_line_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-debug-line-pipeline"),
            layout: Some(&debug_line_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &debug_line_shader,
                entry_point: "vs_main",
                buffers: &[DebugLineVertex::layout()],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            },
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::LineList,
                strip_index_format: None,
                front_face: wgpu::FrontFace::Ccw,
                cull_mode: None,
                polygon_mode: wgpu::PolygonMode::Fill,
                unclipped_depth: false,
                conservative: false,
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: DEPTH_FORMAT,
                depth_write_enabled: false,
                depth_compare: wgpu::CompareFunction::LessEqual,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState {
                count: 1,
                mask: !0,
                alpha_to_coverage_enabled: false,
            },
            fragment: Some(wgpu::FragmentState {
                module: &debug_line_shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format: config.format,
                    blend: Some(wgpu::BlendState::REPLACE),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            }),
            multiview: None,
        });

        // HUD pipeline: 2D overlay with orthographic projection, no depth test.
        let hud_uniform = HudUniform {
            screen_width: config.width as f32,
            screen_height: config.height as f32,
            _pad: [0.0; 2],
        };
        let hud_uniform_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("thingcraft-hud-uniform-buffer"),
            contents: bytemuck::bytes_of(&hud_uniform),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });

        let hud_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-hud-bind-group-layout"),
                entries: &[wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::VERTEX,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                }],
            });

        let hud_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("thingcraft-hud-bind-group"),
            layout: &hud_bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: hud_uniform_buffer.as_entire_binding(),
            }],
        });

        let hud_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-hud-shader"),
            source: wgpu::ShaderSource::Wgsl(HUD_SHADER.into()),
        });

        let hud_pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("thingcraft-hud-pipeline-layout"),
            bind_group_layouts: &[&hud_bind_group_layout],
            push_constant_ranges: &[],
        });

        let hud_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-hud-pipeline"),
            layout: Some(&hud_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &hud_shader,
                entry_point: "vs_main",
                buffers: &[HudVertex::layout()],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            },
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::TriangleList,
                strip_index_format: None,
                front_face: wgpu::FrontFace::Ccw,
                cull_mode: None,
                polygon_mode: wgpu::PolygonMode::Fill,
                unclipped_depth: false,
                conservative: false,
            },
            depth_stencil: None, // No depth test for HUD.
            multisample: wgpu::MultisampleState {
                count: 1,
                mask: !0,
                alpha_to_coverage_enabled: false,
            },
            fragment: Some(wgpu::FragmentState {
                module: &hud_shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format: config.format,
                    blend: Some(wgpu::BlendState::ALPHA_BLENDING),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            }),
            multiview: None,
        });

        Ok(Self {
            surface,
            device,
            queue,
            config,
            size,
            render_pipeline,
            debug_line_pipeline,
            camera_buffer,
            camera_bind_group,
            terrain_bind_group,
            depth_texture,
            depth_view,
            scene_mesh: None,
            chunk_meshes: HashMap::new(),
            chunk_debug_states: HashMap::new(),
            chunk_border_mesh: None,
            chunk_border_mesh_dirty: true,
            chunk_border_debug_enabled: false,
            camera_frustum: None,
            visible_chunk_meshes: 0,
            hud_pipeline,
            hud_uniform_buffer,
            hud_bind_group,
            hud_vertex_buffer: None,
            hud_vertex_count: 0,
        })
    }

    pub fn viewport_aspect(&self) -> f32 {
        if self.size.height == 0 {
            1.0
        } else {
            self.size.width as f32 / self.size.height as f32
        }
    }

    pub fn update_camera(&mut self, view_proj: [[f32; 4]; 4]) {
        let uniform = CameraUniform { view_proj };
        self.queue
            .write_buffer(&self.camera_buffer, 0, bytemuck::bytes_of(&uniform));
        let matrix = Mat4::from_cols_array_2d(&view_proj);
        self.camera_frustum = Some(FrustumPlanes::from_view_proj(matrix));
    }

    pub fn set_scene_mesh(&mut self, mesh: &ChunkMesh) {
        if mesh.vertices.is_empty() || mesh.indices.is_empty() {
            self.scene_mesh = None;
            return;
        }

        let vertex_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-scene-vertex-buffer"),
                contents: bytemuck::cast_slice(&mesh.vertices),
                usage: wgpu::BufferUsages::VERTEX,
            });

        let index_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-scene-index-buffer"),
                contents: bytemuck::cast_slice(&mesh.indices),
                usage: wgpu::BufferUsages::INDEX,
            });

        self.scene_mesh = Some(SceneMeshGpu {
            vertex_buffer,
            index_buffer,
            index_count: mesh.indices.len() as u32,
        });
    }

    pub fn upsert_chunk_mesh(&mut self, pos: ChunkPos, mesh: &ChunkMesh) {
        if mesh.vertices.is_empty() || mesh.indices.is_empty() {
            self.chunk_meshes.remove(&pos);
            self.chunk_border_mesh_dirty = true;
            return;
        }

        let vertex_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-chunk-vertex-buffer"),
                contents: bytemuck::cast_slice(&mesh.vertices),
                usage: wgpu::BufferUsages::VERTEX,
            });

        let index_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-chunk-index-buffer"),
                contents: bytemuck::cast_slice(&mesh.indices),
                usage: wgpu::BufferUsages::INDEX,
            });

        self.chunk_meshes.insert(
            pos,
            SceneMeshGpu {
                vertex_buffer,
                index_buffer,
                index_count: mesh.indices.len() as u32,
            },
        );
        self.chunk_border_mesh_dirty = true;
    }

    pub fn remove_chunk_mesh(&mut self, pos: ChunkPos) {
        self.chunk_meshes.remove(&pos);
        self.chunk_border_mesh_dirty = true;
    }

    #[must_use]
    pub fn chunk_mesh_count(&self) -> usize {
        self.chunk_meshes.len()
    }

    #[must_use]
    pub fn visible_chunk_count(&self) -> usize {
        self.visible_chunk_meshes
    }

    pub fn set_chunk_border_debug(&mut self, enabled: bool) {
        self.chunk_border_debug_enabled = enabled;
    }

    pub fn set_chunk_debug_states(&mut self, states: Vec<ChunkDebugState>) {
        self.chunk_debug_states.clear();
        self.chunk_debug_states
            .extend(states.into_iter().map(|state| (state.pos, state)));
        self.chunk_border_mesh_dirty = true;
    }

    #[must_use]
    pub fn chunk_border_debug_enabled(&self) -> bool {
        self.chunk_border_debug_enabled
    }

    pub fn resize(&mut self, new_size: PhysicalSize<u32>) {
        self.size = new_size;
        if self.size.width == 0 || self.size.height == 0 {
            return;
        }

        self.config.width = self.size.width;
        self.config.height = self.size.height;
        self.surface.configure(&self.device, &self.config);
        let (depth_texture, depth_view) = create_depth_resources(&self.device, &self.config);
        self.depth_texture = depth_texture;
        self.depth_view = depth_view;
        self.update_hud_uniform();
    }

    /// Upload new HUD vertices. Call each frame (or when hotbar state changes).
    pub fn update_hud(&mut self, vertices: &[HudVertex]) {
        if vertices.is_empty() {
            self.hud_vertex_buffer = None;
            self.hud_vertex_count = 0;
            return;
        }

        let buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-hud-vertex-buffer"),
                contents: bytemuck::cast_slice(vertices),
                usage: wgpu::BufferUsages::VERTEX,
            });

        self.hud_vertex_buffer = Some(buffer);
        self.hud_vertex_count = vertices.len() as u32;
    }

    pub fn screen_size(&self) -> (f32, f32) {
        (self.size.width as f32, self.size.height as f32)
    }

    fn update_hud_uniform(&mut self) {
        let uniform = HudUniform {
            screen_width: self.size.width as f32,
            screen_height: self.size.height as f32,
            _pad: [0.0; 2],
        };
        self.queue
            .write_buffer(&self.hud_uniform_buffer, 0, bytemuck::bytes_of(&uniform));
    }

    pub fn reconfigure(&mut self) {
        if self.config.width == 0 || self.config.height == 0 {
            return;
        }
        self.surface.configure(&self.device, &self.config);
        let (depth_texture, depth_view) = create_depth_resources(&self.device, &self.config);
        self.depth_texture = depth_texture;
        self.depth_view = depth_view;
    }

    pub fn render(&mut self) -> Result<(), RenderError> {
        if self.size.width == 0 || self.size.height == 0 {
            return Ok(());
        }

        self.refresh_chunk_border_mesh_if_dirty();

        let output = self
            .surface
            .get_current_texture()
            .map_err(|err| match err {
                SurfaceError::OutOfMemory => RenderError::OutOfMemory,
                SurfaceError::Timeout => RenderError::Timeout,
                SurfaceError::Lost | SurfaceError::Outdated => {
                    self.reconfigure();
                    RenderError::Timeout
                }
            })?;

        let view = output
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        let mut encoder = self
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                label: Some("thingcraft-render-encoder"),
            });

        {
            let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("thingcraft-render-pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.04,
                            g: 0.07,
                            b: 0.12,
                            a: 1.0,
                        }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                    view: &self.depth_view,
                    depth_ops: Some(wgpu::Operations {
                        load: wgpu::LoadOp::Clear(1.0),
                        store: wgpu::StoreOp::Store,
                    }),
                    stencil_ops: None,
                }),
                timestamp_writes: None,
                occlusion_query_set: None,
            });

            if let Some(scene_mesh) = &self.scene_mesh {
                render_pass.set_pipeline(&self.render_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_bind_group(1, &self.terrain_bind_group, &[]);
                render_pass.set_vertex_buffer(0, scene_mesh.vertex_buffer.slice(..));
                render_pass
                    .set_index_buffer(scene_mesh.index_buffer.slice(..), wgpu::IndexFormat::Uint32);
                render_pass.draw_indexed(0..scene_mesh.index_count, 0, 0..1);
            }

            if !self.chunk_meshes.is_empty() {
                render_pass.set_pipeline(&self.render_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_bind_group(1, &self.terrain_bind_group, &[]);
                let frustum = self.camera_frustum.as_ref();
                let mut visible = 0_usize;
                for (pos, chunk_mesh) in &self.chunk_meshes {
                    if frustum.is_some_and(|view| !view.intersects_chunk(*pos)) {
                        continue;
                    }

                    render_pass.set_vertex_buffer(0, chunk_mesh.vertex_buffer.slice(..));
                    render_pass.set_index_buffer(
                        chunk_mesh.index_buffer.slice(..),
                        wgpu::IndexFormat::Uint32,
                    );
                    render_pass.draw_indexed(0..chunk_mesh.index_count, 0, 0..1);
                    visible += 1;
                }
                self.visible_chunk_meshes = visible;
            } else {
                self.visible_chunk_meshes = 0;
            }

            if self.chunk_border_debug_enabled {
                if let Some(chunk_border_mesh) = &self.chunk_border_mesh {
                    render_pass.set_pipeline(&self.debug_line_pipeline);
                    render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                    render_pass.set_vertex_buffer(0, chunk_border_mesh.vertex_buffer.slice(..));
                    render_pass.draw(0..chunk_border_mesh.vertex_count, 0..1);
                }
            }
        }

        // HUD pass: draw 2D overlay on top of the scene (no depth test, alpha blending).
        if let Some(hud_vb) = &self.hud_vertex_buffer {
            let mut hud_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("thingcraft-hud-pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Load,
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: None,
                timestamp_writes: None,
                occlusion_query_set: None,
            });

            hud_pass.set_pipeline(&self.hud_pipeline);
            hud_pass.set_bind_group(0, &self.hud_bind_group, &[]);
            hud_pass.set_vertex_buffer(0, hud_vb.slice(..));
            hud_pass.draw(0..self.hud_vertex_count, 0..1);
        }

        self.queue.submit(std::iter::once(encoder.finish()));
        output.present();
        Ok(())
    }

    fn refresh_chunk_border_mesh_if_dirty(&mut self) {
        if !self.chunk_border_mesh_dirty {
            return;
        }
        self.chunk_border_mesh_dirty = false;

        if self.chunk_meshes.is_empty() && self.chunk_debug_states.is_empty() {
            self.chunk_border_mesh = None;
            return;
        }

        let mut vertices = build_chunk_border_vertices(self.chunk_meshes.keys().copied());
        vertices.extend(build_chunk_status_vertices(
            self.chunk_debug_states.values().copied(),
        ));
        if vertices.is_empty() {
            self.chunk_border_mesh = None;
            return;
        }

        let vertex_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-chunk-border-vertex-buffer"),
                contents: bytemuck::cast_slice(&vertices),
                usage: wgpu::BufferUsages::VERTEX,
            });

        self.chunk_border_mesh = Some(DebugLineMeshGpu {
            vertex_buffer,
            vertex_count: vertices.len() as u32,
        });
    }
}

fn build_chunk_border_vertices<I>(chunk_positions: I) -> Vec<DebugLineVertex>
where
    I: Iterator<Item = ChunkPos>,
{
    let mut vertices = Vec::new();
    for pos in chunk_positions {
        let color = if (pos.x + pos.z) & 1 == 0 {
            [0.1, 1.0, 0.2]
        } else {
            [1.0, 0.6, 0.1]
        };
        append_chunk_border_box(&mut vertices, pos, color);
    }
    vertices
}

fn build_chunk_status_vertices<I>(states: I) -> Vec<DebugLineVertex>
where
    I: Iterator<Item = ChunkDebugState>,
{
    let mut vertices = Vec::new();
    for state in states {
        append_chunk_status_bars(&mut vertices, state);
    }
    vertices
}

fn append_chunk_status_bars(vertices: &mut Vec<DebugLineVertex>, state: ChunkDebugState) {
    let base_x = state.pos.x as f32 * 16.0 + 8.0;
    let base_z = state.pos.z as f32 * 16.0 + 8.0;

    let generation_active =
        state.in_flight_generation || state.residency_state == ChunkResidencyState::Generating;
    let lighting_active = state.in_flight_lighting || state.dirty_lighting;
    let meshing_active = state.in_flight_meshing
        || state.dirty_geometry
        || state.residency_state == ChunkResidencyState::Meshing;

    let generation_color = if generation_active {
        [0.3, 0.55, 1.0]
    } else {
        [0.08, 0.12, 0.25]
    };
    let lighting_color = if lighting_active {
        [1.0, 0.25, 0.25]
    } else {
        [0.2, 0.1, 0.1]
    };
    let meshing_color = if meshing_active {
        [1.0, 0.75, 0.15]
    } else {
        [0.2, 0.12, 0.05]
    };

    let generation_height = if generation_active { 12.0 } else { 2.5 };
    let lighting_height = if lighting_active { 12.0 } else { 2.5 };
    let meshing_height = if meshing_active { 12.0 } else { 2.5 };

    append_vertical_bar(
        vertices,
        [base_x - 2.0, 0.2, base_z],
        generation_height,
        generation_color,
    );
    append_vertical_bar(
        vertices,
        [base_x, 0.2, base_z],
        lighting_height,
        lighting_color,
    );
    append_vertical_bar(
        vertices,
        [base_x + 2.0, 0.2, base_z],
        meshing_height,
        meshing_color,
    );
}

fn append_vertical_bar(
    vertices: &mut Vec<DebugLineVertex>,
    base: [f32; 3],
    height: f32,
    color: [f32; 3],
) {
    append_line(vertices, base, [base[0], base[1] + height, base[2]], color);
}

fn append_chunk_border_box(vertices: &mut Vec<DebugLineVertex>, pos: ChunkPos, color: [f32; 3]) {
    let (min, max) = chunk_aabb(pos);
    let p000 = [min[0], min[1], min[2]];
    let p100 = [max[0], min[1], min[2]];
    let p110 = [max[0], min[1], max[2]];
    let p010 = [min[0], min[1], max[2]];
    let p001 = [min[0], max[1], min[2]];
    let p101 = [max[0], max[1], min[2]];
    let p111 = [max[0], max[1], max[2]];
    let p011 = [min[0], max[1], max[2]];

    append_line(vertices, p000, p100, color);
    append_line(vertices, p100, p110, color);
    append_line(vertices, p110, p010, color);
    append_line(vertices, p010, p000, color);

    append_line(vertices, p001, p101, color);
    append_line(vertices, p101, p111, color);
    append_line(vertices, p111, p011, color);
    append_line(vertices, p011, p001, color);

    append_line(vertices, p000, p001, color);
    append_line(vertices, p100, p101, color);
    append_line(vertices, p110, p111, color);
    append_line(vertices, p010, p011, color);
}

fn append_line(vertices: &mut Vec<DebugLineVertex>, from: [f32; 3], to: [f32; 3], color: [f32; 3]) {
    vertices.push(DebugLineVertex {
        position: from,
        color,
    });
    vertices.push(DebugLineVertex {
        position: to,
        color,
    });
}

impl FrustumPlanes {
    fn from_view_proj(view_proj: Mat4) -> Self {
        let m = view_proj.to_cols_array();
        let row1 = [m[0], m[4], m[8], m[12]];
        let row2 = [m[1], m[5], m[9], m[13]];
        let row3 = [m[2], m[6], m[10], m[14]];
        let row4 = [m[3], m[7], m[11], m[15]];

        let mut planes = [
            add_plane(row4, row1), // left
            sub_plane(row4, row1), // right
            add_plane(row4, row2), // bottom
            sub_plane(row4, row2), // top
            add_plane(row4, row3), // near
            sub_plane(row4, row3), // far
        ];

        for plane in &mut planes {
            normalize_plane(plane);
        }

        Self { planes }
    }

    fn intersects_chunk(&self, pos: ChunkPos) -> bool {
        let (min, max) = chunk_aabb(pos);
        self.intersects_aabb(min, max)
    }

    fn intersects_aabb(&self, min: [f32; 3], max: [f32; 3]) -> bool {
        for plane in &self.planes {
            let px = if plane[0] >= 0.0 { max[0] } else { min[0] };
            let py = if plane[1] >= 0.0 { max[1] } else { min[1] };
            let pz = if plane[2] >= 0.0 { max[2] } else { min[2] };
            let distance = plane[0] * px + plane[1] * py + plane[2] * pz + plane[3];
            if distance < 0.0 {
                return false;
            }
        }
        true
    }
}

fn chunk_aabb(pos: ChunkPos) -> ([f32; 3], [f32; 3]) {
    let min = [pos.x as f32 * 16.0, 0.0, pos.z as f32 * 16.0];
    let max = [min[0] + 16.0, 128.0, min[2] + 16.0];
    (min, max)
}

fn add_plane(a: [f32; 4], b: [f32; 4]) -> [f32; 4] {
    [a[0] + b[0], a[1] + b[1], a[2] + b[2], a[3] + b[3]]
}

fn sub_plane(a: [f32; 4], b: [f32; 4]) -> [f32; 4] {
    [a[0] - b[0], a[1] - b[1], a[2] - b[2], a[3] - b[3]]
}

fn normalize_plane(plane: &mut [f32; 4]) {
    let length = (plane[0] * plane[0] + plane[1] * plane[1] + plane[2] * plane[2]).sqrt();
    if length > 0.0 {
        plane[0] /= length;
        plane[1] /= length;
        plane[2] /= length;
        plane[3] /= length;
    }
}

fn create_depth_resources(
    device: &wgpu::Device,
    config: &wgpu::SurfaceConfiguration,
) -> (wgpu::Texture, wgpu::TextureView) {
    let depth_texture = device.create_texture(&wgpu::TextureDescriptor {
        label: Some("thingcraft-depth-texture"),
        size: wgpu::Extent3d {
            width: config.width.max(1),
            height: config.height.max(1),
            depth_or_array_layers: 1,
        },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: DEPTH_FORMAT,
        usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
        view_formats: &[],
    });

    let depth_view = depth_texture.create_view(&wgpu::TextureViewDescriptor::default());
    (depth_texture, depth_view)
}

fn create_terrain_bind_group(
    device: &wgpu::Device,
    queue: &wgpu::Queue,
    layout: &wgpu::BindGroupLayout,
    atlas_path: &Path,
) -> wgpu::BindGroup {
    let (width, height, atlas_rgba) = load_terrain_atlas_rgba(atlas_path);
    let texture_size = wgpu::Extent3d {
        width,
        height,
        depth_or_array_layers: 1,
    };

    let texture = device.create_texture(&wgpu::TextureDescriptor {
        label: Some("thingcraft-terrain-atlas"),
        size: texture_size,
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Rgba8UnormSrgb,
        usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
        view_formats: &[],
    });

    queue.write_texture(
        wgpu::ImageCopyTexture {
            texture: &texture,
            mip_level: 0,
            origin: wgpu::Origin3d::ZERO,
            aspect: wgpu::TextureAspect::All,
        },
        &atlas_rgba,
        wgpu::ImageDataLayout {
            offset: 0,
            bytes_per_row: Some(4 * width),
            rows_per_image: Some(height),
        },
        texture_size,
    );

    let texture_view = texture.create_view(&wgpu::TextureViewDescriptor::default());
    let sampler = device.create_sampler(&wgpu::SamplerDescriptor {
        label: Some("thingcraft-terrain-sampler"),
        address_mode_u: wgpu::AddressMode::ClampToEdge,
        address_mode_v: wgpu::AddressMode::ClampToEdge,
        address_mode_w: wgpu::AddressMode::ClampToEdge,
        mag_filter: wgpu::FilterMode::Nearest,
        min_filter: wgpu::FilterMode::Nearest,
        mipmap_filter: wgpu::FilterMode::Nearest,
        ..Default::default()
    });

    device.create_bind_group(&wgpu::BindGroupDescriptor {
        label: Some("thingcraft-terrain-bind-group"),
        layout,
        entries: &[
            wgpu::BindGroupEntry {
                binding: 0,
                resource: wgpu::BindingResource::TextureView(&texture_view),
            },
            wgpu::BindGroupEntry {
                binding: 1,
                resource: wgpu::BindingResource::Sampler(&sampler),
            },
        ],
    })
}

fn load_terrain_atlas_rgba(atlas_path: &Path) -> (u32, u32, Vec<u8>) {
    let fallback = || {
        (
            2,
            2,
            vec![
                255, 0, 255, 255, 0, 0, 0, 255, 0, 0, 0, 255, 255, 0, 255, 255,
            ],
        )
    };

    let candidate_paths = [atlas_path.to_path_buf(), Path::new("..").join(atlas_path)];
    for candidate in candidate_paths {
        let bytes = match std::fs::read(&candidate) {
            Ok(bytes) => bytes,
            Err(_) => continue,
        };

        let image = match image::load_from_memory_with_format(&bytes, image::ImageFormat::Png) {
            Ok(image) => image,
            Err(_) => continue,
        };

        let rgba = image.to_rgba8();
        let (width, height) = rgba.dimensions();
        return (width, height, rgba.into_raw());
    }

    fallback()
}

const CHUNK_SHADER: &str = r#"
struct Camera {
    view_proj: mat4x4<f32>,
};

@group(0) @binding(0)
var<uniform> camera: Camera;

@group(1) @binding(0)
var terrain_atlas: texture_2d<f32>;

@group(1) @binding(1)
var terrain_sampler: sampler;

struct VertexIn {
    @location(0) position: vec3<f32>,
    @location(1) uv: vec2<f32>,
    @location(2) light: vec4<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) light: vec3<f32>,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    out.clip_pos = camera.view_proj * vec4<f32>(input.position, 1.0);
    out.uv = input.uv;
    out.light = input.light.rgb;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    let texel = textureSample(terrain_atlas, terrain_sampler, input.uv);
    return vec4<f32>(texel.rgb * input.light, texel.a);
}
"#;

const DEBUG_LINE_SHADER: &str = r#"
struct Camera {
    view_proj: mat4x4<f32>,
};

@group(0) @binding(0)
var<uniform> camera: Camera;

struct VertexIn {
    @location(0) position: vec3<f32>,
    @location(1) color: vec3<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) color: vec3<f32>,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    out.clip_pos = camera.view_proj * vec4<f32>(input.position, 1.0);
    out.color = input.color;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    return vec4<f32>(input.color, 1.0);
}
"#;

const HUD_SHADER: &str = r#"
struct HudScreen {
    screen_width: f32,
    screen_height: f32,
};

@group(0) @binding(0)
var<uniform> screen: HudScreen;

struct VertexIn {
    @location(0) position: vec2<f32>,
    @location(1) color: vec4<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) color: vec4<f32>,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    // Convert screen-pixel coords to NDC: x: [0, width] -> [-1, 1], y: [0, height] -> [1, -1].
    let ndc_x = (input.position.x / screen.screen_width) * 2.0 - 1.0;
    let ndc_y = 1.0 - (input.position.y / screen.screen_height) * 2.0;

    var out: VertexOut;
    out.clip_pos = vec4<f32>(ndc_x, ndc_y, 0.0, 1.0);
    out.color = input.color;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    return input.color;
}
"#;

#[cfg(test)]
mod tests {
    use glam::{Mat4, Vec3};

    use super::{
        build_chunk_border_vertices, build_chunk_status_vertices, chunk_aabb, FrustumPlanes,
    };
    use crate::streaming::{ChunkDebugState, ChunkResidencyState};
    use crate::world::ChunkPos;

    #[test]
    fn frustum_includes_origin_chunk_and_rejects_far_chunk() {
        let view = Mat4::look_to_rh(Vec3::new(8.0, 64.0, 8.0), Vec3::new(0.0, 0.0, 1.0), Vec3::Y);
        let proj = Mat4::perspective_rh_gl(70_f32.to_radians(), 16.0 / 9.0, 0.05, 128.0);
        let frustum = FrustumPlanes::from_view_proj(proj * view);

        assert!(frustum.intersects_chunk(ChunkPos { x: 0, z: 0 }));
        assert!(!frustum.intersects_chunk(ChunkPos { x: 100, z: 100 }));
    }

    #[test]
    fn chunk_aabb_uses_alpha_dimensions() {
        let (min, max) = chunk_aabb(ChunkPos { x: -2, z: 3 });
        assert_eq!(min, [-32.0, 0.0, 48.0]);
        assert_eq!(max, [-16.0, 128.0, 64.0]);
    }

    #[test]
    fn chunk_border_builder_emits_expected_line_vertex_count() {
        let vertices = build_chunk_border_vertices([ChunkPos { x: 0, z: 0 }].into_iter());
        assert_eq!(vertices.len(), 24);
    }

    #[test]
    fn chunk_status_builder_emits_three_bars_per_chunk() {
        let vertices = build_chunk_status_vertices(
            [ChunkDebugState {
                pos: ChunkPos { x: 0, z: 0 },
                residency_state: ChunkResidencyState::Meshing,
                dirty_geometry: true,
                dirty_lighting: true,
                in_flight_generation: false,
                in_flight_lighting: true,
                in_flight_meshing: false,
            }]
            .into_iter(),
        );
        assert_eq!(vertices.len(), 6);
    }
}
