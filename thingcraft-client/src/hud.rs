use crate::world::BlockRegistry;

/// HUD overlay rendered in screen pixels. Shaders convert positions to NDC.
#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
pub struct HudVertex {
    pub position: [f32; 2],
    pub uv: [f32; 2],
    pub color: [f32; 4],
    pub texture_kind: f32,
}

impl HudVertex {
    pub const ATTRS: [wgpu::VertexAttribute; 4] =
        wgpu::vertex_attr_array![0 => Float32x2, 1 => Float32x2, 2 => Float32x4, 3 => Float32];

    #[must_use]
    pub fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

/// Screen-space uniform: [width, height, 0, 0] (padded to 16-byte alignment).
#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
pub struct HudUniform {
    pub screen_width: f32,
    pub screen_height: f32,
    pub _pad: [f32; 2],
}

pub const HOTBAR_SLOT_COUNT: usize = 9;

const HUD_TEX_GUI: f32 = 0.0;
const HUD_TEX_ICONS: f32 = 1.0;
const HUD_TEX_TERRAIN: f32 = 2.0;

const TEX_SIZE_PX: f32 = 256.0;
const WHITE: [f32; 4] = [1.0, 1.0, 1.0, 1.0];

#[derive(Debug, Clone, Copy)]
pub struct HudState {
    pub selected_slot: usize,
    pub slot_counts: [u8; HOTBAR_SLOT_COUNT],
    pub slot_block_ids: [u8; HOTBAR_SLOT_COUNT],
    pub health: i32,
    pub prev_health: i32,
    pub invulnerable_timer: i32,
    pub sim_ticks: u64,
}

/// Build an Alpha-style HUD (crosshair, hotbar, selected slot, hearts, slot items).
#[must_use]
pub fn build_hud_vertices(
    screen_w: f32,
    screen_h: f32,
    state: &HudState,
    registry: &BlockRegistry,
) -> Vec<HudVertex> {
    let scale = gui_scale(screen_w, screen_h);
    let gui_w = (screen_w / scale).floor();
    let gui_h = (screen_h / scale).floor();
    let center_x = (gui_w / 2.0).floor();

    let mut verts = Vec::with_capacity(512);

    // Crosshair (icons.png: 0,0 -> 16x16).
    push_textured_quad_gui(
        &mut verts,
        center_x - 7.0,
        gui_h / 2.0 - 7.0,
        16.0,
        16.0,
        0.0,
        0.0,
        16.0,
        16.0,
        HUD_TEX_ICONS,
        scale,
        WHITE,
    );

    // Hotbar background + selected slot frame (gui.png).
    let hotbar_x = center_x - 91.0;
    let hotbar_y = gui_h - 22.0;
    push_textured_quad_gui(
        &mut verts,
        hotbar_x,
        hotbar_y,
        182.0,
        22.0,
        0.0,
        0.0,
        182.0,
        22.0,
        HUD_TEX_GUI,
        scale,
        WHITE,
    );
    push_textured_quad_gui(
        &mut verts,
        hotbar_x - 1.0 + state.selected_slot as f32 * 20.0,
        hotbar_y - 1.0,
        24.0,
        22.0,
        0.0,
        22.0,
        24.0,
        22.0,
        HUD_TEX_GUI,
        scale,
        WHITE,
    );

    // Hotbar items (terrain atlas sprites).
    for slot in 0..HOTBAR_SLOT_COUNT {
        if state.slot_counts[slot] == 0 {
            continue;
        }
        let block_id = state.slot_block_ids[slot];
        if !registry.is_defined_block(block_id) {
            continue;
        }
        let sprite = registry.sprite_index_of(block_id);
        let u = (sprite % 16) as f32 * 16.0;
        let v = (sprite / 16) as f32 * 16.0;
        let item_x = center_x - 90.0 + slot as f32 * 20.0 + 2.0;
        let item_y = gui_h - 16.0 - 3.0;
        push_textured_quad_gui(
            &mut verts,
            item_x,
            item_y,
            16.0,
            16.0,
            u,
            v,
            16.0,
            16.0,
            HUD_TEX_TERRAIN,
            scale,
            WHITE,
        );
    }

    // Hearts (icons.png), including invulnerability blink and low-health jitter.
    let mut jitter_rng = (state.sim_ticks as u32).wrapping_mul(312_871);
    let mut flashing = (state.invulnerable_timer / 3) % 2 == 1;
    if state.invulnerable_timer < 10 {
        flashing = false;
    }
    for heart in 0..10 {
        let mut y = gui_h - 32.0;
        if state.health <= 4 {
            y += (next_bit(&mut jitter_rng) as i32) as f32;
        }
        let x = center_x - 91.0 + heart as f32 * 8.0;

        let empty_u = if flashing { 25.0 } else { 16.0 };
        push_textured_quad_gui(
            &mut verts,
            x,
            y,
            9.0,
            9.0,
            empty_u,
            0.0,
            9.0,
            9.0,
            HUD_TEX_ICONS,
            scale,
            WHITE,
        );

        if flashing {
            if heart * 2 + 1 < state.prev_health as usize {
                push_textured_quad_gui(
                    &mut verts,
                    x,
                    y,
                    9.0,
                    9.0,
                    70.0,
                    0.0,
                    9.0,
                    9.0,
                    HUD_TEX_ICONS,
                    scale,
                    WHITE,
                );
            } else if heart * 2 + 1 == state.prev_health as usize {
                push_textured_quad_gui(
                    &mut verts,
                    x,
                    y,
                    9.0,
                    9.0,
                    79.0,
                    0.0,
                    9.0,
                    9.0,
                    HUD_TEX_ICONS,
                    scale,
                    WHITE,
                );
            }
        }

        if heart * 2 + 1 < state.health as usize {
            push_textured_quad_gui(
                &mut verts,
                x,
                y,
                9.0,
                9.0,
                52.0,
                0.0,
                9.0,
                9.0,
                HUD_TEX_ICONS,
                scale,
                WHITE,
            );
        } else if heart * 2 + 1 == state.health as usize {
            push_textured_quad_gui(
                &mut verts,
                x,
                y,
                9.0,
                9.0,
                61.0,
                0.0,
                9.0,
                9.0,
                HUD_TEX_ICONS,
                scale,
                WHITE,
            );
        }
    }

    verts
}

fn gui_scale(screen_w: f32, screen_h: f32) -> f32 {
    let mut scale = 1.0_f32;
    while (screen_w / (scale + 1.0)).floor() >= 320.0 && (screen_h / (scale + 1.0)).floor() >= 240.0
    {
        scale += 1.0;
    }
    scale
}

fn next_bit(state: &mut u32) -> u32 {
    *state = state.wrapping_mul(1_664_525).wrapping_add(1_013_904_223);
    *state >> 31
}

#[allow(clippy::too_many_arguments)]
fn push_textured_quad_gui(
    verts: &mut Vec<HudVertex>,
    gui_x: f32,
    gui_y: f32,
    gui_w: f32,
    gui_h: f32,
    tex_x: f32,
    tex_y: f32,
    tex_w: f32,
    tex_h: f32,
    texture_kind: f32,
    scale: f32,
    color: [f32; 4],
) {
    let x0 = (gui_x * scale).floor();
    let y0 = (gui_y * scale).floor();
    let x1 = ((gui_x + gui_w) * scale).floor();
    let y1 = ((gui_y + gui_h) * scale).floor();

    let u0 = tex_x / TEX_SIZE_PX;
    let v0 = tex_y / TEX_SIZE_PX;
    let u1 = (tex_x + tex_w) / TEX_SIZE_PX;
    let v1 = (tex_y + tex_h) / TEX_SIZE_PX;

    let vtx0 = HudVertex {
        position: [x0, y0],
        uv: [u0, v0],
        color,
        texture_kind,
    };
    let vtx1 = HudVertex {
        position: [x1, y0],
        uv: [u1, v0],
        color,
        texture_kind,
    };
    let vtx2 = HudVertex {
        position: [x1, y1],
        uv: [u1, v1],
        color,
        texture_kind,
    };
    let vtx3 = HudVertex {
        position: [x0, y1],
        uv: [u0, v1],
        color,
        texture_kind,
    };

    verts.push(vtx0);
    verts.push(vtx1);
    verts.push(vtx2);
    verts.push(vtx2);
    verts.push(vtx3);
    verts.push(vtx0);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn gui_scale_matches_alpha_breakpoints() {
        assert_eq!(gui_scale(320.0, 240.0), 1.0);
        assert_eq!(gui_scale(640.0, 480.0), 2.0);
        assert_eq!(gui_scale(1280.0, 720.0), 3.0);
    }

    #[test]
    fn hud_emits_geometry_for_core_layers() {
        let registry = BlockRegistry::alpha_1_2_6();
        let state = HudState {
            selected_slot: 0,
            slot_counts: [64; HOTBAR_SLOT_COUNT],
            slot_block_ids: [3, 1, 4, 12, 13, 17, 5, 9, 50],
            health: 20,
            prev_health: 20,
            invulnerable_timer: 0,
            sim_ticks: 0,
        };
        let verts = build_hud_vertices(1280.0, 720.0, &state, &registry);
        assert!(!verts.is_empty());
        assert!(verts
            .iter()
            .any(|v| (v.texture_kind - HUD_TEX_GUI).abs() < 0.01));
        assert!(verts
            .iter()
            .any(|v| (v.texture_kind - HUD_TEX_ICONS).abs() < 0.01));
        assert!(verts
            .iter()
            .any(|v| (v.texture_kind - HUD_TEX_TERRAIN).abs() < 0.01));
    }
}
