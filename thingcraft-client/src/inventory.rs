use std::collections::HashMap;

use bevy_ecs::prelude::Resource;
use winit::event::MouseButton;

pub const HOTBAR_SLOT_COUNT: usize = 9;
pub const MAIN_SLOT_COUNT: usize = 27;
pub const ARMOR_SLOT_COUNT: usize = 4;
pub const PLAYER_CRAFT_INPUT_SLOT_COUNT: usize = 4;
pub const TABLE_CRAFT_INPUT_SLOT_COUNT: usize = 9;
pub const INVENTORY_WIDTH: f32 = 176.0;
pub const INVENTORY_HEIGHT: f32 = 166.0;
pub const AIR_BLOCK_ID: u8 = 0;
pub const HOTBAR_DEFAULT_BLOCK_IDS: [u8; HOTBAR_SLOT_COUNT] =
    [3, 1, 4, 12, AIR_BLOCK_ID, 17, 5, 9, 50];

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum ItemKey {
    Block(u8),
    Tool(u16),
    Item(u16),
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
    pub fn item(item_id: u16, count: u8) -> Self {
        Self {
            item: ItemKey::Item(item_id),
            count,
            metadata: 0,
        }
    }

    #[must_use]
    pub fn alpha_item_id(self) -> u16 {
        match self.item {
            ItemKey::Block(block_id) => u16::from(block_id),
            ItemKey::Tool(item_id) | ItemKey::Item(item_id) => item_id,
        }
    }

    #[must_use]
    pub fn max_stack_size(self) -> u8 {
        match self.item {
            ItemKey::Block(_) => 64,
            ItemKey::Tool(_) => 1,
            ItemKey::Item(item_id) => alpha_item_max_stack_size(item_id),
        }
    }
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum PlayerSlot {
    Hotbar(u8),
    Main(u8),
    Armor(u8),
    PlayerCraftInput(u8),
    PlayerCraftResult,
    CraftingTableInput(u8),
    CraftingTableResult,
    Chest(u8),
    FurnaceInput,
    FurnaceFuel,
    FurnaceResult,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq, Hash)]
pub struct BlockPos {
    pub x: i32,
    pub y: i32,
    pub z: i32,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum InventoryMenuKind {
    Player,
    CraftingTable,
    Chest {
        primary: BlockPos,
        secondary: Option<BlockPos>,
    },
    Furnace {
        pos: BlockPos,
    },
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
    CloseMenu {
        menu: InventoryMenuKind,
    },
}

#[derive(Debug, Clone, Default)]
pub struct InventoryApplyResult {
    pub changed: bool,
    pub dropped_to_world: Vec<ItemStack>,
}

#[derive(Debug, Clone, Default)]
pub struct ChestInventory {
    slots: [Option<ItemStack>; 27],
}

impl ChestInventory {
    #[must_use]
    pub fn slot(&self, index: usize) -> Option<ItemStack> {
        self.slots.get(index).copied().flatten()
    }

    pub fn set_slot(&mut self, index: usize, stack: Option<ItemStack>) {
        if let Some(dst) = self.slots.get_mut(index) {
            *dst = stack;
        }
    }

    pub fn slots(&self) -> &[Option<ItemStack>; 27] {
        &self.slots
    }
}

#[derive(Debug, Clone, Default)]
pub struct FurnaceInventory {
    slots: [Option<ItemStack>; 3],
    pub fuel_time: i32,
    pub total_fuel_time: i32,
    pub cook_time: i32,
}

impl FurnaceInventory {
    #[must_use]
    pub fn slot(&self, index: usize) -> Option<ItemStack> {
        self.slots.get(index).copied().flatten()
    }

    pub fn set_slot(&mut self, index: usize, stack: Option<ItemStack>) {
        if let Some(dst) = self.slots.get_mut(index) {
            *dst = stack;
        }
    }

    #[must_use]
    pub fn has_fuel(&self) -> bool {
        self.fuel_time > 0
    }

    #[must_use]
    pub fn get_lit_progress(&self, range: i32) -> i32 {
        let total = if self.total_fuel_time == 0 {
            200
        } else {
            self.total_fuel_time
        };
        self.fuel_time * range / total
    }

    #[must_use]
    pub fn get_cook_progress(&self, range: i32) -> i32 {
        self.cook_time * range / 200
    }
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub struct FurnaceTickResult {
    pub changed: bool,
    pub lit_state_changed: bool,
    pub now_lit: bool,
}

#[derive(Resource, Debug, Clone, Default)]
pub struct ContainerRuntimeState {
    pub chests: HashMap<BlockPos, ChestInventory>,
    pub furnaces: HashMap<BlockPos, FurnaceInventory>,
}

impl ContainerRuntimeState {
    pub fn ensure_chest(&mut self, pos: BlockPos) {
        self.chests.entry(pos).or_default();
    }

    pub fn remove_chest(&mut self, pos: BlockPos) -> Option<ChestInventory> {
        self.chests.remove(&pos)
    }

    pub fn ensure_furnace(&mut self, pos: BlockPos) {
        self.furnaces.entry(pos).or_default();
    }

    pub fn remove_furnace(&mut self, pos: BlockPos) -> Option<FurnaceInventory> {
        self.furnaces.remove(&pos)
    }

    #[must_use]
    pub fn furnace(&self, pos: BlockPos) -> Option<&FurnaceInventory> {
        self.furnaces.get(&pos)
    }

    #[must_use]
    pub fn chest(&self, pos: BlockPos) -> Option<&ChestInventory> {
        self.chests.get(&pos)
    }

    pub fn tick_furnaces(&mut self) -> Vec<(BlockPos, FurnaceTickResult)> {
        let mut changed = Vec::new();
        for (pos, furnace) in &mut self.furnaces {
            let tick = tick_furnace_inventory(furnace);
            if tick.changed || tick.lit_state_changed {
                changed.push((*pos, tick));
            }
        }
        changed
    }
}

fn tick_furnace_inventory(furnace: &mut FurnaceInventory) -> FurnaceTickResult {
    let was_lit = furnace.fuel_time > 0;
    let mut changed = false;
    if furnace.fuel_time > 0 {
        furnace.fuel_time -= 1;
    }

    if furnace.fuel_time == 0 && furnace_can_cook(furnace) {
        let fuel_time = furnace
            .slot(1)
            .map_or(0, |stack| alpha_fuel_time_for_item(stack.alpha_item_id()));
        furnace.total_fuel_time = fuel_time;
        furnace.fuel_time = fuel_time;
        if fuel_time > 0 {
            changed = true;
            if let Some(mut fuel) = furnace.slot(1) {
                fuel.count = fuel.count.saturating_sub(1);
                furnace.set_slot(1, if fuel.count == 0 { None } else { Some(fuel) });
            }
        }
    }

    if furnace.fuel_time > 0 && furnace_can_cook(furnace) {
        furnace.cook_time += 1;
        if furnace.cook_time == 200 {
            furnace.cook_time = 0;
            furnace_finish_cooking(furnace);
            changed = true;
        }
    } else if furnace.cook_time != 0 {
        furnace.cook_time = 0;
        changed = true;
    }

    let is_lit = furnace.fuel_time > 0;
    FurnaceTickResult {
        changed,
        lit_state_changed: was_lit != is_lit,
        now_lit: is_lit,
    }
}

fn furnace_can_cook(furnace: &FurnaceInventory) -> bool {
    let Some(input) = furnace.slot(0) else {
        return false;
    };
    let Some(result_item) = alpha_furnace_result_for_input(input.alpha_item_id()) else {
        return false;
    };
    let Some(output) = furnace.slot(2) else {
        return true;
    };
    if output.alpha_item_id() != result_item {
        return false;
    }
    output.count < output.max_stack_size()
}

fn furnace_finish_cooking(furnace: &mut FurnaceInventory) {
    let Some(input) = furnace.slot(0) else {
        return;
    };
    let Some(result_item) = alpha_furnace_result_for_input(input.alpha_item_id()) else {
        return;
    };

    let result_stack = if result_item < 256 {
        ItemStack::block(result_item as u8, 1)
    } else {
        ItemStack::item(result_item, 1)
    };
    match furnace.slot(2) {
        None => furnace.set_slot(2, Some(result_stack)),
        Some(mut output)
            if output.item == result_stack.item && output.metadata == result_stack.metadata =>
        {
            output.count = output.count.saturating_add(1);
            furnace.set_slot(2, Some(output));
        }
        _ => return,
    }

    let mut new_input = input;
    new_input.count = new_input.count.saturating_sub(1);
    furnace.set_slot(0, if new_input.count == 0 { None } else { Some(new_input) });
}

#[derive(Resource, Debug, Clone, Eq, PartialEq)]
pub struct PlayerInventoryState {
    hotbar: [Option<ItemStack>; HOTBAR_SLOT_COUNT],
    main: [Option<ItemStack>; MAIN_SLOT_COUNT],
    armor: [Option<ItemStack>; ARMOR_SLOT_COUNT],
    player_craft_input: [Option<ItemStack>; PLAYER_CRAFT_INPUT_SLOT_COUNT],
    player_craft_result: Option<ItemStack>,
    table_craft_input: [Option<ItemStack>; TABLE_CRAFT_INPUT_SLOT_COUNT],
    table_craft_result: Option<ItemStack>,
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
            player_craft_input: [None; PLAYER_CRAFT_INPUT_SLOT_COUNT],
            player_craft_result: None,
            table_craft_input: [None; TABLE_CRAFT_INPUT_SLOT_COUNT],
            table_craft_result: None,
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
    pub fn player_craft_input_stacks(&self) -> &[Option<ItemStack>; PLAYER_CRAFT_INPUT_SLOT_COUNT] {
        &self.player_craft_input
    }

    #[must_use]
    pub fn player_craft_result(&self) -> Option<ItemStack> {
        self.player_craft_result
    }

    #[must_use]
    pub fn table_craft_input_stacks(&self) -> &[Option<ItemStack>; TABLE_CRAFT_INPUT_SLOT_COUNT] {
        &self.table_craft_input
    }

    #[must_use]
    pub fn table_craft_result(&self) -> Option<ItemStack> {
        self.table_craft_result
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
    pub fn damage_selected_tool(
        &mut self,
        amount: u16,
        tool_registry: &crate::tool::ToolRegistry,
    ) -> bool {
        if amount == 0 {
            return false;
        }
        let slot_idx = usize::from(self.selected_hotbar.min((HOTBAR_SLOT_COUNT - 1) as u8));
        if let Some(stack) = &mut self.hotbar[slot_idx] {
            if let ItemKey::Tool(tool_id) = stack.item {
                stack.metadata = stack.metadata.saturating_add(amount);
                let max_damage = tool_registry.get(tool_id).map_or(0, |def| def.max_damage);
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
        self.apply_with_crafting_and_containers(cmd, None, None, None)
    }

    #[must_use]
    pub fn apply_with_crafting_and_containers(
        &mut self,
        cmd: InventoryCommand,
        crafting: Option<&crate::crafting::CraftingRegistry>,
        mut containers: Option<&mut ContainerRuntimeState>,
        open_menu: Option<InventoryMenuKind>,
    ) -> InventoryApplyResult {
        let mut result = InventoryApplyResult::default();
        let prev_player_crafting = self.player_craft_input;
        let prev_table_crafting = self.table_craft_input;
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
                    if consume_slot_amount(self, containers.as_deref_mut(), open_menu, slot, amount)
                    {
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
            InventoryCommand::CloseMenu { menu } => {
                if let Some(cursor) = self.cursor.take() {
                    result.dropped_to_world.push(cursor);
                    result.changed = true;
                }
                match menu {
                    InventoryMenuKind::Player => {
                        for slot in &mut self.player_craft_input {
                            if let Some(stack) = slot.take() {
                                result.dropped_to_world.push(stack);
                                result.changed = true;
                            }
                        }
                        self.player_craft_result = None;
                    }
                    InventoryMenuKind::CraftingTable => {
                        for slot in &mut self.table_craft_input {
                            if let Some(stack) = slot.take() {
                                result.dropped_to_world.push(stack);
                                result.changed = true;
                            }
                        }
                        self.table_craft_result = None;
                    }
                    InventoryMenuKind::Chest { .. } | InventoryMenuKind::Furnace { .. } => {}
                }
            }
            InventoryCommand::ClickSlot { slot, button } => match (slot, button) {
                (PlayerSlot::PlayerCraftResult, MouseButton::Left)
                | (PlayerSlot::PlayerCraftResult, MouseButton::Right) => {
                    if take_player_crafting_result(self) {
                        result.changed = true;
                    }
                }
                (PlayerSlot::CraftingTableResult, MouseButton::Left)
                | (PlayerSlot::CraftingTableResult, MouseButton::Right) => {
                    if take_table_crafting_result(self) {
                        result.changed = true;
                    }
                }
                (_, MouseButton::Left) | (_, MouseButton::Right) => {
                    if apply_slot_click(
                        self,
                        containers.as_deref_mut(),
                        open_menu,
                        slot,
                        button,
                    ) {
                        result.changed = true;
                    }
                }
                _ => {}
            },
        }

        if let Some(crafting) = crafting {
            if self.player_craft_input != prev_player_crafting {
                self.player_craft_result = crafting.player_result(self.player_craft_input);
                result.changed = true;
            }
            if self.table_craft_input != prev_table_crafting {
                self.table_craft_result =
                    crafting.result_for_grid(table_crafting_grid(self.table_craft_input));
                result.changed = true;
            }
        }
        result
    }

    #[must_use]
    pub fn snapshot(&self) -> InventorySnapshot {
        InventorySnapshot {
            hotbar: self.hotbar,
            main: self.main,
            armor: self.armor,
            player_craft_input: self.player_craft_input,
            player_craft_result: self.player_craft_result,
            table_craft_input: self.table_craft_input,
            table_craft_result: self.table_craft_result,
            selected_hotbar: self.selected_hotbar,
            cursor: self.cursor,
        }
    }
}

fn apply_slot_click(
    inv: &mut PlayerInventoryState,
    mut containers: Option<&mut ContainerRuntimeState>,
    open_menu: Option<InventoryMenuKind>,
    slot: PlayerSlot,
    button: MouseButton,
) -> bool {
    if matches!(
        slot,
        PlayerSlot::PlayerCraftResult | PlayerSlot::CraftingTableResult
    ) {
        return false;
    }
    let slot_max = slot_max_stack_size(slot, open_menu);
    let slot_allows = |stack: ItemStack| slot_accepts_item(slot, stack);
    let current_slot_item = get_slot(inv, containers.as_deref(), open_menu, slot);
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
        set_slot(
            inv,
            containers.as_deref_mut(),
            open_menu,
            slot,
            if src.count == 0 { None } else { Some(src) },
        );
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
            containers.as_deref_mut(),
            open_menu,
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
            set_slot(inv, containers.as_deref_mut(), open_menu, slot, None);
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
        set_slot(inv, containers.as_deref_mut(), open_menu, slot, Some(cursor));
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
    set_slot(
        inv,
        containers.as_deref_mut(),
        open_menu,
        slot,
        Some(slot_item),
    );
    inv.cursor = if cursor.count == 0 {
        None
    } else {
        Some(cursor)
    };
    true
}

fn consume_slot_amount(
    inv: &mut PlayerInventoryState,
    containers: Option<&mut ContainerRuntimeState>,
    open_menu: Option<InventoryMenuKind>,
    slot: PlayerSlot,
    amount: u8,
) -> bool {
    let Some(mut stack) = get_slot(inv, containers.as_deref(), open_menu, slot) else {
        return false;
    };
    if stack.count < amount {
        return false;
    }
    stack.count -= amount;
    set_slot(
        inv,
        containers,
        open_menu,
        slot,
        if stack.count == 0 { None } else { Some(stack) },
    );
    true
}

fn get_slot(
    inv: &PlayerInventoryState,
    containers: Option<&ContainerRuntimeState>,
    open_menu: Option<InventoryMenuKind>,
    slot: PlayerSlot,
) -> Option<ItemStack> {
    match slot {
        PlayerSlot::Hotbar(i) => inv.hotbar.get(usize::from(i)).copied().flatten(),
        PlayerSlot::Main(i) => inv.main.get(usize::from(i)).copied().flatten(),
        PlayerSlot::Armor(i) => inv.armor.get(usize::from(i)).copied().flatten(),
        PlayerSlot::PlayerCraftInput(i) => inv
            .player_craft_input
            .get(usize::from(i))
            .copied()
            .flatten(),
        PlayerSlot::PlayerCraftResult => inv.player_craft_result,
        PlayerSlot::CraftingTableInput(i) => {
            inv.table_craft_input.get(usize::from(i)).copied().flatten()
        }
        PlayerSlot::CraftingTableResult => inv.table_craft_result,
        PlayerSlot::Chest(i) => {
            let (menu, containers) = (open_menu?, containers?);
            let InventoryMenuKind::Chest { primary, secondary } = menu else {
                return None;
            };
            if i < 27 {
                return containers
                    .chests
                    .get(&primary)
                    .and_then(|chest| chest.slot(usize::from(i)));
            }
            let secondary = secondary?;
            containers
                .chests
                .get(&secondary)
                .and_then(|chest| chest.slot(usize::from(i.saturating_sub(27))))
        }
        PlayerSlot::FurnaceInput => {
            let (menu, containers) = (open_menu?, containers?);
            let InventoryMenuKind::Furnace { pos } = menu else {
                return None;
            };
            containers.furnaces.get(&pos).and_then(|f| f.slot(0))
        }
        PlayerSlot::FurnaceFuel => {
            let (menu, containers) = (open_menu?, containers?);
            let InventoryMenuKind::Furnace { pos } = menu else {
                return None;
            };
            containers.furnaces.get(&pos).and_then(|f| f.slot(1))
        }
        PlayerSlot::FurnaceResult => {
            let (menu, containers) = (open_menu?, containers?);
            let InventoryMenuKind::Furnace { pos } = menu else {
                return None;
            };
            containers.furnaces.get(&pos).and_then(|f| f.slot(2))
        }
    }
}

fn set_slot(
    inv: &mut PlayerInventoryState,
    containers: Option<&mut ContainerRuntimeState>,
    open_menu: Option<InventoryMenuKind>,
    slot: PlayerSlot,
    stack: Option<ItemStack>,
) {
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
        PlayerSlot::PlayerCraftInput(i) => {
            if let Some(dst) = inv.player_craft_input.get_mut(usize::from(i)) {
                *dst = stack;
            }
        }
        PlayerSlot::PlayerCraftResult => {}
        PlayerSlot::CraftingTableInput(i) => {
            if let Some(dst) = inv.table_craft_input.get_mut(usize::from(i)) {
                *dst = stack;
            }
        }
        PlayerSlot::CraftingTableResult => {}
        PlayerSlot::Chest(i) => {
            let (Some(containers), Some(InventoryMenuKind::Chest { primary, secondary })) =
                (containers, open_menu)
            else {
                return;
            };
            if i < 27 {
                containers
                    .chests
                    .entry(primary)
                    .or_default()
                    .set_slot(usize::from(i), stack);
            } else if let Some(secondary) = secondary {
                containers
                    .chests
                    .entry(secondary)
                    .or_default()
                    .set_slot(usize::from(i.saturating_sub(27)), stack);
            }
        }
        PlayerSlot::FurnaceInput => {
            let (Some(containers), Some(InventoryMenuKind::Furnace { pos })) = (containers, open_menu)
            else {
                return;
            };
            containers.furnaces.entry(pos).or_default().set_slot(0, stack);
        }
        PlayerSlot::FurnaceFuel => {
            let (Some(containers), Some(InventoryMenuKind::Furnace { pos })) = (containers, open_menu)
            else {
                return;
            };
            containers.furnaces.entry(pos).or_default().set_slot(1, stack);
        }
        PlayerSlot::FurnaceResult => {
            let (Some(containers), Some(InventoryMenuKind::Furnace { pos })) = (containers, open_menu)
            else {
                return;
            };
            containers.furnaces.entry(pos).or_default().set_slot(2, stack);
        }
    }
}

fn slot_max_stack_size(slot: PlayerSlot, open_menu: Option<InventoryMenuKind>) -> u8 {
    match slot {
        PlayerSlot::Armor(_) => 1,
        PlayerSlot::PlayerCraftResult | PlayerSlot::CraftingTableResult => 0,
        PlayerSlot::FurnaceResult => 64,
        PlayerSlot::Hotbar(_)
        | PlayerSlot::Main(_)
        | PlayerSlot::PlayerCraftInput(_)
        | PlayerSlot::CraftingTableInput(_)
        | PlayerSlot::Chest(_) => 64,
        PlayerSlot::FurnaceInput | PlayerSlot::FurnaceFuel => {
            if open_menu.is_some() {
                64
            } else {
                0
            }
        }
    }
}

fn slot_accepts_item(slot: PlayerSlot, stack: ItemStack) -> bool {
    match slot {
        PlayerSlot::Armor(_) => stack.count >= 1,
        PlayerSlot::PlayerCraftResult | PlayerSlot::CraftingTableResult | PlayerSlot::FurnaceResult => false,
        PlayerSlot::Hotbar(_)
        | PlayerSlot::Main(_)
        | PlayerSlot::PlayerCraftInput(_)
        | PlayerSlot::CraftingTableInput(_)
        | PlayerSlot::Chest(_) => stack.count >= 1,
        PlayerSlot::FurnaceInput => stack.count >= 1,
        PlayerSlot::FurnaceFuel => alpha_fuel_time_for_item(stack.alpha_item_id()) > 0,
    }
}

fn take_player_crafting_result(inv: &mut PlayerInventoryState) -> bool {
    let Some(result_stack) = inv.player_craft_result else {
        return false;
    };
    if !can_take_craft_result(inv.cursor, result_stack) {
        return false;
    }

    if let Some(cursor) = inv.cursor.as_mut() {
        cursor.count = cursor.count.saturating_add(result_stack.count);
    } else {
        inv.cursor = Some(result_stack);
    }

    for slot in &mut inv.player_craft_input {
        let Some(mut stack) = *slot else {
            continue;
        };
        stack.count = stack.count.saturating_sub(1);
        *slot = if stack.count == 0 { None } else { Some(stack) };
    }
    true
}

fn take_table_crafting_result(inv: &mut PlayerInventoryState) -> bool {
    let Some(result_stack) = inv.table_craft_result else {
        return false;
    };
    if !can_take_craft_result(inv.cursor, result_stack) {
        return false;
    }

    if let Some(cursor) = inv.cursor.as_mut() {
        cursor.count = cursor.count.saturating_add(result_stack.count);
    } else {
        inv.cursor = Some(result_stack);
    }

    for slot in &mut inv.table_craft_input {
        let Some(mut stack) = *slot else {
            continue;
        };
        stack.count = stack.count.saturating_sub(1);
        *slot = if stack.count == 0 { None } else { Some(stack) };
    }
    true
}

fn can_take_craft_result(cursor: Option<ItemStack>, result: ItemStack) -> bool {
    let Some(cursor) = cursor else {
        return true;
    };
    cursor.item == result.item
        && cursor.metadata == result.metadata
        && u16::from(cursor.count) + u16::from(result.count) <= u16::from(cursor.max_stack_size())
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub struct InventorySnapshot {
    hotbar: [Option<ItemStack>; HOTBAR_SLOT_COUNT],
    main: [Option<ItemStack>; MAIN_SLOT_COUNT],
    armor: [Option<ItemStack>; ARMOR_SLOT_COUNT],
    player_craft_input: [Option<ItemStack>; PLAYER_CRAFT_INPUT_SLOT_COUNT],
    player_craft_result: Option<ItemStack>,
    table_craft_input: [Option<ItemStack>; TABLE_CRAFT_INPUT_SLOT_COUNT],
    table_craft_result: Option<ItemStack>,
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
pub fn inventory_layout_for_menu(
    screen_w: f32,
    screen_h: f32,
    menu: InventoryMenuKind,
) -> InventoryScreenLayout {
    let scale = gui_scale(screen_w, screen_h);
    let gui_w = (screen_w / scale).floor();
    let gui_h = (screen_h / scale).floor();
    let (panel_w, panel_h) = menu_panel_size(menu);
    InventoryScreenLayout {
        scale,
        left: ((gui_w - panel_w) * 0.5).floor(),
        top: ((gui_h - panel_h) * 0.5).floor(),
    }
}

#[must_use]
pub fn menu_panel_size(menu: InventoryMenuKind) -> (f32, f32) {
    match menu {
        InventoryMenuKind::Player | InventoryMenuKind::CraftingTable => {
            (INVENTORY_WIDTH, INVENTORY_HEIGHT)
        }
        InventoryMenuKind::Chest { secondary, .. } => {
            let rows = if secondary.is_some() { 6.0 } else { 3.0 };
            (176.0, 114.0 + rows * 18.0)
        }
        InventoryMenuKind::Furnace { .. } => (176.0, 166.0),
    }
}

#[must_use]
pub fn slot_gui_xy(menu: InventoryMenuKind, slot: PlayerSlot) -> (f32, f32) {
    match slot {
        PlayerSlot::PlayerCraftResult => (144.0, 36.0),
        PlayerSlot::PlayerCraftInput(i) => {
            let idx = usize::from(i).min(PLAYER_CRAFT_INPUT_SLOT_COUNT - 1);
            let row = idx / 2;
            let col = idx % 2;
            (88.0 + col as f32 * 18.0, 26.0 + row as f32 * 18.0)
        }
        PlayerSlot::CraftingTableResult => (124.0, 35.0),
        PlayerSlot::CraftingTableInput(i) => {
            let idx = usize::from(i).min(TABLE_CRAFT_INPUT_SLOT_COUNT - 1);
            let row = idx / 3;
            let col = idx % 3;
            (30.0 + col as f32 * 18.0, 17.0 + row as f32 * 18.0)
        }
        PlayerSlot::Chest(i) => {
            let idx = usize::from(i).min(53);
            let row = idx / 9;
            let col = idx % 9;
            (8.0 + col as f32 * 18.0, 18.0 + row as f32 * 18.0)
        }
        PlayerSlot::FurnaceInput => (56.0, 17.0),
        PlayerSlot::FurnaceFuel => (56.0, 53.0),
        PlayerSlot::FurnaceResult => (116.0, 35.0),
        PlayerSlot::Armor(i) => (8.0, 8.0 + f32::from(i) * 18.0),
        PlayerSlot::Main(i) => {
            let idx = usize::from(i).min(MAIN_SLOT_COUNT - 1);
            let row = idx / 9;
            let col = idx % 9;
            let base_y = match menu {
                InventoryMenuKind::Chest { secondary, .. } => {
                    let rows = if secondary.is_some() { 6.0 } else { 3.0 };
                    103.0 + (rows - 4.0) * 18.0
                }
                _ => 84.0,
            };
            (8.0 + col as f32 * 18.0, base_y + row as f32 * 18.0)
        }
        PlayerSlot::Hotbar(i) => {
            let y = match menu {
                InventoryMenuKind::Chest { secondary, .. } => {
                    let rows = if secondary.is_some() { 6.0 } else { 3.0 };
                    161.0 + (rows - 4.0) * 18.0
                }
                _ => 142.0,
            };
            (8.0 + f32::from(i) * 18.0, y)
        }
    }
}

#[must_use]
pub fn hit_test_slot_for_menu(
    mouse_x: f32,
    mouse_y: f32,
    screen_w: f32,
    screen_h: f32,
    menu: InventoryMenuKind,
) -> Option<PlayerSlot> {
    let layout = inventory_layout_for_menu(screen_w, screen_h, menu);
    let mx = mouse_x / layout.scale - layout.left;
    let my = mouse_y / layout.scale - layout.top;
    match menu {
        InventoryMenuKind::Player => {
            let result_slot = PlayerSlot::PlayerCraftResult;
            if slot_contains(menu, result_slot, mx, my) {
                return Some(result_slot);
            }
            for i in 0..PLAYER_CRAFT_INPUT_SLOT_COUNT {
                let slot = PlayerSlot::PlayerCraftInput(i as u8);
                if slot_contains(menu, slot, mx, my) {
                    return Some(slot);
                }
            }
            for i in 0..ARMOR_SLOT_COUNT {
                let slot = PlayerSlot::Armor(i as u8);
                if slot_contains(menu, slot, mx, my) {
                    return Some(slot);
                }
            }
        }
        InventoryMenuKind::CraftingTable => {
            let result_slot = PlayerSlot::CraftingTableResult;
            if slot_contains(menu, result_slot, mx, my) {
                return Some(result_slot);
            }
            for i in 0..TABLE_CRAFT_INPUT_SLOT_COUNT {
                let slot = PlayerSlot::CraftingTableInput(i as u8);
                if slot_contains(menu, slot, mx, my) {
                    return Some(slot);
                }
            }
        }
        InventoryMenuKind::Chest { secondary, .. } => {
            let chest_rows = if secondary.is_some() { 6 } else { 3 };
            for i in 0..(chest_rows * 9) {
                let slot = PlayerSlot::Chest(i as u8);
                if slot_contains(menu, slot, mx, my) {
                    return Some(slot);
                }
            }
        }
        InventoryMenuKind::Furnace { .. } => {
            for slot in [
                PlayerSlot::FurnaceInput,
                PlayerSlot::FurnaceFuel,
                PlayerSlot::FurnaceResult,
            ] {
                if slot_contains(menu, slot, mx, my) {
                    return Some(slot);
                }
            }
        }
    }
    for i in 0..MAIN_SLOT_COUNT {
        let slot = PlayerSlot::Main(i as u8);
        if slot_contains(menu, slot, mx, my) {
            return Some(slot);
        }
    }
    for i in 0..HOTBAR_SLOT_COUNT {
        let slot = PlayerSlot::Hotbar(i as u8);
        if slot_contains(menu, slot, mx, my) {
            return Some(slot);
        }
    }
    None
}

fn slot_contains(menu: InventoryMenuKind, slot: PlayerSlot, mx: f32, my: f32) -> bool {
    let (sx, sy) = slot_gui_xy(menu, slot);
    mx >= sx - 1.0 && mx < sx + 17.0 && my >= sy - 1.0 && my < sy + 17.0
}

fn table_crafting_grid(slots: [Option<ItemStack>; TABLE_CRAFT_INPUT_SLOT_COUNT]) -> [i32; 9] {
    let mut grid = [-1; 9];
    for y in 0..3 {
        for x in 0..3 {
            let slot_index = x + y * 3;
            let alpha_id = slots[slot_index].map_or(-1, |stack| stack.alpha_item_id() as i32);
            grid[x + y * 3] = alpha_id;
        }
    }
    grid
}

#[must_use]
pub fn alpha_furnace_result_for_input(input_id: u16) -> Option<u16> {
    let out = match input_id {
        15 => 265, // iron ore -> iron ingot
        14 => 266, // gold ore -> gold ingot
        56 => 264, // diamond ore -> diamond
        12 => 20,  // sand -> glass block
        319 => 320, // raw pork -> cooked pork
        349 => 350, // raw fish -> cooked fish
        4 => 1,    // cobblestone -> stone
        337 => 336, // clay -> brick
        _ => return None,
    };
    Some(out)
}

#[must_use]
pub fn alpha_fuel_time_for_item(item_id: u16) -> i32 {
    match item_id {
        280 => 100,   // stick
        263 => 1600,  // coal
        327 => 20000, // lava bucket
        // Alpha: any wooden block item (<256 and wood material) burns for 300.
        5 | 17 | 25 | 47 | 53 | 54 | 58 | 63 | 64 | 65 | 68 | 72 | 84 | 85 | 86 | 91 => 300,
        _ => 0,
    }
}

#[must_use]
pub fn alpha_item_max_stack_size(item_id: u16) -> u8 {
    match item_id {
        // Armor
        298..=317 => 1,
        // Core non-stackables in alpha crafting outputs.
        261 | 262 | 267 | 268 | 269 | 270 | 271 | 272 | 273 | 274 | 275 | 276 | 277 | 278 | 279
        | 283 | 284 | 285 | 286 | 290 | 291 | 292 | 293 | 294 | 321 | 323 | 324 | 325 | 328
        | 330 | 333 | 342 | 343 | 359 => 1,
        _ => 64,
    }
}

#[must_use]
pub fn alpha_item_sprite_index(item_id: u16) -> Option<u16> {
    let sprite = match item_id {
        256 => 82,
        257 => 98,
        258 => 114,
        259 => 5,
        260 => 10,
        261 => 21,
        262 => 37,
        263 => 7,
        264 => 55,
        265 => 23,
        266 => 39,
        267 => 66,
        268 => 64,
        269 => 80,
        270 => 96,
        271 => 112,
        272 => 65,
        273 => 81,
        274 => 97,
        275 => 113,
        276 => 67,
        277 => 83,
        278 => 99,
        279 => 115,
        280 => 53,
        281 => 71,
        282 => 72,
        283 => 68,
        284 => 84,
        285 => 100,
        286 => 116,
        287 => 8,
        288 => 24,
        289 => 40,
        290 => 128,
        291 => 129,
        292 => 130,
        293 => 131,
        294 => 132,
        296 => 25,
        297 => 41,
        298 => 0,
        299 => 16,
        300 => 32,
        301 => 48,
        302 => 1,
        303 => 17,
        304 => 33,
        305 => 49,
        306 => 2,
        307 => 18,
        308 => 34,
        309 => 50,
        310 => 3,
        311 => 19,
        312 => 35,
        313 => 51,
        314 => 4,
        315 => 20,
        316 => 36,
        317 => 52,
        318 => 6,
        320 => 87,
        321 => 26,
        322 => 11,
        323 => 42,
        324 => 43,
        325 => 74,
        328 => 135,
        330 => 44,
        331 => 56,
        332 => 14,
        333 => 136,
        334 => 103,
        336 => 22,
        337 => 57,
        338 => 27,
        339 => 58,
        340 => 59,
        341 => 30,
        342 => 151,
        343 => 167,
        345 => 54,
        346 => 69,
        347 => 70,
        348 => 73,
        _ => return None,
    };
    Some(sprite)
}

#[cfg(test)]
mod tests {
    use super::{
        InventoryCommand, InventoryMenuKind, ItemStack, MouseButton, PlayerInventoryState,
        PlayerSlot,
    };

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
        let result = inv.apply(InventoryCommand::CloseMenu {
            menu: InventoryMenuKind::Player,
        });
        assert!(result.changed);
        assert_eq!(result.dropped_to_world, vec![ItemStack::block(1, 3)]);
        assert_eq!(inv.cursor, None);
    }
}
