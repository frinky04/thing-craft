use winit::event::MouseButton;
use bevy_ecs::prelude::Resource;

pub const HOTBAR_SLOT_COUNT: usize = 9;
pub const MAIN_SLOT_COUNT: usize = 27;
pub const ARMOR_SLOT_COUNT: usize = 4;
pub const INVENTORY_WIDTH: f32 = 176.0;
pub const INVENTORY_HEIGHT: f32 = 166.0;
pub const AIR_BLOCK_ID: u8 = 0;
pub const HOTBAR_DEFAULT_BLOCK_IDS: [u8; HOTBAR_SLOT_COUNT] =
    [3, 1, 4, 12, AIR_BLOCK_ID, 17, 5, 9, 50];

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum ItemKey {
    Block(u8),
    Tool(u16),
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub struct ItemStack {
    pub item: ItemKey,
    pub count: u8,
    pub metadata: u16,
}

impl ItemStack {
    #[must_use]
    pub fn block(block_id: u8, count: u8) -> Self {
        Self {
            item: ItemKey::Block(block_id),
            count,
            metadata: 0,
        }
    }

    #[must_use]
    pub fn tool(tool_id: u16) -> Self {
        Self {
            item: ItemKey::Tool(tool_id),
            count: 1,
            metadata: 0,
        }
    }

    #[must_use]
    pub fn max_stack_size(self) -> u8 {
        match self.item {
            ItemKey::Block(_) => 64,
            ItemKey::Tool(_) => 1,
        }
    }
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum PlayerSlot {
    Hotbar(u8),
    Main(u8),
    Armor(u8),
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum InventoryCommand {
    ClickSlot {
        slot: PlayerSlot,
        button: MouseButton,
    },
    ClickOutside {
        button: MouseButton,
    },
    SelectHotbar {
        index: u8,
    },
    ScrollHotbar {
        delta: i8,
    },
    ConsumeSelected {
        amount: u8,
    },
    CloseInventory,
}

#[derive(Debug, Clone, Default)]
pub struct InventoryApplyResult {
    pub changed: bool,
    pub dropped_to_world: Vec<ItemStack>,
}

#[derive(Resource, Debug, Clone, Eq, PartialEq)]
pub struct PlayerInventoryState {
    hotbar: [Option<ItemStack>; HOTBAR_SLOT_COUNT],
    main: [Option<ItemStack>; MAIN_SLOT_COUNT],
    armor: [Option<ItemStack>; ARMOR_SLOT_COUNT],
    pub selected_hotbar: u8,
    pub cursor: Option<ItemStack>,
}

impl PlayerInventoryState {
    #[must_use]
    pub fn alpha_defaults() -> Self {
        let mut hotbar: [Option<ItemStack>; HOTBAR_SLOT_COUNT] = [None; HOTBAR_SLOT_COUNT];
        for (i, &block_id) in HOTBAR_DEFAULT_BLOCK_IDS.iter().enumerate() {
            if block_id != AIR_BLOCK_ID {
                hotbar[i] = Some(ItemStack::block(block_id, 64));
            }
        }
        // Slot 4 (was AIR) gets a wooden pickaxe for testing.
        hotbar[4] = Some(ItemStack::tool(270)); // Wooden Pickaxe

        let mut main = [None; MAIN_SLOT_COUNT];
        // Place a few representative tools in the main inventory for testing.
        main[0] = Some(ItemStack::tool(275)); // Stone Axe
        main[1] = Some(ItemStack::tool(256)); // Iron Shovel
        main[2] = Some(ItemStack::tool(276)); // Diamond Sword
        main[3] = Some(ItemStack::tool(274)); // Stone Pickaxe
        main[4] = Some(ItemStack::tool(278)); // Diamond Pickaxe

        Self {
            hotbar,
            main,
            armor: [None; ARMOR_SLOT_COUNT],
            selected_hotbar: 0,
            cursor: None,
        }
    }

    #[must_use]
    pub fn hotbar_stack(&self, index: usize) -> Option<ItemStack> {
        self.hotbar.get(index).copied().flatten()
    }

    #[must_use]
    pub fn main_stacks(&self) -> &[Option<ItemStack>; MAIN_SLOT_COUNT] {
        &self.main
    }

    #[must_use]
    pub fn armor_stacks(&self) -> &[Option<ItemStack>; ARMOR_SLOT_COUNT] {
        &self.armor
    }

    #[must_use]
    pub fn selected_stack(&self) -> Option<ItemStack> {
        self.hotbar_stack(usize::from(
            self.selected_hotbar.min((HOTBAR_SLOT_COUNT - 1) as u8),
        ))
    }

    #[must_use]
    pub fn selected_block_id(&self) -> Option<u8> {
        match self.selected_stack()? {
            ItemStack {
                item: ItemKey::Block(block_id),
                count,
                ..
            } if count > 0 => Some(block_id),
            _ => None,
        }
    }

    #[must_use]
    pub fn selected_item_key(&self) -> Option<ItemKey> {
        self.selected_stack().map(|s| s.item)
    }

    /// Apply durability damage to the selected hotbar tool. Returns true if state changed.
    /// Looks up `max_damage` from the `ToolRegistry` to determine breakage.
    pub fn damage_selected_tool(&mut self, amount: u16, tool_registry: &crate::tool::ToolRegistry) -> bool {
        if amount == 0 {
            return false;
        }
        let slot_idx = usize::from(
            self.selected_hotbar.min((HOTBAR_SLOT_COUNT - 1) as u8),
        );
        if let Some(stack) = &mut self.hotbar[slot_idx] {
            if let ItemKey::Tool(tool_id) = stack.item {
                stack.metadata = stack.metadata.saturating_add(amount);
                let max_damage = tool_registry
                    .get(tool_id)
                    .map_or(0, |def| def.max_damage);
                if stack.metadata > max_damage {
                    self.hotbar[slot_idx] = None;
                }
                return true;
            }
        }
        false
    }

    pub fn try_add_stack(&mut self, mut incoming: ItemStack) -> bool {
        if incoming.count == 0 {
            return true;
        }
        for slot in self.hotbar.iter_mut().chain(self.main.iter_mut()) {
            let Some(existing) = slot.as_mut() else {
                continue;
            };
            if existing.item != incoming.item || existing.metadata != incoming.metadata {
                continue;
            }
            let max = existing.max_stack_size();
            if existing.count >= max {
                continue;
            }
            let move_count = incoming.count.min(max - existing.count);
            existing.count += move_count;
            incoming.count -= move_count;
            if incoming.count == 0 {
                return true;
            }
        }

        for slot in self.hotbar.iter_mut().chain(self.main.iter_mut()) {
            if slot.is_none() {
                *slot = Some(incoming);
                return true;
            }
        }
        false
    }

    #[must_use]
    pub fn apply(&mut self, cmd: InventoryCommand) -> InventoryApplyResult {
        let mut result = InventoryApplyResult::default();
        match cmd {
            InventoryCommand::SelectHotbar { index } => {
                let clamped = index.min((HOTBAR_SLOT_COUNT - 1) as u8);
                if self.selected_hotbar != clamped {
                    self.selected_hotbar = clamped;
                    result.changed = true;
                }
            }
            InventoryCommand::ScrollHotbar { delta } => {
                if delta != 0 {
                    let mut selected = i32::from(self.selected_hotbar) - i32::from(delta.signum());
                    while selected < 0 {
                        selected += HOTBAR_SLOT_COUNT as i32;
                    }
                    while selected >= HOTBAR_SLOT_COUNT as i32 {
                        selected -= HOTBAR_SLOT_COUNT as i32;
                    }
                    let selected = selected as u8;
                    if self.selected_hotbar != selected {
                        self.selected_hotbar = selected;
                        result.changed = true;
                    }
                }
            }
            InventoryCommand::ConsumeSelected { amount } => {
                if amount > 0 {
                    let slot = PlayerSlot::Hotbar(self.selected_hotbar);
                    if consume_slot_amount(self, slot, amount) {
                        result.changed = true;
                    }
                }
            }
            InventoryCommand::ClickOutside { button } => {
                if let Some(mut cursor) = self.cursor {
                    match button {
                        MouseButton::Left => {
                            self.cursor = None;
                            result.dropped_to_world.push(cursor);
                            result.changed = true;
                        }
                        MouseButton::Right => {
                            let one = ItemStack { count: 1, ..cursor };
                            result.dropped_to_world.push(one);
                            cursor.count = cursor.count.saturating_sub(1);
                            self.cursor = if cursor.count == 0 {
                                None
                            } else {
                                Some(cursor)
                            };
                            result.changed = true;
                        }
                        _ => {}
                    }
                }
            }
            InventoryCommand::CloseInventory => {
                if let Some(cursor) = self.cursor.take() {
                    result.dropped_to_world.push(cursor);
                    result.changed = true;
                }
            }
            InventoryCommand::ClickSlot { slot, button } => match button {
                MouseButton::Left | MouseButton::Right => {
                    if apply_slot_click(self, slot, button) {
                        result.changed = true;
                    }
                }
                _ => {}
            },
        }
        result
    }

    #[must_use]
    pub fn snapshot(&self) -> InventorySnapshot {
        InventorySnapshot {
            hotbar: self.hotbar,
            main: self.main,
            armor: self.armor,
            selected_hotbar: self.selected_hotbar,
            cursor: self.cursor,
        }
    }
}

fn apply_slot_click(inv: &mut PlayerInventoryState, slot: PlayerSlot, button: MouseButton) -> bool {
    let slot_max = slot_max_stack_size(slot);
    let slot_allows = |stack: ItemStack| slot_accepts_item(slot, stack);
    let current_slot_item = get_slot(inv, slot);
    if current_slot_item.is_none() && inv.cursor.is_none() {
        return false;
    }

    // Take from slot to cursor.
    if current_slot_item.is_some() && inv.cursor.is_none() {
        let mut src = current_slot_item.expect("checked above");
        let take = match button {
            MouseButton::Left => src.count,
            MouseButton::Right => src.count.div_ceil(2),
            _ => return false,
        };
        src.count -= take;
        let picked = ItemStack { count: take, ..src };
        set_slot(inv, slot, if src.count == 0 { None } else { Some(src) });
        inv.cursor = Some(picked);
        return true;
    }

    // Place cursor into empty slot.
    if current_slot_item.is_none() && inv.cursor.is_some() {
        let mut cursor = inv.cursor.expect("checked above");
        if !slot_allows(cursor) {
            return false;
        }
        let place = match button {
            MouseButton::Left => cursor.count.min(slot_max),
            MouseButton::Right => 1.min(slot_max),
            _ => return false,
        };
        if place == 0 {
            return false;
        }
        cursor.count -= place;
        set_slot(
            inv,
            slot,
            Some(ItemStack {
                count: place,
                ..cursor
            }),
        );
        inv.cursor = if cursor.count == 0 {
            None
        } else {
            Some(cursor)
        };
        return true;
    }

    // Both cursor and slot are occupied.
    let mut slot_item = current_slot_item.expect("checked above");
    let mut cursor = inv.cursor.expect("checked above");
    if !slot_allows(cursor) {
        // Alpha-compatible: if same stack and can't place, allow pulling into cursor if cursor has room.
        if slot_item.item == cursor.item
            && slot_item.metadata == cursor.metadata
            && cursor.max_stack_size() > 1
            && slot_item.count > 0
            && (u16::from(slot_item.count) + u16::from(cursor.count))
                <= u16::from(cursor.max_stack_size())
        {
            cursor.count = cursor.count.saturating_add(slot_item.count);
            set_slot(inv, slot, None);
            inv.cursor = Some(cursor);
            return true;
        }
        return false;
    }

    // Different items => swap on left click.
    if slot_item.item != cursor.item || slot_item.metadata != cursor.metadata {
        if button != MouseButton::Left || cursor.count > slot_max {
            return false;
        }
        set_slot(inv, slot, Some(cursor));
        inv.cursor = Some(slot_item);
        return true;
    }

    // Same item => merge.
    let max = slot_max.min(slot_item.max_stack_size());
    if slot_item.count >= max {
        return false;
    }
    let add = match button {
        MouseButton::Left => cursor.count.min(max - slot_item.count),
        MouseButton::Right => 1.min(max - slot_item.count).min(cursor.count),
        _ => return false,
    };
    if add == 0 {
        return false;
    }
    slot_item.count += add;
    cursor.count -= add;
    set_slot(inv, slot, Some(slot_item));
    inv.cursor = if cursor.count == 0 {
        None
    } else {
        Some(cursor)
    };
    true
}

fn consume_slot_amount(inv: &mut PlayerInventoryState, slot: PlayerSlot, amount: u8) -> bool {
    let Some(mut stack) = get_slot(inv, slot) else {
        return false;
    };
    if stack.count < amount {
        return false;
    }
    stack.count -= amount;
    set_slot(inv, slot, if stack.count == 0 { None } else { Some(stack) });
    true
}

fn get_slot(inv: &PlayerInventoryState, slot: PlayerSlot) -> Option<ItemStack> {
    match slot {
        PlayerSlot::Hotbar(i) => inv.hotbar.get(usize::from(i)).copied().flatten(),
        PlayerSlot::Main(i) => inv.main.get(usize::from(i)).copied().flatten(),
        PlayerSlot::Armor(i) => inv.armor.get(usize::from(i)).copied().flatten(),
    }
}

fn set_slot(inv: &mut PlayerInventoryState, slot: PlayerSlot, stack: Option<ItemStack>) {
    match slot {
        PlayerSlot::Hotbar(i) => {
            if let Some(dst) = inv.hotbar.get_mut(usize::from(i)) {
                *dst = stack;
            }
        }
        PlayerSlot::Main(i) => {
            if let Some(dst) = inv.main.get_mut(usize::from(i)) {
                *dst = stack;
            }
        }
        PlayerSlot::Armor(i) => {
            if let Some(dst) = inv.armor.get_mut(usize::from(i)) {
                *dst = stack;
            }
        }
    }
}

fn slot_max_stack_size(slot: PlayerSlot) -> u8 {
    match slot {
        PlayerSlot::Armor(_) => 1,
        PlayerSlot::Hotbar(_) | PlayerSlot::Main(_) => 64,
    }
}

fn slot_accepts_item(slot: PlayerSlot, stack: ItemStack) -> bool {
    match slot {
        PlayerSlot::Armor(_) => stack.count >= 1,
        PlayerSlot::Hotbar(_) | PlayerSlot::Main(_) => stack.count >= 1,
    }
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub struct InventorySnapshot {
    hotbar: [Option<ItemStack>; HOTBAR_SLOT_COUNT],
    main: [Option<ItemStack>; MAIN_SLOT_COUNT],
    armor: [Option<ItemStack>; ARMOR_SLOT_COUNT],
    selected_hotbar: u8,
    cursor: Option<ItemStack>,
}

#[derive(Debug, Clone, Copy)]
pub struct InventoryScreenLayout {
    pub scale: f32,
    pub left: f32,
    pub top: f32,
}

#[must_use]
pub fn gui_scale(screen_w: f32, screen_h: f32) -> f32 {
    let mut scale = 1.0_f32;
    while (screen_w / (scale + 1.0)).floor() >= 320.0 && (screen_h / (scale + 1.0)).floor() >= 240.0
    {
        scale += 1.0;
    }
    scale
}

#[must_use]
pub fn inventory_layout(screen_w: f32, screen_h: f32) -> InventoryScreenLayout {
    let scale = gui_scale(screen_w, screen_h);
    let gui_w = (screen_w / scale).floor();
    let gui_h = (screen_h / scale).floor();
    InventoryScreenLayout {
        scale,
        left: ((gui_w - INVENTORY_WIDTH) * 0.5).floor(),
        top: ((gui_h - INVENTORY_HEIGHT) * 0.5).floor(),
    }
}

#[must_use]
pub fn slot_gui_xy(slot: PlayerSlot) -> (f32, f32) {
    match slot {
        PlayerSlot::Armor(i) => (8.0, 8.0 + f32::from(i) * 18.0),
        PlayerSlot::Main(i) => {
            let idx = usize::from(i).min(MAIN_SLOT_COUNT - 1);
            let row = idx / 9;
            let col = idx % 9;
            (8.0 + col as f32 * 18.0, 84.0 + row as f32 * 18.0)
        }
        PlayerSlot::Hotbar(i) => (8.0 + f32::from(i) * 18.0, 142.0),
    }
}

#[must_use]
pub fn hit_test_slot(
    mouse_x: f32,
    mouse_y: f32,
    screen_w: f32,
    screen_h: f32,
) -> Option<PlayerSlot> {
    let layout = inventory_layout(screen_w, screen_h);
    let mx = mouse_x / layout.scale - layout.left;
    let my = mouse_y / layout.scale - layout.top;
    for i in 0..ARMOR_SLOT_COUNT {
        let slot = PlayerSlot::Armor(i as u8);
        if slot_contains(slot, mx, my) {
            return Some(slot);
        }
    }
    for i in 0..MAIN_SLOT_COUNT {
        let slot = PlayerSlot::Main(i as u8);
        if slot_contains(slot, mx, my) {
            return Some(slot);
        }
    }
    for i in 0..HOTBAR_SLOT_COUNT {
        let slot = PlayerSlot::Hotbar(i as u8);
        if slot_contains(slot, mx, my) {
            return Some(slot);
        }
    }
    None
}

fn slot_contains(slot: PlayerSlot, mx: f32, my: f32) -> bool {
    let (sx, sy) = slot_gui_xy(slot);
    mx >= sx - 1.0 && mx < sx + 17.0 && my >= sy - 1.0 && my < sy + 17.0
}

#[cfg(test)]
mod tests {
    use super::{InventoryCommand, ItemStack, MouseButton, PlayerInventoryState, PlayerSlot};

    #[test]
    fn left_click_pick_and_place_roundtrips_stack() {
        let mut inv = PlayerInventoryState::alpha_defaults();
        let slot = PlayerSlot::Hotbar(0);
        let initial = inv.hotbar_stack(0).expect("stack");
        let _ = inv.apply(InventoryCommand::ClickSlot {
            slot,
            button: MouseButton::Left,
        });
        assert_eq!(inv.hotbar_stack(0), None);
        assert_eq!(inv.cursor, Some(initial));

        let _ = inv.apply(InventoryCommand::ClickSlot {
            slot,
            button: MouseButton::Left,
        });
        assert_eq!(inv.hotbar_stack(0), Some(initial));
        assert_eq!(inv.cursor, None);
    }

    #[test]
    fn right_click_splits_stack_to_cursor() {
        let mut inv = PlayerInventoryState::alpha_defaults();
        let slot = PlayerSlot::Hotbar(0);
        let _ = inv.apply(InventoryCommand::ClickSlot {
            slot,
            button: MouseButton::Right,
        });
        let slot_count = inv.hotbar_stack(0).expect("stack").count;
        let cursor_count = inv.cursor.expect("cursor").count;
        assert_eq!(slot_count + cursor_count, 64);
        assert_eq!(cursor_count, 32);
    }

    #[test]
    fn close_inventory_drops_cursor() {
        let mut inv = PlayerInventoryState::alpha_defaults();
        inv.cursor = Some(ItemStack::block(1, 3));
        let result = inv.apply(InventoryCommand::CloseInventory);
        assert!(result.changed);
        assert_eq!(result.dropped_to_world, vec![ItemStack::block(1, 3)]);
        assert_eq!(inv.cursor, None);
    }
}
