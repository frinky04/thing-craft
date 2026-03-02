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
use crate::world::{ChunkPos, CHUNK_SECTION_COUNT, SECTION_HEIGHT};

const DEPTH_FORMAT: wgpu::TextureFormat = wgpu::TextureFormat::Depth32Float;
const DEFAULT_FOG_COLOR: [f32; 3] = [0.04, 0.07, 0.12];
const BUFFER_POOL_BYTE_BUDGET: u64 = 64 * 1024 * 1024;
const CLOUD_LAYER_HEIGHT: f32 = 120.33;
const CLOUD_UV_SCALE: f32 = 1.0 / 2048.0;
const CLOUD_TILE_SIZE: f32 = 32.0;
const CLOUD_GRID_RADIUS_TILES: i32 = 8;

pub struct Renderer<'w> {
    surface: wgpu::Surface<'w>,
    device: wgpu::Device,
    queue: wgpu::Queue,
    config: wgpu::SurfaceConfiguration,
    size: PhysicalSize<u32>,
    render_pipeline: wgpu::RenderPipeline,
    transparent_pipeline: wgpu::RenderPipeline,
    debug_line_pipeline: wgpu::RenderPipeline,
    sky_pipeline: wgpu::RenderPipeline,
    cloud_pipeline: wgpu::RenderPipeline,
    camera_buffer: wgpu::Buffer,
    camera_bind_group: wgpu::BindGroup,
    sky_uniform_buffer: wgpu::Buffer,
    sky_bind_group: wgpu::BindGroup,
    terrain_atlas: TerrainAtlas,
    cloud_layer: CloudLayer,
    depth_texture: wgpu::Texture,
    depth_view: wgpu::TextureView,
    scene_mesh: Option<SceneMeshGpu>,
    chunk_meshes: HashMap<ChunkPos, [Option<SceneMeshGpu>; CHUNK_SECTION_COUNT]>,
    chunk_transparent_meshes: HashMap<ChunkPos, [Option<SceneMeshGpu>; CHUNK_SECTION_COUNT]>,
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
    fog_color: [f32; 3],
    sky_color: [f32; 3],
    render_sky: bool,
    cloud_color: [f32; 3],
    cloud_scroll: f32,
    ambient_darkness: f32,
    leaf_cutout_enabled: f32,
    camera_pos: [f32; 3],
    mesh_buffer_pool: MeshBufferPool,
}

struct SceneMeshGpu {
    vertex_buffer: wgpu::Buffer,
    index_buffer: wgpu::Buffer,
    index_count: u32,
    vertex_bytes: u64,
    index_bytes: u64,
}

struct TerrainAtlas {
    bind_group: wgpu::BindGroup,
    texture: wgpu::Texture,
    water_top: WaterSpriteAnimator,
    water_side: WaterSpriteAnimator,
}

struct CloudLayer {
    bind_group: wgpu::BindGroup,
    uniform_buffer: wgpu::Buffer,
    uniform_bind_group: wgpu::BindGroup,
    vertex_buffer: wgpu::Buffer,
    index_buffer: wgpu::Buffer,
    index_count: u32,
}

struct WaterSpriteAnimator {
    current: [f32; 256],
    next: [f32; 256],
    heat: [f32; 256],
    heat_delta: [f32; 256],
    ticks: i32,
    side_variant: bool,
    rng_state: u32,
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
    camera_pos: [f32; 3],
    fog_start: f32,
    fog_color: [f32; 3],
    fog_end: f32,
    ambient_darkness: f32,
    leaf_cutout_enabled: f32,
    _pad: [f32; 2],
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct SkyUniform {
    color: [f32; 3],
    _pad: f32,
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct CloudUniform {
    origin: [f32; 3],
    alpha: f32,
    uv_offset: [f32; 2],
    _pad0: [f32; 2],
    color: [f32; 3],
    _pad1: f32,
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct CloudVertex {
    local_pos: [f32; 2],
}

impl CloudVertex {
    const ATTRS: [wgpu::VertexAttribute; 1] = wgpu::vertex_attr_array![0 => Float32x2];

    #[must_use]
    fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

#[derive(Default)]
struct MeshBufferPool {
    vertex_buffers: Vec<PooledBuffer>,
    index_buffers: Vec<PooledBuffer>,
    total_bytes: u64,
}

struct PooledBuffer {
    buffer: wgpu::Buffer,
    size: u64,
}

impl WaterSpriteAnimator {
    fn new(side_variant: bool, seed: u32) -> Self {
        Self {
            current: [0.0; 256],
            next: [0.0; 256],
            heat: [0.0; 256],
            heat_delta: [0.0; 256],
            ticks: 0,
            side_variant,
            rng_state: seed.max(1),
        }
    }

    fn rand_f32(&mut self) -> f32 {
        let mut x = self.rng_state;
        x ^= x << 13;
        x ^= x >> 17;
        x ^= x << 5;
        self.rng_state = x.max(1);
        self.rng_state as f32 / u32::MAX as f32
    }

    fn tick(&mut self) -> [u8; 1024] {
        self.ticks += 1;

        for i in 0..16_i32 {
            for j in 0..16_i32 {
                let mut sum = 0.0_f32;
                if self.side_variant {
                    for m in (j - 2)..=j {
                        let n = i & 0xF;
                        let p = m & 0xF;
                        sum += self.current[(n + p * 16) as usize];
                    }
                    self.next[(i + j * 16) as usize] =
                        sum / 3.2 + self.heat[(i + j * 16) as usize] * 0.8;
                } else {
                    for m in (i - 1)..=(i + 1) {
                        let n = m & 0xF;
                        let p = j & 0xF;
                        sum += self.current[(n + p * 16) as usize];
                    }
                    self.next[(i + j * 16) as usize] =
                        sum / 3.3 + self.heat[(i + j * 16) as usize] * 0.8;
                }
            }
        }

        for i in 0..16_i32 {
            for k in 0..16_i32 {
                let index = (i + k * 16) as usize;
                self.heat[index] += self.heat_delta[index] * 0.05;
                if self.heat[index] < 0.0 {
                    self.heat[index] = 0.0;
                }

                if self.side_variant {
                    self.heat_delta[index] -= 0.3;
                    if self.rand_f32() < 0.2 {
                        self.heat_delta[index] = 0.5;
                    }
                } else {
                    self.heat_delta[index] -= 0.1;
                    if self.rand_f32() < 0.05 {
                        self.heat_delta[index] = 0.5;
                    }
                }
            }
        }

        std::mem::swap(&mut self.next, &mut self.current);

        let mut pixels = [0_u8; 1024];
        for l in 0..256_i32 {
            let sample = if self.side_variant {
                (l - self.ticks * 16) & 0xFF
            } else {
                l
            } as usize;
            let g = self.current[sample].clamp(0.0, 1.0);
            let h = g * g;
            let red = (32.0 + h * 32.0) as u8;
            let green = (50.0 + h * 64.0) as u8;
            let blue = 255_u8;
            let alpha = (146.0 + h * 50.0) as u8;

            let p = l as usize * 4;
            pixels[p] = red;
            pixels[p + 1] = green;
            pixels[p + 2] = blue;
            pixels[p + 3] = alpha;
        }

        pixels
    }
}

#[derive(Debug, Error)]
pub enum RenderError {
    #[error("surface is out of memory")]
    OutOfMemory,
    #[error("surface timeout")]
    Timeout,
}

impl MeshBufferPool {
    fn acquire_vertex_buffer(
        &mut self,
        device: &wgpu::Device,
        required_size: u64,
        label: &'static str,
    ) -> wgpu::Buffer {
        self.acquire_buffer(
            device,
            required_size,
            wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
            label,
            true,
        )
    }

    fn acquire_index_buffer(
        &mut self,
        device: &wgpu::Device,
        required_size: u64,
        label: &'static str,
    ) -> wgpu::Buffer {
        self.acquire_buffer(
            device,
            required_size,
            wgpu::BufferUsages::INDEX | wgpu::BufferUsages::COPY_DST,
            label,
            false,
        )
    }

    fn acquire_buffer(
        &mut self,
        device: &wgpu::Device,
        required_size: u64,
        usage: wgpu::BufferUsages,
        label: &'static str,
        vertex: bool,
    ) -> wgpu::Buffer {
        let bucket = if vertex {
            &mut self.vertex_buffers
        } else {
            &mut self.index_buffers
        };
        if let Some(index) = bucket.iter().position(|entry| {
            entry.size >= required_size && entry.size <= required_size.saturating_mul(2)
        }) {
            let entry = bucket.swap_remove(index);
            self.total_bytes = self.total_bytes.saturating_sub(entry.size);
            return entry.buffer;
        }

        device.create_buffer(&wgpu::BufferDescriptor {
            label: Some(label),
            size: required_size.max(4),
            usage,
            mapped_at_creation: false,
        })
    }

    fn recycle_vertex_buffer(&mut self, buffer: wgpu::Buffer, size: u64) {
        self.recycle_buffer(buffer, size, true);
    }

    fn recycle_index_buffer(&mut self, buffer: wgpu::Buffer, size: u64) {
        self.recycle_buffer(buffer, size, false);
    }

    fn recycle_buffer(&mut self, buffer: wgpu::Buffer, size: u64, vertex: bool) {
        let bucket = if vertex {
            &mut self.vertex_buffers
        } else {
            &mut self.index_buffers
        };
        bucket.push(PooledBuffer { buffer, size });
        self.total_bytes = self.total_bytes.saturating_add(size);
        while self.total_bytes > BUFFER_POOL_BYTE_BUDGET {
            if let Some(old) = self.vertex_buffers.pop() {
                self.total_bytes = self.total_bytes.saturating_sub(old.size);
                continue;
            }
            if let Some(old) = self.index_buffers.pop() {
                self.total_bytes = self.total_bytes.saturating_sub(old.size);
                continue;
            }
            break;
        }
    }
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
            .find(|format| !format.is_srgb())
            .or_else(|| {
                surface_caps
                    .formats
                    .iter()
                    .copied()
                    .find(wgpu::TextureFormat::is_srgb)
            })
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
            camera_pos: [0.0, 0.0, 0.0],
            fog_start: 0.0,
            fog_color: DEFAULT_FOG_COLOR,
            fog_end: 1.0,
            ambient_darkness: 0.0,
            leaf_cutout_enabled: 1.0,
            _pad: [0.0; 2],
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
                    visibility: wgpu::ShaderStages::VERTEX_FRAGMENT,
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

        let sky_uniform = SkyUniform {
            color: DEFAULT_FOG_COLOR,
            _pad: 0.0,
        };
        let sky_uniform_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("thingcraft-sky-uniform-buffer"),
            contents: bytemuck::bytes_of(&sky_uniform),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });
        let sky_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-sky-bind-group-layout"),
                entries: &[wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                }],
            });
        let sky_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("thingcraft-sky-bind-group"),
            layout: &sky_bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: sky_uniform_buffer.as_entire_binding(),
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

        let terrain_atlas = create_terrain_atlas(
            &device,
            &queue,
            &terrain_bind_group_layout,
            Path::new("resources/minecraft-a1.2.6-client/terrain.png"),
        );
        let cloud_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-cloud-bind-group-layout"),
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
        let cloud_uniform_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-cloud-uniform-bind-group-layout"),
                entries: &[wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::VERTEX_FRAGMENT,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                }],
            });

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

        let transparent_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-transparent-pipeline"),
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
                cull_mode: None, // Water visible from both sides when underwater
                polygon_mode: wgpu::PolygonMode::Fill,
                unclipped_depth: false,
                conservative: false,
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format: DEPTH_FORMAT,
                depth_write_enabled: false, // Reads depth but doesn't write
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
                    blend: Some(wgpu::BlendState::ALPHA_BLENDING),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            }),
            multiview: None,
        });

        let sky_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-sky-shader"),
            source: wgpu::ShaderSource::Wgsl(SKY_SHADER.into()),
        });
        let sky_pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("thingcraft-sky-pipeline-layout"),
            bind_group_layouts: &[&sky_bind_group_layout],
            push_constant_ranges: &[],
        });
        let sky_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-sky-pipeline"),
            layout: Some(&sky_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &sky_shader,
                entry_point: "vs_main",
                buffers: &[],
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
            depth_stencil: Some(wgpu::DepthStencilState {
                format: DEPTH_FORMAT,
                depth_write_enabled: false,
                depth_compare: wgpu::CompareFunction::Always,
                stencil: wgpu::StencilState::default(),
                bias: wgpu::DepthBiasState::default(),
            }),
            multisample: wgpu::MultisampleState {
                count: 1,
                mask: !0,
                alpha_to_coverage_enabled: false,
            },
            fragment: Some(wgpu::FragmentState {
                module: &sky_shader,
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

        let cloud_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-cloud-shader"),
            source: wgpu::ShaderSource::Wgsl(CLOUD_SHADER.into()),
        });
        let cloud_pipeline_layout =
            device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                label: Some("thingcraft-cloud-pipeline-layout"),
                bind_group_layouts: &[
                    &camera_bind_group_layout,
                    &cloud_bind_group_layout,
                    &cloud_uniform_bind_group_layout,
                ],
                push_constant_ranges: &[],
            });
        let cloud_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-cloud-pipeline"),
            layout: Some(&cloud_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &cloud_shader,
                entry_point: "vs_main",
                buffers: &[CloudVertex::layout()],
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
                module: &cloud_shader,
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
        let cloud_layer = create_cloud_layer(
            &device,
            &queue,
            &cloud_bind_group_layout,
            &cloud_uniform_bind_group_layout,
            Path::new("resources/minecraft-a1.2.6-client/environment/clouds.png"),
        );

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
            transparent_pipeline,
            debug_line_pipeline,
            sky_pipeline,
            cloud_pipeline,
            camera_buffer,
            camera_bind_group,
            sky_uniform_buffer,
            sky_bind_group,
            terrain_atlas,
            cloud_layer,
            depth_texture,
            depth_view,
            scene_mesh: None,
            chunk_meshes: HashMap::new(),
            chunk_transparent_meshes: HashMap::new(),
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
            fog_color: DEFAULT_FOG_COLOR,
            sky_color: DEFAULT_FOG_COLOR,
            render_sky: false,
            cloud_color: [1.0, 1.0, 1.0],
            cloud_scroll: 0.0,
            ambient_darkness: 0.0,
            leaf_cutout_enabled: 1.0,
            camera_pos: [0.0; 3],
            mesh_buffer_pool: MeshBufferPool::default(),
        })
    }

    pub fn viewport_aspect(&self) -> f32 {
        if self.size.height == 0 {
            1.0
        } else {
            self.size.width as f32 / self.size.height as f32
        }
    }

    pub fn update_camera(
        &mut self,
        view_proj: [[f32; 4]; 4],
        camera_pos: [f32; 3],
        fog_start: f32,
        fog_end: f32,
    ) {
        let uniform = CameraUniform {
            view_proj,
            camera_pos,
            fog_start,
            fog_color: self.fog_color,
            fog_end: fog_end.max(fog_start + 0.001),
            ambient_darkness: self.ambient_darkness,
            leaf_cutout_enabled: self.leaf_cutout_enabled,
            _pad: [0.0; 2],
        };
        self.queue
            .write_buffer(&self.camera_buffer, 0, bytemuck::bytes_of(&uniform));
        let matrix = Mat4::from_cols_array_2d(&view_proj);
        self.camera_frustum = Some(FrustumPlanes::from_view_proj(matrix));
        self.camera_pos = camera_pos;
    }

    pub fn set_day_night(
        &mut self,
        fog_color: [f32; 3],
        sky_color: [f32; 3],
        ambient_darkness: u8,
        render_sky: bool,
    ) {
        self.fog_color = fog_color;
        self.sky_color = sky_color;
        self.render_sky = render_sky;
        self.ambient_darkness = f32::from(ambient_darkness.min(11));
        let uniform = SkyUniform {
            color: self.sky_color,
            _pad: 0.0,
        };
        self.queue
            .write_buffer(&self.sky_uniform_buffer, 0, bytemuck::bytes_of(&uniform));
    }

    pub fn set_leaf_cutout_enabled(&mut self, enabled: bool) {
        self.leaf_cutout_enabled = if enabled { 1.0 } else { 0.0 };
    }

    pub fn set_cloud_state(&mut self, cloud_color: [f32; 3], cloud_scroll: f32) {
        self.cloud_color = cloud_color;
        self.cloud_scroll = cloud_scroll;
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
            vertex_bytes: std::mem::size_of_val(mesh.vertices.as_slice()) as u64,
            index_bytes: std::mem::size_of_val(mesh.indices.as_slice()) as u64,
        });
    }

    pub fn upsert_chunk_section_mesh(&mut self, pos: ChunkPos, section_y: u8, mesh: &ChunkMesh) {
        self.upsert_section_mesh_into(
            pos,
            section_y,
            mesh,
            false,
            "thingcraft-chunk-section-vertex-buffer",
            "thingcraft-chunk-section-index-buffer",
        );
        self.chunk_border_mesh_dirty = true;
    }

    pub fn upsert_chunk_section_transparent_mesh(
        &mut self,
        pos: ChunkPos,
        section_y: u8,
        mesh: &ChunkMesh,
    ) {
        // Early return: skip HashMap insert/remove churn for empty meshes with no entry.
        if (mesh.vertices.is_empty() || mesh.indices.is_empty())
            && !self.chunk_transparent_meshes.contains_key(&pos)
        {
            return;
        }
        self.upsert_section_mesh_into(
            pos,
            section_y,
            mesh,
            true,
            "thingcraft-chunk-section-transparent-vertex-buffer",
            "thingcraft-chunk-section-transparent-index-buffer",
        );
    }

    fn upsert_section_mesh_into(
        &mut self,
        pos: ChunkPos,
        section_y: u8,
        mesh: &ChunkMesh,
        transparent: bool,
        vb_label: &'static str,
        ib_label: &'static str,
    ) {
        let index = usize::from(section_y);
        if index >= CHUNK_SECTION_COUNT {
            return;
        }

        let map = if transparent {
            &mut self.chunk_transparent_meshes
        } else {
            &mut self.chunk_meshes
        };

        let sections = map
            .entry(pos)
            .or_insert_with(|| std::array::from_fn(|_| None));

        if let Some(old_mesh) = sections[index].take() {
            self.mesh_buffer_pool
                .recycle_vertex_buffer(old_mesh.vertex_buffer, old_mesh.vertex_bytes);
            self.mesh_buffer_pool
                .recycle_index_buffer(old_mesh.index_buffer, old_mesh.index_bytes);
        }

        if mesh.vertices.is_empty() || mesh.indices.is_empty() {
            if sections.iter().all(Option::is_none) {
                map.remove(&pos);
            }
            return;
        }

        let vertex_bytes = std::mem::size_of_val(mesh.vertices.as_slice()) as u64;
        let index_bytes = std::mem::size_of_val(mesh.indices.as_slice()) as u64;
        let vertex_buffer =
            self.mesh_buffer_pool
                .acquire_vertex_buffer(&self.device, vertex_bytes, vb_label);
        self.queue
            .write_buffer(&vertex_buffer, 0, bytemuck::cast_slice(&mesh.vertices));
        let index_buffer =
            self.mesh_buffer_pool
                .acquire_index_buffer(&self.device, index_bytes, ib_label);
        self.queue
            .write_buffer(&index_buffer, 0, bytemuck::cast_slice(&mesh.indices));

        sections[index] = Some(SceneMeshGpu {
            vertex_buffer,
            index_buffer,
            index_count: mesh.indices.len() as u32,
            vertex_bytes,
            index_bytes,
        });
    }

    pub fn remove_chunk_mesh(&mut self, pos: ChunkPos) {
        let opaque = self.chunk_meshes.remove(&pos);
        let transparent = self.chunk_transparent_meshes.remove(&pos);
        recycle_chunk_sections(&mut self.mesh_buffer_pool, opaque);
        recycle_chunk_sections(&mut self.mesh_buffer_pool, transparent);
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

    fn update_cloud_uniform(&mut self) {
        let uniform = CloudUniform {
            origin: [self.camera_pos[0], CLOUD_LAYER_HEIGHT, self.camera_pos[2]],
            alpha: 0.8,
            uv_offset: [self.cloud_scroll * CLOUD_UV_SCALE, 0.0],
            _pad0: [0.0; 2],
            color: self.cloud_color,
            _pad1: 0.0,
        };
        self.queue.write_buffer(
            &self.cloud_layer.uniform_buffer,
            0,
            bytemuck::bytes_of(&uniform),
        );
    }

    pub fn advance_dynamic_liquid_textures(&mut self, ticks: u32) {
        if ticks == 0 {
            return;
        }

        let mut water_top_tile = [0_u8; 1024];
        let mut water_side_tile = [0_u8; 1024];
        for _ in 0..ticks {
            water_top_tile = self.terrain_atlas.water_top.tick();
            water_side_tile = self.terrain_atlas.water_side.tick();
        }

        upload_dynamic_sprite(
            &self.queue,
            &self.terrain_atlas.texture,
            WATER_TOP_SPRITE,
            1,
            &water_top_tile,
        );
        upload_dynamic_sprite(
            &self.queue,
            &self.terrain_atlas.texture,
            WATER_SIDE_SPRITE,
            2,
            &water_side_tile,
        );
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
        self.update_cloud_uniform();

        {
            let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("thingcraft-render-pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: self.fog_color[0] as f64,
                            g: self.fog_color[1] as f64,
                            b: self.fog_color[2] as f64,
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

            if self.render_sky {
                render_pass.set_pipeline(&self.sky_pipeline);
                render_pass.set_bind_group(0, &self.sky_bind_group, &[]);
                render_pass.draw(0..3, 0..1);
            }

            if let Some(scene_mesh) = &self.scene_mesh {
                render_pass.set_pipeline(&self.render_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_bind_group(1, &self.terrain_atlas.bind_group, &[]);
                render_pass.set_vertex_buffer(0, scene_mesh.vertex_buffer.slice(..));
                render_pass
                    .set_index_buffer(scene_mesh.index_buffer.slice(..), wgpu::IndexFormat::Uint32);
                render_pass.draw_indexed(0..scene_mesh.index_count, 0, 0..1);
            }

            if !self.chunk_meshes.is_empty() {
                render_pass.set_pipeline(&self.render_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_bind_group(1, &self.terrain_atlas.bind_group, &[]);
                let frustum = self.camera_frustum.as_ref();
                let mut visible = 0_usize;
                for (pos, sections) in &self.chunk_meshes {
                    let mut chunk_visible = false;
                    for (section_y, chunk_mesh) in sections.iter().enumerate() {
                        let Some(chunk_mesh) = chunk_mesh else {
                            continue;
                        };
                        if frustum.is_some_and(|view| {
                            !view.intersects_chunk_section(*pos, section_y as u8)
                        }) {
                            continue;
                        }

                        render_pass.set_vertex_buffer(0, chunk_mesh.vertex_buffer.slice(..));
                        render_pass.set_index_buffer(
                            chunk_mesh.index_buffer.slice(..),
                            wgpu::IndexFormat::Uint32,
                        );
                        render_pass.draw_indexed(0..chunk_mesh.index_count, 0, 0..1);
                        chunk_visible = true;
                    }
                    if chunk_visible {
                        visible += 1;
                    }
                }
                self.visible_chunk_meshes = visible;
            } else {
                self.visible_chunk_meshes = 0;
            }

            // Transparent pass: water and other translucent blocks (after opaque, before debug).
            if !self.chunk_transparent_meshes.is_empty() {
                render_pass.set_pipeline(&self.transparent_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_bind_group(1, &self.terrain_atlas.bind_group, &[]);
                let frustum = self.camera_frustum.as_ref();
                for (pos, sections) in &self.chunk_transparent_meshes {
                    for (section_y, chunk_mesh) in sections.iter().enumerate() {
                        let Some(chunk_mesh) = chunk_mesh else {
                            continue;
                        };
                        if frustum.is_some_and(|view| {
                            !view.intersects_chunk_section(*pos, section_y as u8)
                        }) {
                            continue;
                        }

                        render_pass.set_vertex_buffer(0, chunk_mesh.vertex_buffer.slice(..));
                        render_pass.set_index_buffer(
                            chunk_mesh.index_buffer.slice(..),
                            wgpu::IndexFormat::Uint32,
                        );
                        render_pass.draw_indexed(0..chunk_mesh.index_count, 0, 0..1);
                    }
                }
            }

            render_pass.set_pipeline(&self.cloud_pipeline);
            render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
            render_pass.set_bind_group(1, &self.cloud_layer.bind_group, &[]);
            render_pass.set_bind_group(2, &self.cloud_layer.uniform_bind_group, &[]);
            render_pass.set_vertex_buffer(0, self.cloud_layer.vertex_buffer.slice(..));
            render_pass.set_index_buffer(
                self.cloud_layer.index_buffer.slice(..),
                wgpu::IndexFormat::Uint32,
            );
            render_pass.draw_indexed(0..self.cloud_layer.index_count, 0, 0..1);

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

    #[allow(dead_code)]
    fn intersects_chunk(&self, pos: ChunkPos) -> bool {
        let (min, max) = chunk_aabb(pos);
        self.intersects_aabb(min, max)
    }

    fn intersects_chunk_section(&self, pos: ChunkPos, section_y: u8) -> bool {
        let (min, max) = chunk_section_aabb(pos, section_y);
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

fn chunk_section_aabb(pos: ChunkPos, section_y: u8) -> ([f32; 3], [f32; 3]) {
    let y_min = f32::from(section_y) * SECTION_HEIGHT as f32;
    let min = [pos.x as f32 * 16.0, y_min, pos.z as f32 * 16.0];
    let max = [min[0] + 16.0, y_min + SECTION_HEIGHT as f32, min[2] + 16.0];
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

fn recycle_chunk_sections(
    pool: &mut MeshBufferPool,
    sections: Option<[Option<SceneMeshGpu>; CHUNK_SECTION_COUNT]>,
) {
    if let Some(sections) = sections {
        for mesh in sections.into_iter().flatten() {
            pool.recycle_vertex_buffer(mesh.vertex_buffer, mesh.vertex_bytes);
            pool.recycle_index_buffer(mesh.index_buffer, mesh.index_bytes);
        }
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

fn create_cloud_layer(
    device: &wgpu::Device,
    queue: &wgpu::Queue,
    texture_layout: &wgpu::BindGroupLayout,
    uniform_layout: &wgpu::BindGroupLayout,
    cloud_texture_path: &Path,
) -> CloudLayer {
    let (width, height, rgba) = load_png_rgba(cloud_texture_path);
    let cloud_texture = device.create_texture(&wgpu::TextureDescriptor {
        label: Some("thingcraft-cloud-texture"),
        size: wgpu::Extent3d {
            width,
            height,
            depth_or_array_layers: 1,
        },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Rgba8Unorm,
        usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
        view_formats: &[],
    });
    queue.write_texture(
        wgpu::ImageCopyTexture {
            texture: &cloud_texture,
            mip_level: 0,
            origin: wgpu::Origin3d::ZERO,
            aspect: wgpu::TextureAspect::All,
        },
        &rgba,
        wgpu::ImageDataLayout {
            offset: 0,
            bytes_per_row: Some(4 * width),
            rows_per_image: Some(height),
        },
        wgpu::Extent3d {
            width,
            height,
            depth_or_array_layers: 1,
        },
    );

    let texture_view = cloud_texture.create_view(&wgpu::TextureViewDescriptor::default());
    let sampler = device.create_sampler(&wgpu::SamplerDescriptor {
        label: Some("thingcraft-cloud-sampler"),
        address_mode_u: wgpu::AddressMode::Repeat,
        address_mode_v: wgpu::AddressMode::Repeat,
        address_mode_w: wgpu::AddressMode::ClampToEdge,
        mag_filter: wgpu::FilterMode::Linear,
        min_filter: wgpu::FilterMode::Linear,
        mipmap_filter: wgpu::FilterMode::Nearest,
        ..Default::default()
    });
    let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
        label: Some("thingcraft-cloud-bind-group"),
        layout: texture_layout,
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
    });

    let cloud_uniform = CloudUniform {
        origin: [0.0, CLOUD_LAYER_HEIGHT, 0.0],
        alpha: 0.8,
        uv_offset: [0.0, 0.0],
        _pad0: [0.0; 2],
        color: [1.0, 1.0, 1.0],
        _pad1: 0.0,
    };
    let uniform_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-cloud-uniform-buffer"),
        contents: bytemuck::bytes_of(&cloud_uniform),
        usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
    });
    let uniform_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
        label: Some("thingcraft-cloud-uniform-bind-group"),
        layout: uniform_layout,
        entries: &[wgpu::BindGroupEntry {
            binding: 0,
            resource: uniform_buffer.as_entire_binding(),
        }],
    });

    let (vertices, indices) = build_cloud_grid_mesh();
    let vertex_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-cloud-vertex-buffer"),
        contents: bytemuck::cast_slice(&vertices),
        usage: wgpu::BufferUsages::VERTEX,
    });
    let index_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-cloud-index-buffer"),
        contents: bytemuck::cast_slice(&indices),
        usage: wgpu::BufferUsages::INDEX,
    });

    CloudLayer {
        bind_group,
        uniform_buffer,
        uniform_bind_group,
        vertex_buffer,
        index_buffer,
        index_count: indices.len() as u32,
    }
}

fn build_cloud_grid_mesh() -> (Vec<CloudVertex>, Vec<u32>) {
    let extent = CLOUD_GRID_RADIUS_TILES as f32 * CLOUD_TILE_SIZE;
    let tiles_per_axis = CLOUD_GRID_RADIUS_TILES * 2;
    let mut vertices = Vec::with_capacity((tiles_per_axis * tiles_per_axis * 4) as usize);
    let mut indices = Vec::with_capacity((tiles_per_axis * tiles_per_axis * 6) as usize);

    for z in 0..tiles_per_axis {
        for x in 0..tiles_per_axis {
            let x0 = -extent + x as f32 * CLOUD_TILE_SIZE;
            let z0 = -extent + z as f32 * CLOUD_TILE_SIZE;
            let x1 = x0 + CLOUD_TILE_SIZE;
            let z1 = z0 + CLOUD_TILE_SIZE;
            let base = vertices.len() as u32;
            vertices.push(CloudVertex {
                local_pos: [x0, z0],
            });
            vertices.push(CloudVertex {
                local_pos: [x1, z0],
            });
            vertices.push(CloudVertex {
                local_pos: [x1, z1],
            });
            vertices.push(CloudVertex {
                local_pos: [x0, z1],
            });
            indices.extend_from_slice(&[base, base + 1, base + 2, base, base + 2, base + 3]);
        }
    }

    (vertices, indices)
}

fn create_terrain_atlas(
    device: &wgpu::Device,
    queue: &wgpu::Queue,
    layout: &wgpu::BindGroupLayout,
    atlas_path: &Path,
) -> TerrainAtlas {
    let (width, height, mut atlas_rgba) = load_terrain_atlas_rgba(atlas_path);
    let mut water_top = WaterSpriteAnimator::new(false, 0x43D1_2F5B);
    let mut water_side = WaterSpriteAnimator::new(true, 0x91B3_07C9);
    let initial_top_tile = water_top.tick();
    let initial_side_tile = water_side.tick();
    patch_procedural_tiles(
        &mut atlas_rgba,
        width,
        &initial_top_tile,
        &initial_side_tile,
    );
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
        format: wgpu::TextureFormat::Rgba8Unorm,
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

    let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
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
    });

    TerrainAtlas {
        bind_group,
        texture,
        water_top,
        water_side,
    }
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

fn load_png_rgba(path: &Path) -> (u32, u32, Vec<u8>) {
    let fallback = || {
        (
            2,
            2,
            vec![
                255, 255, 255, 0, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 0,
            ],
        )
    };

    let candidate_paths = [path.to_path_buf(), Path::new("..").join(path)];
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

// ---------------------------------------------------------------------------
// Procedural tile generation — MC Alpha generates water & lava textures at
// runtime via DynamicTexture sprites. Water top/side here are updated every
// frame using the original CA formulas. Lava remains a static approximation.
// ---------------------------------------------------------------------------

const TILE_PX: u32 = 16;
const WATER_TOP_SPRITE: u16 = 205;
const WATER_SIDE_SPRITE: u16 = 206;
const LAVA_TOP_SPRITE: u16 = 237;
const LAVA_SIDE_SPRITE: u16 = 238;

/// Deterministic hash → f32 in [0, 1].
fn hash_f32(x: u32, y: u32, seed: u32) -> f32 {
    let mut h = x
        .wrapping_mul(374_761_393)
        .wrapping_add(y.wrapping_mul(668_265_263))
        .wrapping_add(seed.wrapping_mul(1_013_904_223));
    h = (h ^ (h >> 13)).wrapping_mul(1_274_126_177);
    h ^= h >> 16;
    (h & 0xFFFF) as f32 / 65535.0
}

/// Cellular-automaton heat diffusion on a 16×16 torus, then normalise to
/// the full [0, 1] range so the colour mapping gets maximum contrast.
fn diffused_noise(seed: u32, iterations: u32) -> [[f32; 16]; 16] {
    let mut grid = [[0.0_f32; 16]; 16];
    for y in 0..16_u32 {
        for x in 0..16_u32 {
            grid[y as usize][x as usize] = hash_f32(x, y, seed);
        }
    }
    for _ in 0..iterations {
        let prev = grid;
        for y in 0..16_usize {
            for x in 0..16_usize {
                let sum = prev[(y + 15) % 16][x]
                    + prev[(y + 1) % 16][x]
                    + prev[y][(x + 15) % 16]
                    + prev[y][(x + 1) % 16];
                grid[y][x] = (prev[y][x] + sum) / 5.0;
            }
        }
    }
    // Normalise to [0, 1].
    let mut lo = f32::MAX;
    let mut hi = f32::MIN;
    for row in &grid {
        for &v in row {
            lo = lo.min(v);
            hi = hi.max(v);
        }
    }
    let span = (hi - lo).max(1e-6);
    for row in &mut grid {
        for v in row {
            *v = (*v - lo) / span;
        }
    }
    grid
}

/// Approximate one frame of MC Alpha's `LavaSprite`.
/// Colour formula (sRGB): R = 155 + v·100, G = v²·170, B = v⁴·128, A = 255.
fn generate_lava_tile() -> [u8; 1024] {
    let grid = diffused_noise(137, 4);
    let mut px = [0_u8; 1024];
    for (y, row) in grid.iter().enumerate() {
        for (x, &v) in row.iter().enumerate() {
            let i = (y * 16 + x) * 4;
            px[i] = (155.0 + v * 100.0) as u8;
            px[i + 1] = (v * v * 170.0) as u8;
            px[i + 2] = (v * v * v * v * 128.0) as u8;
            px[i + 3] = 255;
        }
    }
    px
}

/// Write a 16×16 RGBA tile into the atlas buffer at the given sprite index.
fn blit_tile(atlas: &mut [u8], atlas_width: u32, sprite: u16, tile: &[u8; 1024]) {
    let col = u32::from(sprite % 16);
    let row = u32::from(sprite / 16);
    let stride = atlas_width * 4;
    for ty in 0..TILE_PX {
        for tx in 0..TILE_PX {
            let ax = col * TILE_PX + tx;
            let ay = row * TILE_PX + ty;
            let ai = (ay * stride + ax * 4) as usize;
            let ti = ((ty * TILE_PX + tx) * 4) as usize;
            atlas[ai..ai + 4].copy_from_slice(&tile[ti..ti + 4]);
        }
    }
}

fn blit_tile_replicated(
    atlas: &mut [u8],
    atlas_width: u32,
    sprite: u16,
    replicate: u32,
    tile: &[u8; 1024],
) {
    let base_col = u32::from(sprite % 16);
    let base_row = u32::from(sprite / 16);
    for ry in 0..replicate {
        for rx in 0..replicate {
            if base_col + rx >= 16 || base_row + ry >= 16 {
                continue;
            }
            let replicated_sprite = ((base_row + ry) * 16 + (base_col + rx)) as u16;
            blit_tile(atlas, atlas_width, replicated_sprite, tile);
        }
    }
}

/// Patch placeholder tiles with procedurally generated water & lava textures.
fn patch_procedural_tiles(
    atlas: &mut [u8],
    atlas_width: u32,
    water_top_tile: &[u8; 1024],
    water_side_tile: &[u8; 1024],
) {
    let lava_tile = generate_lava_tile();
    blit_tile(atlas, atlas_width, WATER_TOP_SPRITE, water_top_tile);
    blit_tile_replicated(atlas, atlas_width, WATER_SIDE_SPRITE, 2, water_side_tile);
    blit_tile(atlas, atlas_width, LAVA_TOP_SPRITE, &lava_tile);
    blit_tile_replicated(atlas, atlas_width, LAVA_SIDE_SPRITE, 2, &lava_tile);
}

fn upload_dynamic_sprite(
    queue: &wgpu::Queue,
    texture: &wgpu::Texture,
    sprite: u16,
    replicate: u32,
    tile: &[u8; 1024],
) {
    let base_col = u32::from(sprite % 16);
    let base_row = u32::from(sprite / 16);
    for ry in 0..replicate {
        for rx in 0..replicate {
            if base_col + rx >= 16 || base_row + ry >= 16 {
                continue;
            }
            let replicated_sprite = ((base_row + ry) * 16 + (base_col + rx)) as u16;
            let col = u32::from(replicated_sprite % 16);
            let row = u32::from(replicated_sprite / 16);
            queue.write_texture(
                wgpu::ImageCopyTexture {
                    texture,
                    mip_level: 0,
                    origin: wgpu::Origin3d {
                        x: col * TILE_PX,
                        y: row * TILE_PX,
                        z: 0,
                    },
                    aspect: wgpu::TextureAspect::All,
                },
                tile,
                wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(4 * TILE_PX),
                    rows_per_image: Some(TILE_PX),
                },
                wgpu::Extent3d {
                    width: TILE_PX,
                    height: TILE_PX,
                    depth_or_array_layers: 1,
                },
            );
        }
    }
}

const CHUNK_SHADER: &str = r#"
struct Camera {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
    fog_start: f32,
    fog_color: vec3<f32>,
    fog_end: f32,
    ambient_darkness: f32,
    leaf_cutout_enabled: f32,
    _pad: vec2<f32>,
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
    @location(2) tint: vec4<f32>,
    @location(3) light_data: vec4<u32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) tint: vec4<f32>,
    @location(2) sky_light: f32,
    @location(3) block_light: f32,
    @location(4) face_scale: f32,
    @location(5) world_pos: vec3<f32>,
    @location(6) leaf_marker: f32,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    out.clip_pos = camera.view_proj * vec4<f32>(input.position, 1.0);
    out.uv = input.uv;
    out.tint = input.tint;
    out.sky_light = f32(input.light_data.x);
    out.block_light = f32(input.light_data.y);
    out.face_scale = f32(input.light_data.z) / 255.0;
    out.leaf_marker = f32(input.light_data.w);
    out.world_pos = input.position;
    return out;
}

fn alpha_brightness(level: f32) -> f32 {
    let min_brightness = 0.05;
    let clamped = clamp(level, 0.0, 15.0);
    let g = 1.0 - clamped / 15.0;
    return ((1.0 - g) / (g * 3.0 + 1.0)) * (1.0 - min_brightness) + min_brightness;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    let leaf_fast = input.leaf_marker > 0.5 && camera.leaf_cutout_enabled < 0.5;
    let uv = if (leaf_fast) {
        input.uv + vec2<f32>(1.0 / 16.0, 0.0)
    } else {
        input.uv
    };
    let texel = textureSample(terrain_atlas, terrain_sampler, uv);
    if (texel.a <= 0.1 && !leaf_fast) {
        discard;
    }
    let day_sky = max(input.sky_light - camera.ambient_darkness, 0.0);
    let effective = max(input.block_light, day_sky);
    let brightness = alpha_brightness(effective);
    let shade = input.face_scale * brightness;
    let lit = texel.rgb * input.tint.rgb * shade;
    let alpha = select(texel.a * input.tint.a, input.tint.a, leaf_fast);
    let distance_to_camera = distance(input.world_pos, camera.camera_pos);
    let fog_span = max(camera.fog_end - camera.fog_start, 0.0001);
    let fog_t = clamp((distance_to_camera - camera.fog_start) / fog_span, 0.0, 1.0);
    let color = mix(lit, camera.fog_color, fog_t);
    return vec4<f32>(color, alpha);
}
"#;

const SKY_SHADER: &str = r#"
struct Sky {
    color: vec3<f32>,
    _pad: f32,
};

@group(0) @binding(0)
var<uniform> sky: Sky;

@vertex
fn vs_main(@builtin(vertex_index) vertex_index: u32) -> @builtin(position) vec4<f32> {
    var pos = array<vec2<f32>, 3>(
        vec2<f32>(-1.0, -1.0),
        vec2<f32>(3.0, -1.0),
        vec2<f32>(-1.0, 3.0),
    );
    return vec4<f32>(pos[vertex_index], 0.0, 1.0);
}

@fragment
fn fs_main() -> @location(0) vec4<f32> {
    return vec4<f32>(sky.color, 1.0);
}
"#;

const CLOUD_SHADER: &str = r#"
struct Camera {
    view_proj: mat4x4<f32>,
    camera_pos: vec3<f32>,
    fog_start: f32,
    fog_color: vec3<f32>,
    fog_end: f32,
    ambient_darkness: f32,
    leaf_cutout_enabled: f32,
    _pad: vec2<f32>,
};

struct Cloud {
    origin: vec3<f32>,
    alpha: f32,
    uv_offset: vec2<f32>,
    _pad0: vec2<f32>,
    color: vec3<f32>,
    _pad1: f32,
};

@group(0) @binding(0)
var<uniform> camera: Camera;

@group(1) @binding(0)
var cloud_texture: texture_2d<f32>;

@group(1) @binding(1)
var cloud_sampler: sampler;

@group(2) @binding(0)
var<uniform> cloud: Cloud;

struct VertexIn {
    @location(0) local_pos: vec2<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) world_pos: vec3<f32>,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    let world_pos = vec3<f32>(input.local_pos.x + cloud.origin.x, cloud.origin.y, input.local_pos.y + cloud.origin.z);
    out.clip_pos = camera.view_proj * vec4<f32>(world_pos, 1.0);
    out.uv = world_pos.xz * 0.00048828125 + cloud.uv_offset;
    out.world_pos = world_pos;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    let texel = textureSample(cloud_texture, cloud_sampler, input.uv);
    let alpha = texel.a * cloud.alpha;
    let base = texel.rgb * cloud.color;
    let distance_to_camera = distance(input.world_pos, camera.camera_pos);
    let fog_span = max(camera.fog_end - camera.fog_start, 0.0001);
    let fog_t = clamp((distance_to_camera - camera.fog_start) / fog_span, 0.0, 1.0);
    let color = mix(base, camera.fog_color, fog_t);
    return vec4<f32>(color, alpha);
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
