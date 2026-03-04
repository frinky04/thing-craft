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
const CLOUD_LAYER_HEIGHT: f32 = 108.33;
const CLOUD_TILE_SIZE: f32 = 8.0;
const CLOUD_RENDER_RADIUS: i32 = 3;
const CLOUD_THICKNESS: f32 = 4.0;
const CLOUD_EDGE_EPSILON: f32 = 1.0 / 1024.0;
const CLOUD_WORLD_SCALE: f32 = 12.0;
const CLOUD_TEXEL_UV: f32 = 1.0 / 256.0;
const CLOUD_ALPHA: f32 = 0.8;

pub struct Renderer<'w> {
    surface: wgpu::Surface<'w>,
    device: wgpu::Device,
    queue: wgpu::Queue,
    config: wgpu::SurfaceConfiguration,
    size: PhysicalSize<u32>,
    render_pipeline: wgpu::RenderPipeline,
    transparent_pipeline: wgpu::RenderPipeline,
    debug_line_pipeline: wgpu::RenderPipeline,
    block_outline_pipeline: wgpu::RenderPipeline,
    sky_pipeline: wgpu::RenderPipeline,
    sunrise_pipeline: wgpu::RenderPipeline,
    stars_pipeline: wgpu::RenderPipeline,
    cloud_depth_pipeline: wgpu::RenderPipeline,
    cloud_pipeline: wgpu::RenderPipeline,
    camera_buffer: wgpu::Buffer,
    camera_bind_group: wgpu::BindGroup,
    sky_uniform_buffer: wgpu::Buffer,
    sky_bind_group: wgpu::BindGroup,
    sky_dome: SkyDome,
    sunrise_uniform_buffer: wgpu::Buffer,
    sunrise_bind_group: wgpu::BindGroup,
    sunrise_mesh: Option<SunriseMeshGpu>,
    stars_uniform_buffer: wgpu::Buffer,
    stars_bind_group: wgpu::BindGroup,
    starfield: Starfield,
    celestial_pipeline: wgpu::RenderPipeline,
    celestial_bodies: CelestialBodies,
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
    block_outline_mesh: Option<OutlineMeshGpu>,
    camera_frustum: Option<FrustumPlanes>,
    cached_view_proj: [[f32; 4]; 4],
    /// View-proj with translation stripped — for sky dome and celestial bodies (infinite distance).
    cached_sky_view_proj: [[f32; 4]; 4],
    visible_chunk_meshes: usize,
    hud_pipeline: wgpu::RenderPipeline,
    hud_uniform_buffer: wgpu::Buffer,
    hud_uniform_bind_group: wgpu::BindGroup,
    hud_texture_bind_group: wgpu::BindGroup,
    hud_vertex_buffer: Option<wgpu::Buffer>,
    hud_vertex_count: u32,
    fog_color: [f32; 3],
    fog_end: f32,
    sky_color: [f32; 3],
    render_sky: bool,
    cloud_color: [f32; 3],
    cloud_scroll: f32,
    sunrise_color: Option<[f32; 4]>,
    star_brightness: f32,
    ambient_darkness: f32,
    leaf_cutout_enabled: f32,
    camera_pos: [f32; 3],
    time_of_day: f32,
    mesh_buffer_pool: MeshBufferPool,
    entity_sprite_mesh: Option<SceneMeshGpu>,
    shadow_pipeline: wgpu::RenderPipeline,
    shadow_bind_group: wgpu::BindGroup,
    entity_shadow_mesh: Option<SceneMeshGpu>,
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

/// Vertex for block outline rendering: position + RGBA color (supports alpha).
#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct OutlineVertex {
    position: [f32; 3],
    color: [f32; 4],
}

impl OutlineVertex {
    const ATTRS: [wgpu::VertexAttribute; 2] =
        wgpu::vertex_attr_array![0 => Float32x3, 1 => Float32x4];

    #[must_use]
    fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

struct OutlineMeshGpu {
    vertex_buffer: wgpu::Buffer,
    vertex_count: u32,
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
    /// Rotation-only view-proj (no translation — sky at infinite distance).
    sky_view_proj: [[f32; 4]; 4],
    color: [f32; 3],
    fog_end: f32,
    fog_color: [f32; 3],
    _pad0: f32,
    dark_color: [f32; 3],
    _pad1: f32,
}

/// Vertex for the sky dome meshes: world-space position only (color is uniform).
#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct SkyDomeVertex {
    position: [f32; 3],
}

impl SkyDomeVertex {
    const ATTRS: [wgpu::VertexAttribute; 1] = wgpu::vertex_attr_array![0 => Float32x3];

    #[must_use]
    fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

struct SkyDome {
    vertex_buffer: wgpu::Buffer,
    index_buffer: wgpu::Buffer,
    light_index_count: u32,
    dark_index_offset: u32,
    dark_index_count: u32,
}

struct SunriseMeshGpu {
    vertex_buffer: wgpu::Buffer,
    index_buffer: wgpu::Buffer,
    index_count: u32,
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct SunriseUniform {
    sky_view_proj: [[f32; 4]; 4],
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct SunriseVertex {
    position: [f32; 3],
    color: [f32; 4],
}

impl SunriseVertex {
    const ATTRS: [wgpu::VertexAttribute; 2] =
        wgpu::vertex_attr_array![0 => Float32x3, 1 => Float32x4];

    #[must_use]
    fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

struct Starfield {
    vertex_buffer: wgpu::Buffer,
    index_buffer: wgpu::Buffer,
    index_count: u32,
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct StarsUniform {
    sky_view_proj: [[f32; 4]; 4],
    params: [f32; 4], // x = brightness, y = time_angle
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct StarVertex {
    position: [f32; 3],
}

impl StarVertex {
    const ATTRS: [wgpu::VertexAttribute; 1] = wgpu::vertex_attr_array![0 => Float32x3];

    #[must_use]
    fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

/// Celestial bodies (sun + moon): textured quads rotated by time of day.
struct CelestialBodies {
    uniform_bind_group: wgpu::BindGroup,
    texture_bind_group: wgpu::BindGroup,
    uniform_buffer: wgpu::Buffer,
    vertex_buffer: wgpu::Buffer,
    sun_index_offset: u32,
    sun_index_count: u32,
    moon_index_offset: u32,
    moon_index_count: u32,
    index_buffer: wgpu::Buffer,
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct CelestialUniform {
    view_proj: [[f32; 4]; 4],
    /// Rotation angle in radians: timeOfDay * 2π
    time_angle: f32,
    _pad0: f32,
    /// Camera XZ position (celestial bodies follow camera with no parallax).
    camera_xz: [f32; 2],
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct CelestialVertex {
    /// Local position before rotation (x, y, z)
    position: [f32; 3],
    uv: [f32; 2],
    /// 0.0 = sun, 1.0 = moon (used to flip rotation in shader)
    body_id: f32,
}

impl CelestialVertex {
    const ATTRS: [wgpu::VertexAttribute; 3] =
        wgpu::vertex_attr_array![0 => Float32x3, 1 => Float32x2, 2 => Float32];

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
struct CloudUniform {
    camera_origin: [f32; 3],
    alpha: f32,
    uv_base: [f32; 2],
    uv_frac: [f32; 2],
    color: [f32; 3],
    _pad1: f32,
}

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
struct CloudVertex {
    local_pos: [f32; 3],
    uv: [f32; 2],
    shade: f32,
    face_kind: f32,
}

impl CloudVertex {
    const ATTRS: [wgpu::VertexAttribute; 4] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x2,
        2 => Float32,
        3 => Float32
    ];

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
        // Prefer non-sRGB surface: our gamma-space pipeline (matching MC Alpha's OpenGL)
        // writes already sRGB-encoded values. A non-sRGB swapchain writes them byte-for-byte
        // without any additional gamma curve.
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

        tracing::info!(
            "Selected surface format: {:?} (is_srgb: {}, available: {:?})",
            surface_format,
            surface_format.is_srgb(),
            surface_caps.formats,
        );

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
            sky_view_proj: glam::Mat4::IDENTITY.to_cols_array_2d(),
            color: DEFAULT_FOG_COLOR,
            fog_end: 128.0,
            fog_color: DEFAULT_FOG_COLOR,
            _pad0: 0.0,
            dark_color: [0.0; 3],
            _pad1: 0.0,
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
                    visibility: wgpu::ShaderStages::VERTEX_FRAGMENT,
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

        let sky_dome = create_sky_dome(&device);
        let sunrise_uniform = SunriseUniform {
            sky_view_proj: glam::Mat4::IDENTITY.to_cols_array_2d(),
        };
        let sunrise_uniform_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("thingcraft-sunrise-uniform-buffer"),
            contents: bytemuck::bytes_of(&sunrise_uniform),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });
        let sunrise_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-sunrise-bind-group-layout"),
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
        let sunrise_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("thingcraft-sunrise-bind-group"),
            layout: &sunrise_bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: sunrise_uniform_buffer.as_entire_binding(),
            }],
        });
        let stars_uniform = StarsUniform {
            sky_view_proj: glam::Mat4::IDENTITY.to_cols_array_2d(),
            params: [0.0, 0.0, 0.0, 0.0],
        };
        let stars_uniform_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("thingcraft-stars-uniform-buffer"),
            contents: bytemuck::bytes_of(&stars_uniform),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });
        let stars_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-stars-bind-group-layout"),
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
        let stars_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("thingcraft-stars-bind-group"),
            layout: &stars_bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: stars_uniform_buffer.as_entire_binding(),
            }],
        });
        let starfield = create_starfield(&device);

        // Celestial body texture bind group layout (shared with cloud-style layout).
        let celestial_tex_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-celestial-tex-bind-group-layout"),
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
                        ty: wgpu::BindingType::Texture {
                            sample_type: wgpu::TextureSampleType::Float { filterable: true },
                            view_dimension: wgpu::TextureViewDimension::D2,
                            multisampled: false,
                        },
                        count: None,
                    },
                    wgpu::BindGroupLayoutEntry {
                        binding: 2,
                        visibility: wgpu::ShaderStages::FRAGMENT,
                        ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                        count: None,
                    },
                ],
            });
        let celestial_uniform_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-celestial-uniform-bind-group-layout"),
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

        let celestial_bodies = create_celestial_bodies(
            &device,
            &queue,
            &celestial_tex_bind_group_layout,
            &celestial_uniform_bind_group_layout,
            Path::new("resources/minecraft-a1.2.6-client/terrain/sun.png"),
            Path::new("resources/minecraft-a1.2.6-client/terrain/moon.png"),
        );

        let celestial_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-celestial-shader"),
            source: wgpu::ShaderSource::Wgsl(CELESTIAL_SHADER.into()),
        });
        let celestial_pipeline_layout =
            device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                label: Some("thingcraft-celestial-pipeline-layout"),
                bind_group_layouts: &[
                    &celestial_uniform_bind_group_layout,
                    &celestial_tex_bind_group_layout,
                ],
                push_constant_ranges: &[],
            });
        let celestial_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-celestial-pipeline"),
            layout: Some(&celestial_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &celestial_shader,
                entry_point: "vs_main",
                buffers: &[CelestialVertex::layout()],
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
                module: &celestial_shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format: config.format,
                    // Additive blending: GL_ONE, GL_ONE (MC Alpha)
                    blend: Some(wgpu::BlendState {
                        color: wgpu::BlendComponent {
                            src_factor: wgpu::BlendFactor::One,
                            dst_factor: wgpu::BlendFactor::One,
                            operation: wgpu::BlendOperation::Add,
                        },
                        alpha: wgpu::BlendComponent {
                            src_factor: wgpu::BlendFactor::One,
                            dst_factor: wgpu::BlendFactor::One,
                            operation: wgpu::BlendOperation::Add,
                        },
                    }),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: wgpu::PipelineCompilationOptions::default(),
            }),
            multiview: None,
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
                buffers: &[SkyDomeVertex::layout()],
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

        let sunrise_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-sunrise-shader"),
            source: wgpu::ShaderSource::Wgsl(SUNRISE_SHADER.into()),
        });
        let sunrise_pipeline_layout =
            device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                label: Some("thingcraft-sunrise-pipeline-layout"),
                bind_group_layouts: &[&sunrise_bind_group_layout],
                push_constant_ranges: &[],
            });
        let sunrise_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-sunrise-pipeline"),
            layout: Some(&sunrise_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &sunrise_shader,
                entry_point: "vs_main",
                buffers: &[SunriseVertex::layout()],
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
                module: &sunrise_shader,
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

        let stars_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-stars-shader"),
            source: wgpu::ShaderSource::Wgsl(STARS_SHADER.into()),
        });
        let stars_pipeline_layout =
            device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                label: Some("thingcraft-stars-pipeline-layout"),
                bind_group_layouts: &[&stars_bind_group_layout],
                push_constant_ranges: &[],
            });
        let stars_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-stars-pipeline"),
            layout: Some(&stars_pipeline_layout),
            vertex: wgpu::VertexState {
                module: &stars_shader,
                entry_point: "vs_main",
                buffers: &[StarVertex::layout()],
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
                module: &stars_shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format: config.format,
                    blend: Some(wgpu::BlendState {
                        color: wgpu::BlendComponent {
                            src_factor: wgpu::BlendFactor::One,
                            dst_factor: wgpu::BlendFactor::One,
                            operation: wgpu::BlendOperation::Add,
                        },
                        alpha: wgpu::BlendComponent {
                            src_factor: wgpu::BlendFactor::One,
                            dst_factor: wgpu::BlendFactor::One,
                            operation: wgpu::BlendOperation::Add,
                        },
                    }),
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
        let cloud_depth_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("thingcraft-cloud-depth-pipeline"),
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
                depth_write_enabled: true,
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
                    blend: None,
                    write_mask: wgpu::ColorWrites::empty(),
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

        // Block outline pipeline: line list with alpha blending for selection wireframe.
        let outline_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-outline-shader"),
            source: wgpu::ShaderSource::Wgsl(BLOCK_OUTLINE_SHADER.into()),
        });
        let block_outline_pipeline_layout =
            device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                label: Some("thingcraft-outline-pipeline-layout"),
                bind_group_layouts: &[&camera_bind_group_layout],
                push_constant_ranges: &[],
            });
        let block_outline_pipeline =
            device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
                label: Some("thingcraft-outline-pipeline"),
                layout: Some(&block_outline_pipeline_layout),
                vertex: wgpu::VertexState {
                    module: &outline_shader,
                    entry_point: "vs_main",
                    buffers: &[OutlineVertex::layout()],
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
                    module: &outline_shader,
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

        let hud_uniform_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-hud-uniform-bind-group-layout"),
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

        let hud_uniform_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("thingcraft-hud-uniform-bind-group"),
            layout: &hud_uniform_bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: hud_uniform_buffer.as_entire_binding(),
            }],
        });

        let hud_texture_bind_group_layout =
            device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                label: Some("thingcraft-hud-texture-bind-group-layout"),
                entries: &[
                    wgpu::BindGroupLayoutEntry {
                        binding: 0,
                        visibility: wgpu::ShaderStages::FRAGMENT,
                        ty: wgpu::BindingType::Texture {
                            multisampled: false,
                            view_dimension: wgpu::TextureViewDimension::D2,
                            sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        },
                        count: None,
                    },
                    wgpu::BindGroupLayoutEntry {
                        binding: 1,
                        visibility: wgpu::ShaderStages::FRAGMENT,
                        ty: wgpu::BindingType::Texture {
                            multisampled: false,
                            view_dimension: wgpu::TextureViewDimension::D2,
                            sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        },
                        count: None,
                    },
                    wgpu::BindGroupLayoutEntry {
                        binding: 2,
                        visibility: wgpu::ShaderStages::FRAGMENT,
                        ty: wgpu::BindingType::Texture {
                            multisampled: false,
                            view_dimension: wgpu::TextureViewDimension::D2,
                            sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        },
                        count: None,
                    },
                    wgpu::BindGroupLayoutEntry {
                        binding: 3,
                        visibility: wgpu::ShaderStages::FRAGMENT,
                        ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                        count: None,
                    },
                ],
            });
        let (gui_w, gui_h, gui_rgba) =
            load_png_rgba(Path::new("resources/minecraft-a1.2.6-client/gui/gui.png"));
        let gui_texture = device.create_texture(&wgpu::TextureDescriptor {
            label: Some("thingcraft-hud-gui-texture"),
            size: wgpu::Extent3d {
                width: gui_w,
                height: gui_h,
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            // HUD textures in this pipeline are authored for direct gamma-space output to the
            // non-sRGB swapchain (matching the rest of ThingCraft's Alpha-style path).
            format: wgpu::TextureFormat::Rgba8Unorm,
            usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
            view_formats: &[],
        });
        queue.write_texture(
            wgpu::ImageCopyTexture {
                texture: &gui_texture,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            &gui_rgba,
            wgpu::ImageDataLayout {
                offset: 0,
                bytes_per_row: Some(4 * gui_w),
                rows_per_image: Some(gui_h),
            },
            wgpu::Extent3d {
                width: gui_w,
                height: gui_h,
                depth_or_array_layers: 1,
            },
        );

        let (icons_w, icons_h, icons_rgba) =
            load_png_rgba(Path::new("resources/minecraft-a1.2.6-client/gui/icons.png"));
        let icons_texture = device.create_texture(&wgpu::TextureDescriptor {
            label: Some("thingcraft-hud-icons-texture"),
            size: wgpu::Extent3d {
                width: icons_w,
                height: icons_h,
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
                texture: &icons_texture,
                mip_level: 0,
                origin: wgpu::Origin3d::ZERO,
                aspect: wgpu::TextureAspect::All,
            },
            &icons_rgba,
            wgpu::ImageDataLayout {
                offset: 0,
                bytes_per_row: Some(4 * icons_w),
                rows_per_image: Some(icons_h),
            },
            wgpu::Extent3d {
                width: icons_w,
                height: icons_h,
                depth_or_array_layers: 1,
            },
        );
        let hud_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
            label: Some("thingcraft-hud-sampler"),
            mag_filter: wgpu::FilterMode::Nearest,
            min_filter: wgpu::FilterMode::Nearest,
            mipmap_filter: wgpu::FilterMode::Nearest,
            address_mode_u: wgpu::AddressMode::ClampToEdge,
            address_mode_v: wgpu::AddressMode::ClampToEdge,
            address_mode_w: wgpu::AddressMode::ClampToEdge,
            ..Default::default()
        });

        let gui_view = gui_texture.create_view(&wgpu::TextureViewDescriptor::default());
        let icons_view = icons_texture.create_view(&wgpu::TextureViewDescriptor::default());
        let terrain_view = terrain_atlas
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());
        let hud_texture_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("thingcraft-hud-texture-bind-group"),
            layout: &hud_texture_bind_group_layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: wgpu::BindingResource::TextureView(&gui_view),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: wgpu::BindingResource::TextureView(&icons_view),
                },
                wgpu::BindGroupEntry {
                    binding: 2,
                    resource: wgpu::BindingResource::TextureView(&terrain_view),
                },
                wgpu::BindGroupEntry {
                    binding: 3,
                    resource: wgpu::BindingResource::Sampler(&hud_sampler),
                },
            ],
        });

        let hud_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-hud-shader"),
            source: wgpu::ShaderSource::Wgsl(HUD_SHADER.into()),
        });

        let hud_pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("thingcraft-hud-pipeline-layout"),
            bind_group_layouts: &[
                &hud_uniform_bind_group_layout,
                &hud_texture_bind_group_layout,
            ],
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

        // Shadow pipeline: alpha-blended disc projected under dropped items.
        let shadow_shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("thingcraft-shadow-shader"),
            source: wgpu::ShaderSource::Wgsl(SHADOW_SHADER.into()),
        });

        let (shadow_bind_group, shadow_pipeline) = {
            let (sw, sh, shadow_rgba) = load_png_rgba(Path::new(
                "resources/minecraft-a1.2.6-client/misc/shadow.png",
            ));
            let shadow_texture = device.create_texture(&wgpu::TextureDescriptor {
                label: Some("thingcraft-shadow-texture"),
                size: wgpu::Extent3d {
                    width: sw,
                    height: sh,
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
                    texture: &shadow_texture,
                    mip_level: 0,
                    origin: wgpu::Origin3d::ZERO,
                    aspect: wgpu::TextureAspect::All,
                },
                &shadow_rgba,
                wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(4 * sw),
                    rows_per_image: Some(sh),
                },
                wgpu::Extent3d {
                    width: sw,
                    height: sh,
                    depth_or_array_layers: 1,
                },
            );
            let shadow_view = shadow_texture.create_view(&wgpu::TextureViewDescriptor::default());
            let shadow_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
                label: Some("thingcraft-shadow-sampler"),
                address_mode_u: wgpu::AddressMode::ClampToEdge,
                address_mode_v: wgpu::AddressMode::ClampToEdge,
                address_mode_w: wgpu::AddressMode::ClampToEdge,
                mag_filter: wgpu::FilterMode::Linear,
                min_filter: wgpu::FilterMode::Linear,
                mipmap_filter: wgpu::FilterMode::Nearest,
                ..Default::default()
            });
            let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
                label: Some("thingcraft-shadow-bind-group"),
                layout: &terrain_bind_group_layout,
                entries: &[
                    wgpu::BindGroupEntry {
                        binding: 0,
                        resource: wgpu::BindingResource::TextureView(&shadow_view),
                    },
                    wgpu::BindGroupEntry {
                        binding: 1,
                        resource: wgpu::BindingResource::Sampler(&shadow_sampler),
                    },
                ],
            });

            let pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
                label: Some("thingcraft-shadow-pipeline"),
                layout: Some(&pipeline_layout),
                vertex: wgpu::VertexState {
                    module: &shadow_shader,
                    entry_point: "vs_main",
                    buffers: &[MeshVertex::layout()],
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
                    module: &shadow_shader,
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

            (bind_group, pipeline)
        };

        Ok(Self {
            surface,
            device,
            queue,
            config,
            size,
            render_pipeline,
            transparent_pipeline,
            debug_line_pipeline,
            block_outline_pipeline,
            sky_pipeline,
            sunrise_pipeline,
            stars_pipeline,
            cloud_depth_pipeline,
            cloud_pipeline,
            camera_buffer,
            camera_bind_group,
            sky_uniform_buffer,
            sky_bind_group,
            sky_dome,
            sunrise_uniform_buffer,
            sunrise_bind_group,
            sunrise_mesh: None,
            stars_uniform_buffer,
            stars_bind_group,
            starfield,
            celestial_pipeline,
            celestial_bodies,
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
            block_outline_mesh: None,
            camera_frustum: None,
            cached_view_proj: glam::Mat4::IDENTITY.to_cols_array_2d(),
            cached_sky_view_proj: glam::Mat4::IDENTITY.to_cols_array_2d(),
            visible_chunk_meshes: 0,
            hud_pipeline,
            hud_uniform_buffer,
            hud_uniform_bind_group,
            hud_texture_bind_group,
            hud_vertex_buffer: None,
            hud_vertex_count: 0,
            fog_color: DEFAULT_FOG_COLOR,
            fog_end: 128.0,
            sky_color: DEFAULT_FOG_COLOR,
            render_sky: false,
            cloud_color: [1.0, 1.0, 1.0],
            cloud_scroll: 0.0,
            sunrise_color: None,
            star_brightness: 0.0,
            ambient_darkness: 0.0,
            leaf_cutout_enabled: 1.0,
            camera_pos: [0.0; 3],
            time_of_day: 0.0,
            mesh_buffer_pool: MeshBufferPool::default(),
            entity_sprite_mesh: None,
            shadow_pipeline,
            shadow_bind_group,
            entity_shadow_mesh: None,
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
        sky_view_proj: [[f32; 4]; 4],
        camera_pos: [f32; 3],
        fog_start: f32,
        fog_end: f32,
    ) {
        let fog_end_clamped = fog_end.max(fog_start + 0.001);
        let uniform = CameraUniform {
            view_proj,
            camera_pos,
            fog_start,
            fog_color: self.fog_color,
            fog_end: fog_end_clamped,
            ambient_darkness: self.ambient_darkness,
            leaf_cutout_enabled: self.leaf_cutout_enabled,
            _pad: [0.0; 2],
        };
        self.queue
            .write_buffer(&self.camera_buffer, 0, bytemuck::bytes_of(&uniform));
        let matrix = Mat4::from_cols_array_2d(&view_proj);
        self.camera_frustum = Some(FrustumPlanes::from_view_proj(matrix));
        self.camera_pos = camera_pos;
        self.fog_end = fog_end_clamped;
        self.cached_view_proj = view_proj;
        self.cached_sky_view_proj = sky_view_proj;
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
    }

    pub fn set_leaf_cutout_enabled(&mut self, enabled: bool) {
        self.leaf_cutout_enabled = if enabled { 1.0 } else { 0.0 };
    }

    pub fn set_time_of_day(&mut self, time_of_day: f32) {
        self.time_of_day = time_of_day;
        if self.sunrise_color.is_some() {
            self.rebuild_sunrise_mesh();
        }
    }

    pub fn set_sunrise_state(&mut self, sunrise: Option<[f32; 4]>) {
        self.sunrise_color = sunrise;
        self.rebuild_sunrise_mesh();
    }

    pub fn set_cloud_state(&mut self, cloud_color: [f32; 3], cloud_scroll: f32) {
        self.cloud_color = cloud_color;
        self.cloud_scroll = cloud_scroll;
    }

    pub fn set_star_brightness(&mut self, brightness: f32) {
        self.star_brightness = brightness.clamp(0.0, 1.0);
    }

    pub fn set_scene_mesh(&mut self, mesh: &ChunkMesh) {
        self.scene_mesh = self.upload_mesh(mesh, "thingcraft-scene");
    }

    /// Upload entity sprite geometry for this frame (billboarded item quads).
    pub fn update_entity_sprites(&mut self, mesh: &ChunkMesh) {
        self.entity_sprite_mesh = self.upload_mesh(mesh, "thingcraft-entity-sprite");
    }

    /// Upload entity shadow geometry for this frame (ground-projected discs).
    pub fn update_entity_shadows(&mut self, mesh: &ChunkMesh) {
        self.entity_shadow_mesh = self.upload_mesh(mesh, "thingcraft-entity-shadow");
    }

    fn upload_mesh(&self, mesh: &ChunkMesh, label_prefix: &str) -> Option<SceneMeshGpu> {
        if mesh.vertices.is_empty() || mesh.indices.is_empty() {
            return None;
        }

        let vertex_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some(&format!("{label_prefix}-vb")),
                contents: bytemuck::cast_slice(&mesh.vertices),
                usage: wgpu::BufferUsages::VERTEX,
            });

        let index_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some(&format!("{label_prefix}-ib")),
                contents: bytemuck::cast_slice(&mesh.indices),
                usage: wgpu::BufferUsages::INDEX,
            });

        Some(SceneMeshGpu {
            vertex_buffer,
            index_buffer,
            index_count: mesh.indices.len() as u32,
            vertex_bytes: std::mem::size_of_val(mesh.vertices.as_slice()) as u64,
            index_bytes: std::mem::size_of_val(mesh.indices.as_slice()) as u64,
        })
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

    /// Set the block outline wireframe. Pass `None` to hide the outline.
    /// Coordinates are the world-space integer block position of the targeted block.
    pub fn set_block_outline(&mut self, target_block: Option<[i32; 3]>) {
        let Some(pos) = target_block else {
            self.block_outline_mesh = None;
            return;
        };
        let vertices = build_block_outline_vertices(pos);
        let vertex_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-block-outline-vertex-buffer"),
                contents: bytemuck::cast_slice(&vertices),
                usage: wgpu::BufferUsages::VERTEX,
            });
        self.block_outline_mesh = Some(OutlineMeshGpu {
            vertex_buffer,
            vertex_count: vertices.len() as u32,
        });
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

    fn update_sky_uniform(&mut self) {
        let dark_color = [
            self.sky_color[0] * 0.2 + 0.04,
            self.sky_color[1] * 0.2 + 0.04,
            self.sky_color[2] * 0.6 + 0.1,
        ];
        let uniform = SkyUniform {
            sky_view_proj: self.cached_sky_view_proj,
            color: self.sky_color,
            fog_end: self.fog_end,
            fog_color: self.fog_color,
            _pad0: 0.0,
            dark_color,
            _pad1: 0.0,
        };
        self.queue
            .write_buffer(&self.sky_uniform_buffer, 0, bytemuck::bytes_of(&uniform));
    }

    fn update_sunrise_uniform(&mut self) {
        let uniform = SunriseUniform {
            sky_view_proj: self.cached_sky_view_proj,
        };
        self.queue.write_buffer(
            &self.sunrise_uniform_buffer,
            0,
            bytemuck::bytes_of(&uniform),
        );
    }

    fn update_stars_uniform(&mut self) {
        let uniform = StarsUniform {
            sky_view_proj: self.cached_sky_view_proj,
            params: [
                self.star_brightness.clamp(0.0, 1.0),
                self.time_of_day * std::f32::consts::TAU,
                0.0,
                0.0,
            ],
        };
        self.queue
            .write_buffer(&self.stars_uniform_buffer, 0, bytemuck::bytes_of(&uniform));
    }

    fn update_celestial_uniform(&mut self) {
        let uniform = CelestialUniform {
            view_proj: self.cached_sky_view_proj,
            time_angle: self.time_of_day * std::f32::consts::TAU,
            _pad0: 0.0,
            camera_xz: [0.0, 0.0], // not needed — sky_view_proj has no translation
        };
        self.queue.write_buffer(
            &self.celestial_bodies.uniform_buffer,
            0,
            bytemuck::bytes_of(&uniform),
        );
    }

    fn update_cloud_uniform(&mut self) {
        let cloud_x = (self.camera_pos[0] + self.cloud_scroll) / CLOUD_WORLD_SCALE;
        let cloud_z = self.camera_pos[2] / CLOUD_WORLD_SCALE + 0.33;
        let cloud_x_floor = cloud_x.floor();
        let cloud_z_floor = cloud_z.floor();
        let uniform = CloudUniform {
            camera_origin: [self.camera_pos[0], CLOUD_LAYER_HEIGHT, self.camera_pos[2]],
            alpha: CLOUD_ALPHA,
            uv_base: [
                cloud_x_floor * CLOUD_TEXEL_UV,
                cloud_z_floor * CLOUD_TEXEL_UV,
            ],
            uv_frac: [cloud_x - cloud_x_floor, cloud_z - cloud_z_floor],
            color: self.cloud_color,
            _pad1: 0.0,
        };
        self.queue.write_buffer(
            &self.cloud_layer.uniform_buffer,
            0,
            bytemuck::bytes_of(&uniform),
        );
    }

    fn rebuild_sunrise_mesh(&mut self) {
        let Some([r, g, b, a]) = self.sunrise_color else {
            self.sunrise_mesh = None;
            return;
        };

        let (vertices, indices) = build_alpha_sunrise_fan(self.time_of_day, [r, g, b, a]);
        let vertex_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-sunrise-vertex-buffer"),
                contents: bytemuck::cast_slice(&vertices),
                usage: wgpu::BufferUsages::VERTEX,
            });
        let index_buffer = self
            .device
            .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("thingcraft-sunrise-index-buffer"),
                contents: bytemuck::cast_slice(&indices),
                usage: wgpu::BufferUsages::INDEX,
            });
        self.sunrise_mesh = Some(SunriseMeshGpu {
            vertex_buffer,
            index_buffer,
            index_count: indices.len() as u32,
        });
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
        self.update_sky_uniform();
        self.update_sunrise_uniform();
        self.update_stars_uniform();
        self.update_celestial_uniform();

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
                render_pass.set_vertex_buffer(0, self.sky_dome.vertex_buffer.slice(..));
                render_pass.set_index_buffer(
                    self.sky_dome.index_buffer.slice(..),
                    wgpu::IndexFormat::Uint32,
                );
                // Light dome (upper hemisphere)
                render_pass.draw_indexed(0..self.sky_dome.light_index_count, 0, 0..1);
                // Dark dome (lower hemisphere)
                render_pass.draw_indexed(
                    self.sky_dome.dark_index_offset
                        ..self.sky_dome.dark_index_offset + self.sky_dome.dark_index_count,
                    0,
                    0..1,
                );
            }

            if self.render_sky {
                if let Some(sunrise) = &self.sunrise_mesh {
                    render_pass.set_pipeline(&self.sunrise_pipeline);
                    render_pass.set_bind_group(0, &self.sunrise_bind_group, &[]);
                    render_pass.set_vertex_buffer(0, sunrise.vertex_buffer.slice(..));
                    render_pass.set_index_buffer(
                        sunrise.index_buffer.slice(..),
                        wgpu::IndexFormat::Uint32,
                    );
                    render_pass.draw_indexed(0..sunrise.index_count, 0, 0..1);
                }
            }

            // Celestial bodies (sun + moon) — after sky dome, before terrain.
            if self.render_sky {
                render_pass.set_pipeline(&self.celestial_pipeline);
                render_pass.set_bind_group(0, &self.celestial_bodies.uniform_bind_group, &[]);
                render_pass.set_bind_group(1, &self.celestial_bodies.texture_bind_group, &[]);
                render_pass.set_vertex_buffer(0, self.celestial_bodies.vertex_buffer.slice(..));
                render_pass.set_index_buffer(
                    self.celestial_bodies.index_buffer.slice(..),
                    wgpu::IndexFormat::Uint32,
                );
                // Sun
                render_pass.draw_indexed(
                    self.celestial_bodies.sun_index_offset
                        ..self.celestial_bodies.sun_index_offset
                            + self.celestial_bodies.sun_index_count,
                    0,
                    0..1,
                );
                // Moon
                render_pass.draw_indexed(
                    self.celestial_bodies.moon_index_offset
                        ..self.celestial_bodies.moon_index_offset
                            + self.celestial_bodies.moon_index_count,
                    0,
                    0..1,
                );

                if self.star_brightness > 0.0 {
                    render_pass.set_pipeline(&self.stars_pipeline);
                    render_pass.set_bind_group(0, &self.stars_bind_group, &[]);
                    render_pass.set_vertex_buffer(0, self.starfield.vertex_buffer.slice(..));
                    render_pass.set_index_buffer(
                        self.starfield.index_buffer.slice(..),
                        wgpu::IndexFormat::Uint32,
                    );
                    render_pass.draw_indexed(0..self.starfield.index_count, 0, 0..1);
                }
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

            // Entity shadows: ground-projected discs drawn before sprites.
            if let Some(shadow_mesh) = &self.entity_shadow_mesh {
                render_pass.set_pipeline(&self.shadow_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_bind_group(1, &self.shadow_bind_group, &[]);
                render_pass.set_vertex_buffer(0, shadow_mesh.vertex_buffer.slice(..));
                render_pass.set_index_buffer(
                    shadow_mesh.index_buffer.slice(..),
                    wgpu::IndexFormat::Uint32,
                );
                render_pass.draw_indexed(0..shadow_mesh.index_count, 0, 0..1);
            }

            // Entity sprites: drawn after opaque terrain, before transparent pass.
            if let Some(entity_mesh) = &self.entity_sprite_mesh {
                render_pass.set_pipeline(&self.render_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_bind_group(1, &self.terrain_atlas.bind_group, &[]);
                render_pass.set_vertex_buffer(0, entity_mesh.vertex_buffer.slice(..));
                render_pass.set_index_buffer(
                    entity_mesh.index_buffer.slice(..),
                    wgpu::IndexFormat::Uint32,
                );
                render_pass.draw_indexed(0..entity_mesh.index_count, 0, 0..1);
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

            // Block outline: wireframe around targeted block.
            if let Some(outline) = &self.block_outline_mesh {
                render_pass.set_pipeline(&self.block_outline_pipeline);
                render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
                render_pass.set_vertex_buffer(0, outline.vertex_buffer.slice(..));
                render_pass.draw(0..outline.vertex_count, 0..1);
            }

            // Alpha fancy-cloud parity: write cloud depth first (no color), then blend color.
            render_pass.set_pipeline(&self.cloud_depth_pipeline);
            render_pass.set_bind_group(0, &self.camera_bind_group, &[]);
            render_pass.set_bind_group(1, &self.cloud_layer.bind_group, &[]);
            render_pass.set_bind_group(2, &self.cloud_layer.uniform_bind_group, &[]);
            render_pass.set_vertex_buffer(0, self.cloud_layer.vertex_buffer.slice(..));
            render_pass.set_index_buffer(
                self.cloud_layer.index_buffer.slice(..),
                wgpu::IndexFormat::Uint32,
            );
            render_pass.draw_indexed(0..self.cloud_layer.index_count, 0, 0..1);
            render_pass.set_pipeline(&self.cloud_pipeline);
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
            hud_pass.set_bind_group(0, &self.hud_uniform_bind_group, &[]);
            hud_pass.set_bind_group(1, &self.hud_texture_bind_group, &[]);
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

/// Create celestial body (sun + moon) GPU resources.
/// Sun: 30×30 quad at Y=+100. Moon: 20×20 quad at Y=-100 (opposite sun).
/// Both are rotated by `timeOfDay × 360°` around X in the vertex shader.
fn create_celestial_bodies(
    device: &wgpu::Device,
    queue: &wgpu::Queue,
    tex_bind_group_layout: &wgpu::BindGroupLayout,
    uniform_bind_group_layout: &wgpu::BindGroupLayout,
    sun_path: &Path,
    moon_path: &Path,
) -> CelestialBodies {
    // Load sun texture
    let sun_img = image::open(sun_path)
        .unwrap_or_else(|_| {
            // Fallback: bright yellow 32×32
            let mut img = image::RgbaImage::new(32, 32);
            for p in img.pixels_mut() {
                *p = image::Rgba([255, 255, 200, 255]);
            }
            image::DynamicImage::ImageRgba8(img)
        })
        .into_rgba8();
    let sun_tex = create_texture_from_rgba(device, queue, &sun_img, "thingcraft-sun-texture");

    // Load moon texture
    let moon_img = image::open(moon_path)
        .unwrap_or_else(|_| {
            let mut img = image::RgbaImage::new(32, 32);
            for p in img.pixels_mut() {
                *p = image::Rgba([200, 200, 220, 255]);
            }
            image::DynamicImage::ImageRgba8(img)
        })
        .into_rgba8();
    let moon_tex = create_texture_from_rgba(device, queue, &moon_img, "thingcraft-moon-texture");

    // MC Alpha uses GL default (nearest) for celestial textures — crisp pixel art.
    let sampler = device.create_sampler(&wgpu::SamplerDescriptor {
        label: Some("thingcraft-celestial-sampler"),
        mag_filter: wgpu::FilterMode::Nearest,
        min_filter: wgpu::FilterMode::Nearest,
        mipmap_filter: wgpu::FilterMode::Nearest,
        ..Default::default()
    });

    let sun_view = sun_tex.create_view(&wgpu::TextureViewDescriptor::default());
    let moon_view = moon_tex.create_view(&wgpu::TextureViewDescriptor::default());

    let texture_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
        label: Some("thingcraft-celestial-tex-bind-group"),
        layout: tex_bind_group_layout,
        entries: &[
            wgpu::BindGroupEntry {
                binding: 0,
                resource: wgpu::BindingResource::TextureView(&sun_view),
            },
            wgpu::BindGroupEntry {
                binding: 1,
                resource: wgpu::BindingResource::TextureView(&moon_view),
            },
            wgpu::BindGroupEntry {
                binding: 2,
                resource: wgpu::BindingResource::Sampler(&sampler),
            },
        ],
    });

    let uniform = CelestialUniform {
        view_proj: glam::Mat4::IDENTITY.to_cols_array_2d(),
        time_angle: 0.0,
        _pad0: 0.0,
        camera_xz: [0.0; 2],
    };
    let uniform_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-celestial-uniform-buffer"),
        contents: bytemuck::bytes_of(&uniform),
        usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
    });
    let uniform_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
        label: Some("thingcraft-celestial-uniform-bind-group"),
        layout: uniform_bind_group_layout,
        entries: &[wgpu::BindGroupEntry {
            binding: 0,
            resource: uniform_buffer.as_entire_binding(),
        }],
    });

    // Build quad geometry for sun (body_id=0) and moon (body_id=1).
    // Sun: 30×30 at Y=+100, Moon: 20×20 at Y=-100 (MC Alpha WorldRenderer.java:584–607).
    let mut vertices = Vec::new();
    let mut indices: Vec<u32> = Vec::new();

    // Sun quad: centered at (0, +100, 0), size 30×30 in XZ plane
    let r = 30.0_f32;
    let sun_base = vertices.len() as u32;
    vertices.push(CelestialVertex {
        position: [-r, 100.0, -r],
        uv: [0.0, 0.0],
        body_id: 0.0,
    });
    vertices.push(CelestialVertex {
        position: [r, 100.0, -r],
        uv: [1.0, 0.0],
        body_id: 0.0,
    });
    vertices.push(CelestialVertex {
        position: [r, 100.0, r],
        uv: [1.0, 1.0],
        body_id: 0.0,
    });
    vertices.push(CelestialVertex {
        position: [-r, 100.0, r],
        uv: [0.0, 1.0],
        body_id: 0.0,
    });
    let sun_index_offset = indices.len() as u32;
    indices.extend_from_slice(&[
        sun_base,
        sun_base + 1,
        sun_base + 2,
        sun_base + 2,
        sun_base + 3,
        sun_base,
    ]);
    let sun_index_count = 6;

    // Moon quad: centered at (0, -100, 0), size 20×20, reversed winding (viewed from below)
    let r = 20.0_f32;
    let moon_base = vertices.len() as u32;
    // MC Alpha: vertex order is reversed for moon (WorldRenderer.java:601-606)
    vertices.push(CelestialVertex {
        position: [-r, -100.0, r],
        uv: [1.0, 1.0],
        body_id: 1.0,
    });
    vertices.push(CelestialVertex {
        position: [r, -100.0, r],
        uv: [0.0, 1.0],
        body_id: 1.0,
    });
    vertices.push(CelestialVertex {
        position: [r, -100.0, -r],
        uv: [0.0, 0.0],
        body_id: 1.0,
    });
    vertices.push(CelestialVertex {
        position: [-r, -100.0, -r],
        uv: [1.0, 0.0],
        body_id: 1.0,
    });
    let moon_index_offset = indices.len() as u32;
    indices.extend_from_slice(&[
        moon_base,
        moon_base + 1,
        moon_base + 2,
        moon_base + 2,
        moon_base + 3,
        moon_base,
    ]);
    let moon_index_count = 6;

    let vertex_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-celestial-vertex-buffer"),
        contents: bytemuck::cast_slice(&vertices),
        usage: wgpu::BufferUsages::VERTEX,
    });
    let index_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-celestial-index-buffer"),
        contents: bytemuck::cast_slice(&indices),
        usage: wgpu::BufferUsages::INDEX,
    });

    CelestialBodies {
        uniform_bind_group,
        texture_bind_group,
        uniform_buffer,
        vertex_buffer,
        sun_index_offset,
        sun_index_count,
        moon_index_offset,
        moon_index_count,
        index_buffer,
    }
}

/// Create a wgpu texture from an RGBA image.
fn create_texture_from_rgba(
    device: &wgpu::Device,
    queue: &wgpu::Queue,
    img: &image::RgbaImage,
    label: &str,
) -> wgpu::Texture {
    let (width, height) = img.dimensions();
    let texture = device.create_texture(&wgpu::TextureDescriptor {
        label: Some(label),
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
            texture: &texture,
            mip_level: 0,
            origin: wgpu::Origin3d::ZERO,
            aspect: wgpu::TextureAspect::All,
        },
        img.as_raw(),
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
    texture
}

/// Create the sky dome geometry: a flat grid at Y=+16 (light dome) and Y=-16 (dark dome).
/// MC Alpha `WorldRenderer.java:129–160`: tiles are 64×64 blocks, grid extends ±384 blocks.
fn create_sky_dome(device: &wgpu::Device) -> SkyDome {
    let tile = 64.0_f32;
    let extent = 6; // tiles in each direction → ±384 blocks
    let mut vertices = Vec::new();
    let mut indices: Vec<u32> = Vec::new();

    // Light dome at Y = +16
    let y_light = 16.0_f32;
    for iz in -extent..=extent {
        for ix in -extent..=extent {
            vertices.push(SkyDomeVertex {
                position: [ix as f32 * tile, y_light, iz as f32 * tile],
            });
        }
    }
    let cols = (extent * 2 + 1) as u32;
    for iz in 0..(cols - 1) {
        for ix in 0..(cols - 1) {
            let tl = iz * cols + ix;
            let tr = tl + 1;
            let bl = tl + cols;
            let br = bl + 1;
            indices.push(tl);
            indices.push(bl);
            indices.push(tr);
            indices.push(tr);
            indices.push(bl);
            indices.push(br);
        }
    }
    let light_index_count = indices.len() as u32;

    // Dark dome at Y = -16
    let y_dark = -16.0_f32;
    let dark_vertex_offset = vertices.len() as u32;
    for iz in -extent..=extent {
        for ix in -extent..=extent {
            vertices.push(SkyDomeVertex {
                position: [ix as f32 * tile, y_dark, iz as f32 * tile],
            });
        }
    }
    let dark_index_offset = indices.len() as u32;
    for iz in 0..(cols - 1) {
        for ix in 0..(cols - 1) {
            let tl = dark_vertex_offset + iz * cols + ix;
            let tr = tl + 1;
            let bl = tl + cols;
            let br = bl + 1;
            // Wind opposite direction (viewed from below)
            indices.push(tl);
            indices.push(tr);
            indices.push(bl);
            indices.push(tr);
            indices.push(br);
            indices.push(bl);
        }
    }
    let dark_index_count = indices.len() as u32 - dark_index_offset;

    let vertex_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-sky-dome-vertex-buffer"),
        contents: bytemuck::cast_slice(&vertices),
        usage: wgpu::BufferUsages::VERTEX,
    });
    let index_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-sky-dome-index-buffer"),
        contents: bytemuck::cast_slice(&indices),
        usage: wgpu::BufferUsages::INDEX,
    });

    SkyDome {
        vertex_buffer,
        index_buffer,
        light_index_count,
        dark_index_offset,
        dark_index_count,
    }
}

fn create_starfield(device: &wgpu::Device) -> Starfield {
    let (vertices, indices) = build_alpha_starfield_mesh();
    let vertex_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-stars-vertex-buffer"),
        contents: bytemuck::cast_slice(&vertices),
        usage: wgpu::BufferUsages::VERTEX,
    });
    let index_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
        label: Some("thingcraft-stars-index-buffer"),
        contents: bytemuck::cast_slice(&indices),
        usage: wgpu::BufferUsages::INDEX,
    });
    Starfield {
        vertex_buffer,
        index_buffer,
        index_count: indices.len() as u32,
    }
}

fn build_alpha_starfield_mesh() -> (Vec<StarVertex>, Vec<u32>) {
    let mut rng = JavaRandom::new(10_842);
    let mut vertices = Vec::with_capacity(1_500 * 4);
    let mut indices = Vec::with_capacity(1_500 * 6);

    for _ in 0..1_500 {
        let mut dx = rng.next_f32() as f64 * 2.0 - 1.0;
        let mut dy = rng.next_f32() as f64 * 2.0 - 1.0;
        let mut dz = rng.next_f32() as f64 * 2.0 - 1.0;
        let size = 0.25 + rng.next_f32() as f64 * 0.25;
        let mut length_sq = dx * dx + dy * dy + dz * dz;
        if length_sq >= 1.0 || length_sq <= 0.01 {
            continue;
        }
        length_sq = 1.0 / length_sq.sqrt();
        dx *= length_sq;
        dy *= length_sq;
        dz *= length_sq;

        let px = dx * 100.0;
        let py = dy * 100.0;
        let pz = dz * 100.0;
        let yaw = dx.atan2(dz);
        let sin_yaw = yaw.sin();
        let cos_yaw = yaw.cos();
        let pitch = (dx * dx + dz * dz).sqrt().atan2(dy);
        let sin_pitch = pitch.sin();
        let cos_pitch = pitch.cos();
        let roll = rng.next_f64() * std::f64::consts::PI * 2.0;
        let sin_roll = roll.sin();
        let cos_roll = roll.cos();

        let base = vertices.len() as u32;
        for corner in 0..4 {
            let x = ((corner & 2) as f64 - 1.0) * size;
            let y = (((corner + 1) & 2) as f64 - 1.0) * size;
            let z = 0.0_f64;
            let aa = x * cos_roll - y * sin_roll;
            let ac = y * cos_roll + x * sin_roll;
            let ad = aa * sin_pitch + z * cos_pitch;
            let ae = z * sin_pitch - aa * cos_pitch;
            let af = ae * sin_yaw - ac * cos_yaw;
            let ag = ad;
            let ah = ac * sin_yaw + ae * cos_yaw;
            vertices.push(StarVertex {
                position: [(px + af) as f32, (py + ag) as f32, (pz + ah) as f32],
            });
        }
        indices.extend_from_slice(&[base, base + 1, base + 2, base, base + 2, base + 3]);
    }

    (vertices, indices)
}

fn build_alpha_sunrise_fan(time_of_day: f32, color: [f32; 4]) -> (Vec<SunriseVertex>, Vec<u32>) {
    let mut vertices = Vec::with_capacity(18);
    let mut indices = Vec::with_capacity(16 * 3);

    let mut push_vertex = |position: [f32; 3], alpha: f32| {
        vertices.push(SunriseVertex {
            position,
            color: [color[0], color[1], color[2], alpha],
        });
    };

    push_vertex(
        rotate_sunrise_vertex([0.0, 100.0, 0.0], time_of_day),
        color[3],
    );
    for q in 0..=16 {
        let angle = q as f32 * std::f32::consts::TAU / 16.0;
        let u = angle.sin();
        let v = angle.cos();
        let ring = [u * 120.0, v * 120.0, -v * 40.0 * color[3]];
        push_vertex(rotate_sunrise_vertex(ring, time_of_day), 0.0);
    }

    for i in 1..=16_u32 {
        indices.extend_from_slice(&[0, i, i + 1]);
    }

    (vertices, indices)
}

fn rotate_sunrise_vertex(position: [f32; 3], time_of_day: f32) -> [f32; 3] {
    // WorldRenderer.renderSky: Rx(90deg), then Rz(180deg) for evening half.
    let rotated_x = [position[0], -position[2], position[1]];
    if time_of_day > 0.5 {
        [-rotated_x[0], -rotated_x[1], rotated_x[2]]
    } else {
        rotated_x
    }
}

struct JavaRandom {
    seed: u64,
}

impl JavaRandom {
    const MULTIPLIER: u64 = 0x5DEECE66D;
    const ADDEND: u64 = 0xB;
    const MASK: u64 = (1_u64 << 48) - 1;

    fn new(seed: i64) -> Self {
        Self {
            seed: ((seed as u64) ^ Self::MULTIPLIER) & Self::MASK,
        }
    }

    fn next_bits(&mut self, bits: u32) -> u32 {
        self.seed = (self
            .seed
            .wrapping_mul(Self::MULTIPLIER)
            .wrapping_add(Self::ADDEND))
            & Self::MASK;
        (self.seed >> (48 - bits)) as u32
    }

    fn next_f32(&mut self) -> f32 {
        self.next_bits(24) as f32 / (1_u32 << 24) as f32
    }

    fn next_f64(&mut self) -> f64 {
        let high = self.next_bits(26) as u64;
        let low = self.next_bits(27) as u64;
        ((high << 27) | low) as f64 / (1_u64 << 53) as f64
    }
}

/// Build the 12-edge wireframe for a block outline, inflated by 0.002 to prevent z-fighting.
/// Color: black at 40% opacity, matching MC Alpha's `renderBlockOutline`.
fn build_block_outline_vertices(block: [i32; 3]) -> Vec<OutlineVertex> {
    const INFLATE: f32 = 0.002;
    const COLOR: [f32; 4] = [0.0, 0.0, 0.0, 0.4];

    let min = [
        block[0] as f32 - INFLATE,
        block[1] as f32 - INFLATE,
        block[2] as f32 - INFLATE,
    ];
    let max = [
        block[0] as f32 + 1.0 + INFLATE,
        block[1] as f32 + 1.0 + INFLATE,
        block[2] as f32 + 1.0 + INFLATE,
    ];

    let p000 = [min[0], min[1], min[2]];
    let p100 = [max[0], min[1], min[2]];
    let p110 = [max[0], min[1], max[2]];
    let p010 = [min[0], min[1], max[2]];
    let p001 = [min[0], max[1], min[2]];
    let p101 = [max[0], max[1], min[2]];
    let p111 = [max[0], max[1], max[2]];
    let p011 = [min[0], max[1], max[2]];

    let mut verts = Vec::with_capacity(24);
    let mut line = |a: [f32; 3], b: [f32; 3]| {
        verts.push(OutlineVertex {
            position: a,
            color: COLOR,
        });
        verts.push(OutlineVertex {
            position: b,
            color: COLOR,
        });
    };

    // Bottom ring
    line(p000, p100);
    line(p100, p110);
    line(p110, p010);
    line(p010, p000);
    // Top ring
    line(p001, p101);
    line(p101, p111);
    line(p111, p011);
    line(p011, p001);
    // Vertical edges
    line(p000, p001);
    line(p100, p101);
    line(p110, p111);
    line(p010, p011);

    verts
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
        mag_filter: wgpu::FilterMode::Nearest,
        min_filter: wgpu::FilterMode::Nearest,
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
        camera_origin: [0.0, CLOUD_LAYER_HEIGHT, 0.0],
        alpha: CLOUD_ALPHA,
        uv_base: [0.0, 0.0],
        uv_frac: [0.0, 0.0],
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

    let (vertices, indices) = build_fancy_cloud_mesh();
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

fn build_fancy_cloud_mesh() -> (Vec<CloudVertex>, Vec<u32>) {
    let mut vertices = Vec::new();
    let mut indices = Vec::new();
    let tile = CLOUD_TILE_SIZE;
    let y0 = 0.0_f32;
    let y1 = CLOUD_THICKNESS;
    let y_top = y1 - CLOUD_EDGE_EPSILON;
    let max_tile = CLOUD_RENDER_RADIUS;

    for tile_z in (-max_tile + 1)..=max_tile {
        for tile_x in (-max_tile + 1)..=max_tile {
            let ac = tile_x as f32 * tile;
            let ad = tile_z as f32 * tile;
            let x0 = ac;
            let x1 = ac + tile;
            let z0 = ad;
            let z1 = ad + tile;

            append_cloud_quad(
                &mut vertices,
                &mut indices,
                [x0, y0, z1],
                [x1, y0, z1],
                [x1, y0, z0],
                [x0, y0, z0],
                [x0 * CLOUD_TEXEL_UV, z1 * CLOUD_TEXEL_UV],
                [x1 * CLOUD_TEXEL_UV, z1 * CLOUD_TEXEL_UV],
                [x1 * CLOUD_TEXEL_UV, z0 * CLOUD_TEXEL_UV],
                [x0 * CLOUD_TEXEL_UV, z0 * CLOUD_TEXEL_UV],
                0.7,
                0.0,
            );
            append_cloud_quad(
                &mut vertices,
                &mut indices,
                [x0, y_top, z1],
                [x1, y_top, z1],
                [x1, y_top, z0],
                [x0, y_top, z0],
                [x0 * CLOUD_TEXEL_UV, z1 * CLOUD_TEXEL_UV],
                [x1 * CLOUD_TEXEL_UV, z1 * CLOUD_TEXEL_UV],
                [x1 * CLOUD_TEXEL_UV, z0 * CLOUD_TEXEL_UV],
                [x0 * CLOUD_TEXEL_UV, z0 * CLOUD_TEXEL_UV],
                1.0,
                1.0,
            );

            if tile_x > -1 {
                for strip in 0..(tile as i32) {
                    let x = ac + strip as f32;
                    let uv_x = (ac + strip as f32 + 0.5) * CLOUD_TEXEL_UV;
                    append_cloud_quad(
                        &mut vertices,
                        &mut indices,
                        [x, y0, z1],
                        [x, y1, z1],
                        [x, y1, z0],
                        [x, y0, z0],
                        [uv_x, z1 * CLOUD_TEXEL_UV],
                        [uv_x, z1 * CLOUD_TEXEL_UV],
                        [uv_x, z0 * CLOUD_TEXEL_UV],
                        [uv_x, z0 * CLOUD_TEXEL_UV],
                        0.9,
                        2.0,
                    );
                }
            }
            if tile_x <= 1 {
                for strip in 0..(tile as i32) {
                    let x = ac + strip as f32 + 1.0 - CLOUD_EDGE_EPSILON;
                    let uv_x = (ac + strip as f32 + 0.5) * CLOUD_TEXEL_UV;
                    append_cloud_quad(
                        &mut vertices,
                        &mut indices,
                        [x, y0, z1],
                        [x, y1, z1],
                        [x, y1, z0],
                        [x, y0, z0],
                        [uv_x, z1 * CLOUD_TEXEL_UV],
                        [uv_x, z1 * CLOUD_TEXEL_UV],
                        [uv_x, z0 * CLOUD_TEXEL_UV],
                        [uv_x, z0 * CLOUD_TEXEL_UV],
                        0.9,
                        2.0,
                    );
                }
            }

            if tile_z > -1 {
                for strip in 0..(tile as i32) {
                    let z = ad + strip as f32;
                    let uv_z = (ad + strip as f32 + 0.5) * CLOUD_TEXEL_UV;
                    append_cloud_quad(
                        &mut vertices,
                        &mut indices,
                        [x0, y1, z],
                        [x1, y1, z],
                        [x1, y0, z],
                        [x0, y0, z],
                        [x0 * CLOUD_TEXEL_UV, uv_z],
                        [x1 * CLOUD_TEXEL_UV, uv_z],
                        [x1 * CLOUD_TEXEL_UV, uv_z],
                        [x0 * CLOUD_TEXEL_UV, uv_z],
                        0.8,
                        2.0,
                    );
                }
            }
            if tile_z <= 1 {
                for strip in 0..(tile as i32) {
                    let z = ad + strip as f32 + 1.0 - CLOUD_EDGE_EPSILON;
                    let uv_z = (ad + strip as f32 + 0.5) * CLOUD_TEXEL_UV;
                    append_cloud_quad(
                        &mut vertices,
                        &mut indices,
                        [x0, y1, z],
                        [x1, y1, z],
                        [x1, y0, z],
                        [x0, y0, z],
                        [x0 * CLOUD_TEXEL_UV, uv_z],
                        [x1 * CLOUD_TEXEL_UV, uv_z],
                        [x1 * CLOUD_TEXEL_UV, uv_z],
                        [x0 * CLOUD_TEXEL_UV, uv_z],
                        0.8,
                        2.0,
                    );
                }
            }
        }
    }

    (vertices, indices)
}

#[allow(clippy::too_many_arguments)]
fn append_cloud_quad(
    vertices: &mut Vec<CloudVertex>,
    indices: &mut Vec<u32>,
    p0: [f32; 3],
    p1: [f32; 3],
    p2: [f32; 3],
    p3: [f32; 3],
    uv0: [f32; 2],
    uv1: [f32; 2],
    uv2: [f32; 2],
    uv3: [f32; 2],
    shade: f32,
    face_kind: f32,
) {
    let base = vertices.len() as u32;
    vertices.push(CloudVertex {
        local_pos: p0,
        uv: uv0,
        shade,
        face_kind,
    });
    vertices.push(CloudVertex {
        local_pos: p1,
        uv: uv1,
        shade,
        face_kind,
    });
    vertices.push(CloudVertex {
        local_pos: p2,
        uv: uv2,
        shade,
        face_kind,
    });
    vertices.push(CloudVertex {
        local_pos: p3,
        uv: uv3,
        shade,
        face_kind,
    });
    indices.extend_from_slice(&[base, base + 1, base + 2, base, base + 2, base + 3]);
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

const SHADOW_SHADER: &str = r#"
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
@group(0) @binding(0) var<uniform> camera: Camera;
@group(1) @binding(0) var shadow_tex: texture_2d<f32>;
@group(1) @binding(1) var shadow_sampler: sampler;

struct VertexIn {
    @location(0) position: vec3<f32>,
    @location(1) uv: vec2<f32>,
    @location(2) tint_rgba: vec4<f32>,
    @location(3) light_data: vec4<u32>,
};
struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) alpha: f32,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    out.clip_pos = camera.view_proj * vec4<f32>(input.position, 1.0);
    out.uv = input.uv;
    out.alpha = input.tint_rgba.a;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    let texel = textureSample(shadow_tex, shadow_sampler, input.uv);
    return vec4<f32>(0.0, 0.0, 0.0, texel.a * input.alpha);
}
"#;

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
    var uv = input.uv;
    if (leaf_fast) {
        uv = input.uv + vec2<f32>(1.0 / 16.0, 0.0);
    }
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
    sky_view_proj: mat4x4<f32>,
    color: vec3<f32>,
    fog_end: f32,
    fog_color: vec3<f32>,
    _pad0: f32,
    dark_color: vec3<f32>,
    _pad1: f32,
};

@group(0) @binding(0)
var<uniform> sky: Sky;

struct VertexIn {
    @location(0) position: vec3<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) local_pos: vec3<f32>,
    @location(1) dome_y: f32,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    // Rotation-only view-proj: dome stays at infinite distance, no camera offset needed.
    out.clip_pos = sky.sky_view_proj * vec4<f32>(input.position, 1.0);
    out.local_pos = input.position;
    out.dome_y = input.position.y;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    // Horizontal distance from dome center to this fragment.
    let dist = sqrt(input.local_pos.x * input.local_pos.x + input.local_pos.z * input.local_pos.z);

    // MC Alpha sky fog: start=0, end=renderDistance*0.8
    let sky_fog_end = sky.fog_end * 0.8;
    let fog_t = clamp(dist / max(sky_fog_end, 0.001), 0.0, 1.0);

    // Select dome color based on whether this is light or dark dome.
    let dome_color = select(sky.dark_color, sky.color, input.dome_y > 0.0);
    let color = mix(dome_color, sky.fog_color, fog_t);
    return vec4<f32>(color, 1.0);
}
"#;

const CELESTIAL_SHADER: &str = r#"
struct CelestialUniforms {
    view_proj: mat4x4<f32>,
    time_angle: f32,
    _pad0: f32,
    camera_pos: vec2<f32>,
};

@group(0) @binding(0)
var<uniform> uniforms: CelestialUniforms;

@group(1) @binding(0)
var sun_texture: texture_2d<f32>;

@group(1) @binding(1)
var moon_texture: texture_2d<f32>;

@group(1) @binding(2)
var celestial_sampler: sampler;

struct VertexIn {
    @location(0) position: vec3<f32>,
    @location(1) uv: vec2<f32>,
    @location(2) body_id: f32,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) body_id: f32,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    // Rotate around X axis by time_angle (MC Alpha: timeOfDay * 360°).
    let angle = uniforms.time_angle;
    let ca = cos(angle);
    let sa = sin(angle);
    let rotated = vec3<f32>(
        input.position.x,
        input.position.y * ca - input.position.z * sa,
        input.position.y * sa + input.position.z * ca,
    );

    var out: VertexOut;
    // view_proj has no translation — celestial bodies are at infinite distance.
    out.clip_pos = uniforms.view_proj * vec4<f32>(rotated, 1.0);
    out.uv = input.uv;
    out.body_id = input.body_id;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    // Sample the correct texture based on body_id (0 = sun, 1 = moon).
    let sun_color = textureSample(sun_texture, celestial_sampler, input.uv);
    let moon_color = textureSample(moon_texture, celestial_sampler, input.uv);
    let color = select(sun_color, moon_color, input.body_id > 0.5);
    // Discard fully transparent pixels to avoid additive blending artifacts.
    if (color.a < 0.01) {
        discard;
    }
    return color;
}
"#;

const SUNRISE_SHADER: &str = r#"
struct Sunrise {
    sky_view_proj: mat4x4<f32>,
};

@group(0) @binding(0)
var<uniform> sunrise: Sunrise;

struct VertexIn {
    @location(0) position: vec3<f32>,
    @location(1) color: vec4<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) color: vec4<f32>,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    out.clip_pos = sunrise.sky_view_proj * vec4<f32>(input.position, 1.0);
    out.color = input.color;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    return input.color;
}
"#;

const STARS_SHADER: &str = r#"
struct Stars {
    sky_view_proj: mat4x4<f32>,
    params: vec4<f32>, // x = brightness, y = time_angle
};

@group(0) @binding(0)
var<uniform> stars: Stars;

struct VertexIn {
    @location(0) position: vec3<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    let angle = stars.params.y;
    let ca = cos(angle);
    let sa = sin(angle);
    let rotated = vec3<f32>(
        input.position.x,
        input.position.y * ca - input.position.z * sa,
        input.position.y * sa + input.position.z * ca,
    );
    out.clip_pos = stars.sky_view_proj * vec4<f32>(rotated, 1.0);
    return out;
}

@fragment
fn fs_main(_input: VertexOut) -> @location(0) vec4<f32> {
    let b = clamp(stars.params.x, 0.0, 1.0);
    return vec4<f32>(b, b, b, b);
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
    camera_origin: vec3<f32>,
    alpha: f32,
    uv_base: vec2<f32>,
    uv_frac: vec2<f32>,
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
    @location(0) local_pos: vec3<f32>,
    @location(1) uv: vec2<f32>,
    @location(2) shade: f32,
    @location(3) face_kind: f32,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) world_pos: vec3<f32>,
    @location(2) shade: f32,
    @location(3) face_kind: f32,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    var out: VertexOut;
    let world_pos = vec3<f32>(
        (input.local_pos.x - cloud.uv_frac.x) * 12.0 + cloud.camera_origin.x,
        input.local_pos.y + cloud.camera_origin.y,
        (input.local_pos.z - cloud.uv_frac.y) * 12.0 + cloud.camera_origin.z
    );
    out.clip_pos = camera.view_proj * vec4<f32>(world_pos, 1.0);
    out.uv = input.uv + cloud.uv_base;
    out.world_pos = world_pos;
    out.shade = input.shade;
    out.face_kind = input.face_kind;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    // Face visibility parity with Alpha fancy clouds.
    if (input.face_kind < 0.5 && camera.camera_pos.y > 113.33) {
        discard;
    }
    if (input.face_kind > 0.5 && input.face_kind < 1.5 && camera.camera_pos.y < 103.33) {
        discard;
    }
    let texel = textureSample(cloud_texture, cloud_sampler, input.uv);
    if (texel.a <= 0.1) {
        discard;
    }
    let alpha = texel.a * cloud.alpha;
    let base = texel.rgb * cloud.color * input.shade;
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

const BLOCK_OUTLINE_SHADER: &str = r#"
struct Camera {
    view_proj: mat4x4<f32>,
};

@group(0) @binding(0)
var<uniform> camera: Camera;

struct VertexIn {
    @location(0) position: vec3<f32>,
    @location(1) color: vec4<f32>,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) color: vec4<f32>,
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
    return input.color;
}
"#;

const HUD_SHADER: &str = r#"
struct HudScreen {
    screen_width: f32,
    screen_height: f32,
};

@group(0) @binding(0)
var<uniform> screen: HudScreen;
@group(1) @binding(0) var hud_gui_tex: texture_2d<f32>;
@group(1) @binding(1) var hud_icons_tex: texture_2d<f32>;
@group(1) @binding(2) var hud_terrain_tex: texture_2d<f32>;
@group(1) @binding(3) var hud_sampler: sampler;

struct VertexIn {
    @location(0) position: vec2<f32>,
    @location(1) uv: vec2<f32>,
    @location(2) color: vec4<f32>,
    @location(3) texture_kind: f32,
};

struct VertexOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) color: vec4<f32>,
    @location(2) texture_kind: f32,
};

@vertex
fn vs_main(input: VertexIn) -> VertexOut {
    // Convert screen-pixel coords to NDC: x: [0, width] -> [-1, 1], y: [0, height] -> [1, -1].
    let ndc_x = (input.position.x / screen.screen_width) * 2.0 - 1.0;
    let ndc_y = 1.0 - (input.position.y / screen.screen_height) * 2.0;

    var out: VertexOut;
    out.clip_pos = vec4<f32>(ndc_x, ndc_y, 0.0, 1.0);
    out.uv = input.uv;
    out.color = input.color;
    out.texture_kind = input.texture_kind;
    return out;
}

@fragment
fn fs_main(input: VertexOut) -> @location(0) vec4<f32> {
    if (input.texture_kind < -0.5) {
        return input.color;
    }
    var texel = textureSample(hud_gui_tex, hud_sampler, input.uv);
    if (input.texture_kind > 0.5 && input.texture_kind < 1.5) {
        texel = textureSample(hud_icons_tex, hud_sampler, input.uv);
    } else if (input.texture_kind >= 1.5) {
        texel = textureSample(hud_terrain_tex, hud_sampler, input.uv);
    }
    if (texel.a <= 0.01) {
        discard;
    }
    return texel * input.color;
}
"#;

#[cfg(test)]
mod tests {
    use glam::{Mat4, Vec3};

    use super::{
        build_alpha_starfield_mesh, build_alpha_sunrise_fan, build_chunk_border_vertices,
        build_chunk_status_vertices, build_fancy_cloud_mesh, chunk_aabb, FrustumPlanes,
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

    #[test]
    fn alpha_starfield_mesh_contains_expected_geometry() {
        let (vertices, indices) = build_alpha_starfield_mesh();
        assert!(!vertices.is_empty());
        assert!(!indices.is_empty());
        assert_eq!(vertices.len() % 4, 0);
        assert_eq!(indices.len() % 6, 0);
    }

    #[test]
    fn fancy_cloud_mesh_contains_top_bottom_and_side_faces() {
        let (vertices, indices) = build_fancy_cloud_mesh();
        assert!(!vertices.is_empty());
        assert!(!indices.is_empty());
        assert!(vertices
            .iter()
            .any(|v| (v.face_kind - 0.0).abs() < f32::EPSILON));
        assert!(vertices
            .iter()
            .any(|v| (v.face_kind - 1.0).abs() < f32::EPSILON));
        assert!(vertices
            .iter()
            .any(|v| (v.face_kind - 2.0).abs() < f32::EPSILON));
    }

    #[test]
    fn sunrise_fan_has_center_alpha_and_outer_fade() {
        let (vertices, indices) = build_alpha_sunrise_fan(0.0, [1.0, 0.6, 0.2, 0.8]);
        assert_eq!(vertices.len(), 18);
        assert_eq!(indices.len(), 48);
        assert!((vertices[0].color[3] - 0.8).abs() < 0.0001);
        assert!(vertices.iter().skip(1).all(|v| v.color[3] <= 0.0001));
    }
}
