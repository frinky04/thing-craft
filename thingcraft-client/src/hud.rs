use crate::inventory::{
    inventory_layout, slot_gui_xy, ItemKey, PlayerInventoryState, PlayerSlot, ARMOR_SLOT_COUNT,
    MAIN_SLOT_COUNT,
};
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
pub const HUD_TEX_INVENTORY: f32 = 3.0;

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
    pub breath: i32,
    pub breath_capacity: i32,
    pub submerged_in_water: bool,
    pub armor_points: i32,
    pub is_dead: bool,
    pub death_ticks: i32,
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

    // Hotbar items (Alpha-like item rendering path: 3D mini block for solid cubes,
    // flat sprite for cutout/liquid/non-solid blocks).
    for slot in 0..HOTBAR_SLOT_COUNT {
        if state.slot_counts[slot] == 0 {
            continue;
        }
        let block_id = state.slot_block_ids[slot];
        if !registry.is_defined_block(block_id) {
            continue;
        }
        let item_x = center_x - 90.0 + slot as f32 * 20.0 + 2.0;
        let item_y = gui_h - 16.0 - 3.0;
        push_hotbar_item_vertices(&mut verts, item_x, item_y, block_id, registry, scale);
    }

    // Armor row (icons.png, shown only when armor points > 0).
    if state.armor_points > 0 {
        for s in 0..10 {
            let x = center_x + 91.0 - s as f32 * 8.0 - 9.0;
            let y = gui_h - 32.0;
            let (u, v) = if s * 2 + 1 < state.armor_points as usize {
                (34.0, 9.0)
            } else if s * 2 + 1 == state.armor_points as usize {
                (25.0, 9.0)
            } else {
                (16.0, 9.0)
            };
            push_textured_quad_gui(
                &mut verts,
                x,
                y,
                9.0,
                9.0,
                u,
                v,
                9.0,
                9.0,
                HUD_TEX_ICONS,
                scale,
                WHITE,
            );
        }
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

    if state.submerged_in_water {
        let full = (((state.breath - 2).max(0) as f32) * 10.0 / state.breath_capacity as f32).ceil()
            as i32;
        let partial = ((state.breath.max(0) as f32) * 10.0 / state.breath_capacity as f32).ceil()
            as i32
            - full;
        let bubble_count = (full + partial).max(0) as usize;
        for i in 0..bubble_count {
            let x = center_x - 91.0 + i as f32 * 8.0;
            let y = gui_h - 32.0 - 9.0;
            let tex_x = if i < full as usize { 16.0 } else { 25.0 };
            push_textured_quad_gui(
                &mut verts,
                x,
                y,
                9.0,
                9.0,
                tex_x,
                18.0,
                9.0,
                9.0,
                HUD_TEX_ICONS,
                scale,
                WHITE,
            );
        }
    }

    if state.is_dead {
        let fade = ((state.death_ticks as f32) / 20.0).clamp(0.0, 1.0) * 0.6;
        push_colored_quad_gui(
            &mut verts,
            0.0,
            0.0,
            gui_w,
            gui_h,
            scale,
            [0.3, 0.0, 0.0, fade],
        );
    }

    verts
}

#[must_use]
pub fn build_inventory_vertices(
    screen_w: f32,
    screen_h: f32,
    inventory: &PlayerInventoryState,
    mouse_screen_pos: [f32; 2],
    registry: &BlockRegistry,
) -> Vec<HudVertex> {
    let mut verts = Vec::with_capacity(1024);
    let layout = inventory_layout(screen_w, screen_h);

    // Dim world behind inventory.
    push_colored_quad_gui(
        &mut verts,
        0.0,
        0.0,
        (screen_w / layout.scale).floor(),
        (screen_h / layout.scale).floor(),
        layout.scale,
        [0.06, 0.06, 0.06, 0.60],
    );

    // Inventory panel background (inventory.png).
    push_textured_quad_gui(
        &mut verts,
        layout.left,
        layout.top,
        176.0,
        166.0,
        0.0,
        0.0,
        176.0,
        166.0,
        HUD_TEX_INVENTORY,
        layout.scale,
        WHITE,
    );

    let hovered = crate::inventory::hit_test_slot(
        mouse_screen_pos[0],
        mouse_screen_pos[1],
        screen_w,
        screen_h,
    );

    for i in 0..ARMOR_SLOT_COUNT {
        let slot = PlayerSlot::Armor(i as u8);
        render_inventory_slot_item(
            &mut verts,
            layout.left,
            layout.top,
            layout.scale,
            inventory,
            slot,
            registry,
        );
    }
    for i in 0..MAIN_SLOT_COUNT {
        let slot = PlayerSlot::Main(i as u8);
        render_inventory_slot_item(
            &mut verts,
            layout.left,
            layout.top,
            layout.scale,
            inventory,
            slot,
            registry,
        );
    }
    for i in 0..HOTBAR_SLOT_COUNT {
        let slot = PlayerSlot::Hotbar(i as u8);
        render_inventory_slot_item(
            &mut verts,
            layout.left,
            layout.top,
            layout.scale,
            inventory,
            slot,
            registry,
        );
    }

    if let Some(hovered_slot) = hovered {
        let (sx, sy) = slot_gui_xy(hovered_slot);
        push_colored_quad_gui(
            &mut verts,
            layout.left + sx - 1.0,
            layout.top + sy - 1.0,
            18.0,
            18.0,
            layout.scale,
            [1.0, 1.0, 1.0, 0.28],
        );
    }

    if let Some(cursor_stack) = inventory.cursor {
        let item_x = mouse_screen_pos[0] / layout.scale - 8.0;
        let item_y = mouse_screen_pos[1] / layout.scale - 8.0;
        let ItemKey::Block(block_id) = cursor_stack.item;
        push_hotbar_item_vertices(&mut verts, item_x, item_y, block_id, registry, layout.scale);
    }

    verts
}

fn render_inventory_slot_item(
    verts: &mut Vec<HudVertex>,
    panel_left: f32,
    panel_top: f32,
    scale: f32,
    inventory: &PlayerInventoryState,
    slot: PlayerSlot,
    registry: &BlockRegistry,
) {
    let stack = match slot {
        PlayerSlot::Hotbar(i) => inventory.hotbar_stack(usize::from(i)),
        PlayerSlot::Main(i) => inventory
            .main_stacks()
            .get(usize::from(i))
            .copied()
            .flatten(),
        PlayerSlot::Armor(i) => inventory
            .armor_stacks()
            .get(usize::from(i))
            .copied()
            .flatten(),
    };
    let Some(stack) = stack else {
        return;
    };
    let (sx, sy) = slot_gui_xy(slot);
    let ItemKey::Block(block_id) = stack.item;
    push_hotbar_item_vertices(
        verts,
        panel_left + sx,
        panel_top + sy,
        block_id,
        registry,
        scale,
    );
}

fn push_hotbar_item_vertices(
    verts: &mut Vec<HudVertex>,
    x: f32,
    y: f32,
    block_id: u8,
    registry: &BlockRegistry,
    scale: f32,
) {
    if registry.is_solid(block_id)
        && !registry.is_liquid(block_id)
        && !registry.is_billboard_plant(block_id)
    {
        push_alpha_item3d_block(verts, x, y, block_id, registry, scale);
    } else {
        let sprite = registry.sprite_index_of(block_id);
        let u = (sprite % 16) as f32 * 16.0;
        let v = (sprite / 16) as f32 * 16.0;
        push_textured_quad_gui(
            verts,
            x,
            y,
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
}

fn push_alpha_item3d_block(
    verts: &mut Vec<HudVertex>,
    x: f32,
    y: f32,
    block_id: u8,
    registry: &BlockRegistry,
    scale: f32,
) {
    let vertices = [
        [-0.5, -0.5, -0.5],
        [0.5, -0.5, -0.5],
        [0.5, 0.5, -0.5],
        [-0.5, 0.5, -0.5],
        [-0.5, -0.5, 0.5],
        [0.5, -0.5, 0.5],
        [0.5, 0.5, 0.5],
        [-0.5, 0.5, 0.5],
    ];

    // Mirrors ItemRenderer.renderGuiItem + BlockRenderer.renderAsItem transforms in Alpha.
    let transformed = vertices.map(|v| alpha_item3d_transform(v, x, y));
    let face_defs = [
        // block.getSprite(face): 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east
        (
            [0_usize, 1, 5, 4],
            [0.0, -1.0, 0.0],
            registry.sprite_index_for_face(block_id, [0, -1, 0]),
        ),
        (
            [3, 2, 6, 7],
            [0.0, 1.0, 0.0],
            registry.sprite_index_for_face(block_id, [0, 1, 0]),
        ),
        (
            [1, 0, 3, 2],
            [0.0, 0.0, -1.0],
            registry.sprite_index_for_face(block_id, [0, 0, -1]),
        ),
        (
            [4, 5, 6, 7],
            [0.0, 0.0, 1.0],
            registry.sprite_index_for_face(block_id, [0, 0, 1]),
        ),
        (
            [0, 4, 7, 3],
            [-1.0, 0.0, 0.0],
            registry.sprite_index_for_face(block_id, [-1, 0, 0]),
        ),
        (
            [5, 1, 2, 6],
            [1.0, 0.0, 0.0],
            registry.sprite_index_for_face(block_id, [1, 0, 0]),
        ),
    ];

    let mut faces: Vec<(f32, [[f32; 2]; 4], [[f32; 2]; 4], [f32; 4])> = Vec::with_capacity(3);
    for (idx, normal, sprite) in face_defs {
        let n = alpha_item3d_rotate_normal(normal);
        // Alpha GUI item path renders with backface culling; this keeps only camera-facing faces.
        if n[2] >= 0.0 {
            continue;
        }
        let brightness = alpha_item3d_face_brightness(n);
        let points = [
            [transformed[idx[0]][0], transformed[idx[0]][1]],
            [transformed[idx[1]][0], transformed[idx[1]][1]],
            [transformed[idx[2]][0], transformed[idx[2]][1]],
            [transformed[idx[3]][0], transformed[idx[3]][1]],
        ];
        let avg_z = (transformed[idx[0]][2]
            + transformed[idx[1]][2]
            + transformed[idx[2]][2]
            + transformed[idx[3]][2])
            * 0.25;
        faces.push((
            avg_z,
            points,
            sprite_uv(sprite),
            [brightness, brightness, brightness, 1.0],
        ));
    }

    // Painter order for 2D HUD path (far to near).
    faces.sort_by(|a, b| a.0.partial_cmp(&b.0).unwrap_or(std::cmp::Ordering::Equal));
    for (_, points, uv, color) in faces {
        push_textured_quad_points_gui(verts, points, uv, HUD_TEX_TERRAIN, scale, color);
    }
}

fn alpha_item3d_transform(v: [f32; 3], x: f32, y: f32) -> [f32; 3] {
    let mut p = v;
    // ItemRenderer.renderGuiItem:
    // glTranslatef(x - 2, y + 3, 0)
    // glScalef(10, 10, 10)
    // glTranslatef(1, 0.5, 8)
    // glRotatef(210, 1,0,0)
    // glRotatef(45, 0,1,0)
    p = rotate_y(p, 45.0_f32.to_radians());
    p = rotate_x(p, 210.0_f32.to_radians());
    p[0] += 1.0;
    p[1] += 0.5;
    p[2] += 8.0;
    p[0] *= 10.0;
    p[1] *= 10.0;
    p[2] *= 10.0;
    p[0] += x - 2.0;
    p[1] += y + 3.0;
    p
}

fn alpha_item3d_rotate_normal(n: [f32; 3]) -> [f32; 3] {
    let n = rotate_y(n, 45.0_f32.to_radians());
    rotate_x(n, 210.0_f32.to_radians())
}

fn alpha_item3d_face_brightness(n: [f32; 3]) -> f32 {
    // GameGui applies glRotatef(180, 1, 0, 0) before Lighting.turnOn(), so GUI
    // light vectors from Lighting.turnOn() are effectively rotated in X.
    let light0 = normalize3([0.2, -1.0, 0.7]);
    let light1 = normalize3([-0.2, -1.0, -0.7]);
    let ambient = 0.4_f32;
    let diffuse = 0.6_f32;
    let b = ambient + diffuse * dot3(n, light0).max(0.0) + diffuse * dot3(n, light1).max(0.0);
    b.clamp(0.0, 1.0)
}

fn rotate_x(v: [f32; 3], radians: f32) -> [f32; 3] {
    let (s, c) = radians.sin_cos();
    [v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c]
}

fn rotate_y(v: [f32; 3], radians: f32) -> [f32; 3] {
    let (s, c) = radians.sin_cos();
    [v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c]
}

fn dot3(a: [f32; 3], b: [f32; 3]) -> f32 {
    a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
}

fn normalize3(v: [f32; 3]) -> [f32; 3] {
    let len = (v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).sqrt().max(1e-6);
    [v[0] / len, v[1] / len, v[2] / len]
}

fn sprite_uv(sprite: u16) -> [[f32; 2]; 4] {
    let u = (sprite % 16) as f32 * 16.0 / TEX_SIZE_PX;
    let v = (sprite / 16) as f32 * 16.0 / TEX_SIZE_PX;
    let u1 = u + 16.0 / TEX_SIZE_PX;
    let v1 = v + 16.0 / TEX_SIZE_PX;
    [[u, v], [u1, v], [u1, v1], [u, v1]]
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

fn push_colored_quad_gui(
    verts: &mut Vec<HudVertex>,
    gui_x: f32,
    gui_y: f32,
    gui_w: f32,
    gui_h: f32,
    scale: f32,
    color: [f32; 4],
) {
    push_textured_quad_gui(
        verts, gui_x, gui_y, gui_w, gui_h, 0.0, 0.0, 1.0, 1.0, -1.0, scale, color,
    );
}

fn push_textured_quad_points_gui(
    verts: &mut Vec<HudVertex>,
    points: [[f32; 2]; 4],
    uv: [[f32; 2]; 4],
    texture_kind: f32,
    scale: f32,
    color: [f32; 4],
) {
    let p = points.map(|p| [(p[0] * scale).floor(), (p[1] * scale).floor()]);
    let v0 = HudVertex {
        position: p[0],
        uv: uv[0],
        color,
        texture_kind,
    };
    let v1 = HudVertex {
        position: p[1],
        uv: uv[1],
        color,
        texture_kind,
    };
    let v2 = HudVertex {
        position: p[2],
        uv: uv[2],
        color,
        texture_kind,
    };
    let v3 = HudVertex {
        position: p[3],
        uv: uv[3],
        color,
        texture_kind,
    };
    verts.push(v0);
    verts.push(v1);
    verts.push(v2);
    verts.push(v2);
    verts.push(v3);
    verts.push(v0);
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
            breath: 300,
            breath_capacity: 300,
            submerged_in_water: false,
            armor_points: 0,
            is_dead: false,
            death_ticks: 0,
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
