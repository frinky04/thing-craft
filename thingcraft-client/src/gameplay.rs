use std::collections::VecDeque;

use bevy_ecs::prelude::Resource;

use crate::ecs::EcsRuntime;
use crate::inventory::{InventoryCommand, ItemStack, PlayerInventoryState};
use crate::tool::ToolRegistry;

pub const ALPHA_MINING_COOLDOWN_TICKS: u8 = 5;

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub struct MiningTarget {
    pub x: i32,
    pub y: i32,
    pub z: i32,
}

#[derive(Resource, Debug, Default, Clone, Copy)]
pub struct MiningState {
    pub target: Option<MiningTarget>,
    pub progress: f32,
    pub last_progress: f32,
    pub ticks: f32,
    pub cooldown_ticks: u8,
}

impl MiningState {
    pub fn stop(&mut self) {
        self.target = None;
        self.progress = 0.0;
        self.last_progress = 0.0;
        self.ticks = 0.0;
        self.cooldown_ticks = 0;
    }

    pub fn retarget(&mut self, target: MiningTarget) {
        self.target = Some(target);
        self.progress = 0.0;
        self.last_progress = 0.0;
        self.ticks = 0.0;
    }

    pub fn on_block_broken(&mut self) {
        self.progress = 0.0;
        self.last_progress = 0.0;
        self.ticks = 0.0;
        self.cooldown_ticks = ALPHA_MINING_COOLDOWN_TICKS;
    }

    pub fn render_progress(self, alpha: f32) -> f32 {
        self.last_progress + (self.progress - self.last_progress) * alpha
    }
}

#[derive(Resource, Debug, Default)]
pub struct InventoryCommandQueue {
    pub pending: VecDeque<InventoryCommand>,
}

#[derive(Debug, Default)]
pub struct InventoryCommandEvents {
    pub changed: bool,
    pub dropped_to_world: Vec<ItemStack>,
}

pub fn run_inventory_command_system(ecs_runtime: &mut EcsRuntime) -> InventoryCommandEvents {
    let pending_commands: Vec<InventoryCommand> = {
        let mut queue = ecs_runtime.world_mut().resource_mut::<InventoryCommandQueue>();
        queue.pending.drain(..).collect()
    };

    let mut events = InventoryCommandEvents::default();
    for cmd in pending_commands {
        let result = {
            let mut inventory = ecs_runtime.world_mut().resource_mut::<PlayerInventoryState>();
            inventory.apply(cmd)
        };
        events.changed |= result.changed;
        events.dropped_to_world.extend(result.dropped_to_world);
    }
    events
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum MiningToolKind {
    None,
    Tool(u16),
}

pub fn apply_post_block_break_effects(
    ecs_runtime: &mut EcsRuntime,
    tool_registry: &ToolRegistry,
    tool: MiningToolKind,
) {
    ecs_runtime.world_mut().resource_mut::<MiningState>().on_block_broken();
    if let MiningToolKind::Tool(id) = tool {
        if let Some(def) = tool_registry.get(id) {
            let cost = ToolRegistry::mining_durability_cost(def);
            ecs_runtime
                .world_mut()
                .resource_mut::<PlayerInventoryState>()
                .damage_selected_tool(cost, tool_registry);
        }
    }
}
