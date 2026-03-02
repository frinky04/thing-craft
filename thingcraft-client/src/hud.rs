/// HUD overlay: crosshair at screen center + hotbar bar at bottom.
/// All coordinates are in screen pixels. The HUD pipeline converts to NDC.

#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
pub struct HudVertex {
    pub position: [f32; 2],
    pub color: [f32; 4],
}

impl HudVertex {
    pub const ATTRS: [wgpu::VertexAttribute; 2] =
        wgpu::vertex_attr_array![0 => Float32x2, 1 => Float32x4];

    #[must_use]
    pub fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

/// Screen-space uniform: [width, height, 0, 0] (padded to 16 bytes for alignment).
#[derive(Debug, Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
#[repr(C)]
pub struct HudUniform {
    pub screen_width: f32,
    pub screen_height: f32,
    pub _pad: [f32; 2],
}

const CROSSHAIR_ARM_LENGTH: f32 = 10.0;
const CROSSHAIR_THICKNESS: f32 = 2.0;
const CROSSHAIR_COLOR: [f32; 4] = [1.0, 1.0, 1.0, 0.8];

const HOTBAR_SLOT_SIZE: f32 = 40.0;
const HOTBAR_SLOT_GAP: f32 = 4.0;
const HOTBAR_SLOT_COUNT: usize = 9;
const HOTBAR_BOTTOM_MARGIN: f32 = 10.0;
const HOTBAR_SLOT_COLOR: [f32; 4] = [0.15, 0.15, 0.15, 0.7];
const HOTBAR_SELECTED_COLOR: [f32; 4] = [0.4, 0.4, 0.4, 0.85];
const HOTBAR_BORDER_COLOR: [f32; 4] = [0.5, 0.5, 0.5, 0.6];
const HOTBAR_BORDER_THICKNESS: f32 = 2.0;

/// Build vertices for the crosshair (white "+" at center).
#[must_use]
pub fn build_crosshair_vertices(screen_w: f32, screen_h: f32) -> Vec<HudVertex> {
    let cx = (screen_w / 2.0).floor();
    let cy = (screen_h / 2.0).floor();
    let half_thick = CROSSHAIR_THICKNESS / 2.0;
    let arm = CROSSHAIR_ARM_LENGTH;

    let mut verts = Vec::with_capacity(12);

    // Horizontal bar.
    push_quad(
        &mut verts,
        cx - arm,
        cy - half_thick,
        cx + arm,
        cy + half_thick,
        CROSSHAIR_COLOR,
    );

    // Vertical bar.
    push_quad(
        &mut verts,
        cx - half_thick,
        cy - arm,
        cx + half_thick,
        cy + arm,
        CROSSHAIR_COLOR,
    );

    verts
}

/// Build vertices for the hotbar (9 dark squares at bottom center, selected slot brighter).
#[must_use]
pub fn build_hotbar_vertices(
    screen_w: f32,
    screen_h: f32,
    selected: usize,
    slot_counts: &[u8; HOTBAR_SLOT_COUNT],
) -> Vec<HudVertex> {
    let _ = slot_counts; // Reserved for future stack count display.
    let total_width = HOTBAR_SLOT_COUNT as f32 * HOTBAR_SLOT_SIZE
        + (HOTBAR_SLOT_COUNT - 1) as f32 * HOTBAR_SLOT_GAP;
    let start_x = ((screen_w - total_width) / 2.0).floor();
    let bottom_y = screen_h - HOTBAR_BOTTOM_MARGIN - HOTBAR_SLOT_SIZE;

    let mut verts = Vec::with_capacity(HOTBAR_SLOT_COUNT * 18); // 6 verts per fill + up to 12 for border

    for i in 0..HOTBAR_SLOT_COUNT {
        let x = start_x + i as f32 * (HOTBAR_SLOT_SIZE + HOTBAR_SLOT_GAP);
        let y = bottom_y;
        let color = if i == selected {
            HOTBAR_SELECTED_COLOR
        } else {
            HOTBAR_SLOT_COLOR
        };

        // Slot background.
        push_quad(
            &mut verts,
            x,
            y,
            x + HOTBAR_SLOT_SIZE,
            y + HOTBAR_SLOT_SIZE,
            color,
        );

        // Border around slot.
        let b = HOTBAR_BORDER_THICKNESS;
        let border_color = if i == selected {
            [0.9, 0.9, 0.9, 0.9]
        } else {
            HOTBAR_BORDER_COLOR
        };
        // Top edge.
        push_quad(&mut verts, x, y, x + HOTBAR_SLOT_SIZE, y + b, border_color);
        // Bottom edge.
        push_quad(
            &mut verts,
            x,
            y + HOTBAR_SLOT_SIZE - b,
            x + HOTBAR_SLOT_SIZE,
            y + HOTBAR_SLOT_SIZE,
            border_color,
        );
        // Left edge.
        push_quad(&mut verts, x, y, x + b, y + HOTBAR_SLOT_SIZE, border_color);
        // Right edge.
        push_quad(
            &mut verts,
            x + HOTBAR_SLOT_SIZE - b,
            y,
            x + HOTBAR_SLOT_SIZE,
            y + HOTBAR_SLOT_SIZE,
            border_color,
        );
    }

    verts
}

fn push_quad(verts: &mut Vec<HudVertex>, x0: f32, y0: f32, x1: f32, y1: f32, color: [f32; 4]) {
    // Two triangles: (0,1,2) and (2,3,0).
    let v0 = HudVertex {
        position: [x0, y0],
        color,
    };
    let v1 = HudVertex {
        position: [x1, y0],
        color,
    };
    let v2 = HudVertex {
        position: [x1, y1],
        color,
    };
    let v3 = HudVertex {
        position: [x0, y1],
        color,
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
    fn crosshair_vertices_centered_on_screen() {
        let verts = build_crosshair_vertices(1280.0, 720.0);
        // 2 quads * 6 vertices each = 12 vertices.
        assert_eq!(verts.len(), 12);

        // Check that the average position is at screen center.
        let avg_x: f32 = verts.iter().map(|v| v.position[0]).sum::<f32>() / verts.len() as f32;
        let avg_y: f32 = verts.iter().map(|v| v.position[1]).sum::<f32>() / verts.len() as f32;
        assert!((avg_x - 640.0).abs() < 1.0);
        assert!((avg_y - 360.0).abs() < 1.0);
    }

    #[test]
    fn hotbar_has_expected_vertex_count() {
        let counts = [64; 9];
        let verts = build_hotbar_vertices(1280.0, 720.0, 0, &counts);
        // Each slot: 1 background quad + 4 border quads = 5 quads * 6 verts = 30 verts.
        // 9 slots * 30 = 270 verts.
        assert_eq!(verts.len(), 9 * 5 * 6);
    }

    #[test]
    fn hotbar_centered_horizontally() {
        let counts = [64; 9];
        let verts = build_hotbar_vertices(1280.0, 720.0, 4, &counts);
        // Find min and max X across all vertices.
        let min_x = verts.iter().map(|v| v.position[0]).fold(f32::MAX, f32::min);
        let max_x = verts.iter().map(|v| v.position[0]).fold(f32::MIN, f32::max);
        let center_x = (min_x + max_x) / 2.0;
        assert!(
            (center_x - 640.0).abs() < 1.0,
            "expected center near 640, got {center_x}"
        );
    }

    #[test]
    fn selected_slot_uses_brighter_color() {
        let counts = [64; 9];
        let verts = build_hotbar_vertices(100.0, 100.0, 2, &counts);
        // The first 6 vertices of slot 0 (not selected) should use HOTBAR_SLOT_COLOR.
        // The first 6 vertices of slot 2 (selected) should use HOTBAR_SELECTED_COLOR.
        // Each slot contributes 30 vertices (5 quads). Slot 2 starts at index 60.
        let slot0_bg = &verts[0];
        let slot2_bg = &verts[60];
        assert!(
            slot2_bg.color[0] > slot0_bg.color[0],
            "selected slot should be brighter"
        );
    }
}
