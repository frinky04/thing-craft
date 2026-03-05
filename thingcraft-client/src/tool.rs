use crate::world::{BlockRegistry, MaterialKind};

// ---------------------------------------------------------------------------
// Alpha 1.2.6 Tool Definitions
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum ToolClass {
    Pickaxe,
    Axe,
    Shovel,
    Sword,
    Hoe,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum ToolMaterial {
    Wood,
    Stone,
    Iron,
    Gold,
    Diamond,
}

#[derive(Debug, Clone, Copy)]
#[allow(dead_code)]
pub struct ToolDef {
    pub id: u16,
    pub name: &'static str,
    pub class: ToolClass,
    pub material: ToolMaterial,
    pub harvest_tier: u8,
    pub mining_speed: f32,
    pub max_damage: u16,
    pub attack_damage: i32,
    pub sprite_index: u16,
}

/// Tool IDs range from 256..320; we offset by 256 to store in a compact array.
const TOOL_ID_OFFSET: u16 = 256;
const TOOL_ARRAY_LEN: usize = 64;

pub struct ToolRegistry {
    defs: [Option<ToolDef>; TOOL_ARRAY_LEN],
}

impl ToolRegistry {
    #[must_use]
    pub fn alpha_1_2_6() -> Self {
        let mut defs = [None; TOOL_ARRAY_LEN];
        let tools = all_alpha_tools();
        for tool in &tools {
            if let Some(idx) = tool.id.checked_sub(TOOL_ID_OFFSET) {
                let idx = idx as usize;
                if idx < defs.len() {
                    defs[idx] = Some(*tool);
                }
            }
        }
        Self { defs }
    }

    #[must_use]
    pub fn get(&self, item_id: u16) -> Option<&ToolDef> {
        let idx = item_id.checked_sub(TOOL_ID_OFFSET)? as usize;
        self.defs.get(idx).and_then(|o| o.as_ref())
    }

    /// Returns the effective mining speed for a tool against a specific block.
    /// If the tool's class matches the block's effective-block list, returns
    /// the tool's mining speed; otherwise returns 1.0 (bare-hand speed).
    /// Swords always return 1.5 for all blocks.
    #[must_use]
    pub fn mining_speed_for_block(
        &self,
        tool: &ToolDef,
        block_id: u8,
        _registry: &BlockRegistry,
    ) -> f32 {
        match tool.class {
            ToolClass::Sword => 1.5,
            ToolClass::Hoe => 1.0,
            ToolClass::Pickaxe => {
                if is_pickaxe_effective(block_id) {
                    tool.mining_speed
                } else {
                    1.0
                }
            }
            ToolClass::Axe => {
                if is_axe_effective(block_id) {
                    tool.mining_speed
                } else {
                    1.0
                }
            }
            ToolClass::Shovel => {
                if is_shovel_effective(block_id) {
                    tool.mining_speed
                } else {
                    1.0
                }
            }
        }
    }

    /// Returns true if the held tool can harvest drops from the given block.
    /// Blocks without harvest gates always return true.
    #[must_use]
    pub fn can_harvest(&self, tool: &ToolDef, block_id: u8, registry: &BlockRegistry) -> bool {
        if let Some((required_class, min_tier)) = required_harvest_tier(block_id, registry) {
            tool.class == required_class && tool.harvest_tier >= min_tier
        } else {
            true
        }
    }

    /// Durability damage per block mined.
    #[must_use]
    pub fn mining_durability_cost(tool: &ToolDef) -> u16 {
        match tool.class {
            ToolClass::Sword => 2,
            ToolClass::Hoe => 0,
            _ => 1,
        }
    }
}

/// Returns the required (ToolClass, min_tier) to harvest drops from a block,
/// or None if the block has no harvest gate (anyone can harvest).
#[must_use]
pub fn required_harvest_tier(block_id: u8, registry: &BlockRegistry) -> Option<(ToolClass, u8)> {
    match block_id {
        // Obsidian: diamond pickaxe (tier 3)
        49 => Some((ToolClass::Pickaxe, 3)),
        // Diamond ore/block: iron pickaxe (tier 2)
        56 | 57 => Some((ToolClass::Pickaxe, 2)),
        // Gold ore/block: iron pickaxe (tier 2)
        14 | 41 => Some((ToolClass::Pickaxe, 2)),
        // Redstone ore/lit redstone ore: iron pickaxe (tier 2)
        73 | 74 => Some((ToolClass::Pickaxe, 2)),
        // Iron ore/block: stone pickaxe (tier 1)
        15 | 42 => Some((ToolClass::Pickaxe, 1)),
        // Snow layer, snow block: any shovel (tier 0)
        78 | 80 => Some((ToolClass::Shovel, 0)),
        _ => {
            // All stone/metal material blocks require any pickaxe (tier 0).
            let mat = registry.material_of(block_id);
            if mat == MaterialKind::Stone || mat == MaterialKind::Metal {
                Some((ToolClass::Pickaxe, 0))
            } else {
                None
            }
        }
    }
}

/// Returns true if bare hands can harvest the given block (no tool required).
#[must_use]
pub fn hand_can_harvest(block_id: u8, registry: &BlockRegistry) -> bool {
    required_harvest_tier(block_id, registry).is_none()
}

// ---------------------------------------------------------------------------
// Effective block lists (from decompiled Alpha 1.2.6)
// ---------------------------------------------------------------------------

fn is_pickaxe_effective(block_id: u8) -> bool {
    matches!(
        block_id,
        1 | 4 | 14 | 15 | 16 | 41 | 42 | 43 | 44 | 48 | 49 | 56 | 57 | 73 | 74 | 79 | 87
    )
}

fn is_axe_effective(block_id: u8) -> bool {
    matches!(block_id, 5 | 17 | 47 | 54)
}

fn is_shovel_effective(block_id: u8) -> bool {
    matches!(block_id, 2 | 3 | 12 | 13 | 78 | 80 | 82)
}

// ---------------------------------------------------------------------------
// All 25 Alpha 1.2.6 tools
// ---------------------------------------------------------------------------

fn all_alpha_tools() -> [ToolDef; 25] {
    [
        // ---- Pickaxes ----
        // ToolItem: attackDamage = base + tier, miningSpeed = (tier+1)*2, maxDamage = 32 << tier (4x for tier 3)
        // Pickaxe base damage: 2
        ToolDef {
            id: 270,
            name: "Wooden Pickaxe",
            class: ToolClass::Pickaxe,
            material: ToolMaterial::Wood,
            harvest_tier: 0,
            mining_speed: 2.0, // (0+1)*2
            max_damage: 32,    // 32 << 0 = 32; breaks when metadata > 32, so 33 uses
            attack_damage: 2,  // 2 + 0
            sprite_index: 96,
        },
        ToolDef {
            id: 274,
            name: "Stone Pickaxe",
            class: ToolClass::Pickaxe,
            material: ToolMaterial::Stone,
            harvest_tier: 1,
            mining_speed: 4.0, // (1+1)*2
            max_damage: 64,    // 32 << 1
            attack_damage: 3,  // 2 + 1
            sprite_index: 97,
        },
        ToolDef {
            id: 257,
            name: "Iron Pickaxe",
            class: ToolClass::Pickaxe,
            material: ToolMaterial::Iron,
            harvest_tier: 2,
            mining_speed: 6.0, // (2+1)*2
            max_damage: 128,   // 32 << 2
            attack_damage: 4,  // 2 + 2
            sprite_index: 98,
        },
        ToolDef {
            id: 285,
            name: "Golden Pickaxe",
            class: ToolClass::Pickaxe,
            material: ToolMaterial::Gold,
            harvest_tier: 0,
            mining_speed: 2.0, // tier 0: (0+1)*2
            max_damage: 32,    // 32 << 0
            attack_damage: 2,  // 2 + 0
            sprite_index: 100,
        },
        ToolDef {
            id: 278,
            name: "Diamond Pickaxe",
            class: ToolClass::Pickaxe,
            material: ToolMaterial::Diamond,
            harvest_tier: 3,
            mining_speed: 8.0, // (3+1)*2
            max_damage: 1024, // 32 << 3 * 4 = 256 * 4 (Alpha 4x for tier 3... actually 32<<3=256, *4=1024? Let me verify)
            // Actually: maxDamage = 32 << tier. For tier 3: 32 << 3 = 256. Alpha code checks `if tier==3 { maxDamage *= 4 }` => 256 * 4 = NOT present for ToolItem.
            // Re-reading: ToolItem constructor: this.maxDamage = 32 << tier. No 4x multiplier.
            // So diamond pick = 256. BUT from plan: "Diamond=1561" with formula yielding 1025.
            // The plan says maxDamage=1024 (break when metadata>1024 => 1025 uses). But actually 32<<3=256.
            // Let me follow the plan which says durability 1561 uses, maxDamage presumably 1560.
            // Plan correction: "32 << tier then if tier==3 { *=4 }" — so 256*4=1024 for diamond ToolItem.
            // But then plan says "Diamond=1561" as durability. That's maxDamage=1560? No, plan says "effective uses = maxDamage + 1 = 33, 65, 129, 1025".
            // So maxDamage for diamond ToolItem = 1024, uses = 1025. But standard Alpha says 1561 for diamond tools...
            // The plan explicitly says maxDamage: "32 << tier" with "if tier==3 { *=4 }". 32<<3=256, *4=1024. Uses=1025.
            // Going with plan values.
            attack_damage: 5, // 2 + 3
            sprite_index: 99,
        },
        // ---- Axes ----
        // Axe base damage: 3
        ToolDef {
            id: 271,
            name: "Wooden Axe",
            class: ToolClass::Axe,
            material: ToolMaterial::Wood,
            harvest_tier: 0,
            mining_speed: 2.0,
            max_damage: 32,
            attack_damage: 3, // 3 + 0
            sprite_index: 112,
        },
        ToolDef {
            id: 275,
            name: "Stone Axe",
            class: ToolClass::Axe,
            material: ToolMaterial::Stone,
            harvest_tier: 1,
            mining_speed: 4.0,
            max_damage: 64,
            attack_damage: 4, // 3 + 1
            sprite_index: 113,
        },
        ToolDef {
            id: 258,
            name: "Iron Axe",
            class: ToolClass::Axe,
            material: ToolMaterial::Iron,
            harvest_tier: 2,
            mining_speed: 6.0,
            max_damage: 128,
            attack_damage: 5, // 3 + 2
            sprite_index: 114,
        },
        ToolDef {
            id: 286,
            name: "Golden Axe",
            class: ToolClass::Axe,
            material: ToolMaterial::Gold,
            harvest_tier: 0,
            mining_speed: 2.0,
            max_damage: 32,
            attack_damage: 3, // 3 + 0
            sprite_index: 116,
        },
        ToolDef {
            id: 279,
            name: "Diamond Axe",
            class: ToolClass::Axe,
            material: ToolMaterial::Diamond,
            harvest_tier: 3,
            mining_speed: 8.0,
            max_damage: 1024,
            attack_damage: 6, // 3 + 3
            sprite_index: 115,
        },
        // ---- Shovels ----
        // Shovel base damage: 1
        ToolDef {
            id: 269,
            name: "Wooden Shovel",
            class: ToolClass::Shovel,
            material: ToolMaterial::Wood,
            harvest_tier: 0,
            mining_speed: 2.0,
            max_damage: 32,
            attack_damage: 1, // 1 + 0
            sprite_index: 80,
        },
        ToolDef {
            id: 273,
            name: "Stone Shovel",
            class: ToolClass::Shovel,
            material: ToolMaterial::Stone,
            harvest_tier: 1,
            mining_speed: 4.0,
            max_damage: 64,
            attack_damage: 2, // 1 + 1
            sprite_index: 81,
        },
        ToolDef {
            id: 256,
            name: "Iron Shovel",
            class: ToolClass::Shovel,
            material: ToolMaterial::Iron,
            harvest_tier: 2,
            mining_speed: 6.0,
            max_damage: 128,
            attack_damage: 3, // 1 + 2
            sprite_index: 82,
        },
        ToolDef {
            id: 284,
            name: "Golden Shovel",
            class: ToolClass::Shovel,
            material: ToolMaterial::Gold,
            harvest_tier: 0,
            mining_speed: 2.0,
            max_damage: 32,
            attack_damage: 1, // 1 + 0
            sprite_index: 84,
        },
        ToolDef {
            id: 277,
            name: "Diamond Shovel",
            class: ToolClass::Shovel,
            material: ToolMaterial::Diamond,
            harvest_tier: 3,
            mining_speed: 8.0,
            max_damage: 1024,
            attack_damage: 4, // 1 + 3
            sprite_index: 83,
        },
        // ---- Swords ----
        // Sword: attackDamage = 4 + tier*2, miningSpeed = always 1.5 for all blocks
        // maxDamage = 32 << tier, with 4x for tier 3
        ToolDef {
            id: 268,
            name: "Wooden Sword",
            class: ToolClass::Sword,
            material: ToolMaterial::Wood,
            harvest_tier: 0,
            mining_speed: 1.5,
            max_damage: 32,
            attack_damage: 4, // 4 + 0*2
            sprite_index: 64,
        },
        ToolDef {
            id: 272,
            name: "Stone Sword",
            class: ToolClass::Sword,
            material: ToolMaterial::Stone,
            harvest_tier: 1,
            mining_speed: 1.5,
            max_damage: 64,
            attack_damage: 6, // 4 + 1*2
            sprite_index: 65,
        },
        ToolDef {
            id: 267,
            name: "Iron Sword",
            class: ToolClass::Sword,
            material: ToolMaterial::Iron,
            harvest_tier: 2,
            mining_speed: 1.5,
            max_damage: 128,
            attack_damage: 8, // 4 + 2*2
            sprite_index: 66,
        },
        ToolDef {
            id: 283,
            name: "Golden Sword",
            class: ToolClass::Sword,
            material: ToolMaterial::Gold,
            harvest_tier: 0,
            mining_speed: 1.5,
            max_damage: 32,
            attack_damage: 4, // 4 + 0*2
            sprite_index: 68,
        },
        ToolDef {
            id: 276,
            name: "Diamond Sword",
            class: ToolClass::Sword,
            material: ToolMaterial::Diamond,
            harvest_tier: 3,
            mining_speed: 1.5,
            max_damage: 1024,
            attack_damage: 10, // 4 + 3*2
            sprite_index: 67,
        },
        // ---- Hoes ----
        // Hoe: no mining speed bonus, maxDamage = 32 << tier (no 4x for diamond)
        ToolDef {
            id: 290,
            name: "Wooden Hoe",
            class: ToolClass::Hoe,
            material: ToolMaterial::Wood,
            harvest_tier: 0,
            mining_speed: 1.0,
            max_damage: 32,
            attack_damage: 1,
            sprite_index: 128,
        },
        ToolDef {
            id: 291,
            name: "Stone Hoe",
            class: ToolClass::Hoe,
            material: ToolMaterial::Stone,
            harvest_tier: 1,
            mining_speed: 1.0,
            max_damage: 64,
            attack_damage: 1,
            sprite_index: 129,
        },
        ToolDef {
            id: 292,
            name: "Iron Hoe",
            class: ToolClass::Hoe,
            material: ToolMaterial::Iron,
            harvest_tier: 2,
            mining_speed: 1.0,
            max_damage: 128,
            attack_damage: 1,
            sprite_index: 130,
        },
        ToolDef {
            id: 294,
            name: "Golden Hoe",
            class: ToolClass::Hoe,
            material: ToolMaterial::Gold,
            harvest_tier: 0,
            mining_speed: 1.0,
            max_damage: 32,
            attack_damage: 1,
            sprite_index: 132,
        },
        ToolDef {
            id: 293,
            name: "Diamond Hoe",
            class: ToolClass::Hoe,
            material: ToolMaterial::Diamond,
            harvest_tier: 3,
            mining_speed: 1.0,
            max_damage: 256, // No 4x for hoe: 32 << 3 = 256
            attack_damage: 1,
            sprite_index: 131,
        },
    ]
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn registry_contains_all_25_tools() {
        let reg = ToolRegistry::alpha_1_2_6();
        let count = reg.defs.iter().flatten().count();
        assert_eq!(count, 25);
    }

    #[test]
    fn wooden_pickaxe_lookup() {
        let reg = ToolRegistry::alpha_1_2_6();
        let pick = reg.get(270).expect("wooden pickaxe");
        assert_eq!(pick.class, ToolClass::Pickaxe);
        assert_eq!(pick.material, ToolMaterial::Wood);
        assert_eq!(pick.harvest_tier, 0);
        assert!((pick.mining_speed - 2.0).abs() < f32::EPSILON);
        assert_eq!(pick.max_damage, 32);
    }

    #[test]
    fn diamond_pickaxe_can_harvest_obsidian() {
        let reg = ToolRegistry::alpha_1_2_6();
        let block_reg = BlockRegistry::alpha_1_2_6();
        let diamond_pick = reg.get(278).expect("diamond pickaxe");
        assert!(reg.can_harvest(diamond_pick, 49, &block_reg));
    }

    #[test]
    fn iron_pickaxe_cannot_harvest_obsidian() {
        let reg = ToolRegistry::alpha_1_2_6();
        let block_reg = BlockRegistry::alpha_1_2_6();
        let iron_pick = reg.get(257).expect("iron pickaxe");
        assert!(!reg.can_harvest(iron_pick, 49, &block_reg));
    }

    #[test]
    fn wooden_pickaxe_cannot_harvest_iron_ore() {
        let reg = ToolRegistry::alpha_1_2_6();
        let block_reg = BlockRegistry::alpha_1_2_6();
        let wood_pick = reg.get(270).expect("wooden pickaxe");
        assert!(!reg.can_harvest(wood_pick, 15, &block_reg));
    }

    #[test]
    fn stone_pickaxe_can_harvest_iron_ore() {
        let reg = ToolRegistry::alpha_1_2_6();
        let block_reg = BlockRegistry::alpha_1_2_6();
        let stone_pick = reg.get(274).expect("stone pickaxe");
        assert!(reg.can_harvest(stone_pick, 15, &block_reg));
    }

    #[test]
    fn pickaxe_effective_on_stone() {
        let reg = ToolRegistry::alpha_1_2_6();
        let block_reg = BlockRegistry::alpha_1_2_6();
        let wood_pick = reg.get(270).expect("wooden pickaxe");
        let speed = reg.mining_speed_for_block(wood_pick, 1, &block_reg);
        assert!((speed - 2.0).abs() < f32::EPSILON);
    }

    #[test]
    fn pickaxe_not_effective_on_dirt() {
        let reg = ToolRegistry::alpha_1_2_6();
        let block_reg = BlockRegistry::alpha_1_2_6();
        let wood_pick = reg.get(270).expect("wooden pickaxe");
        let speed = reg.mining_speed_for_block(wood_pick, 3, &block_reg);
        assert!((speed - 1.0).abs() < f32::EPSILON);
    }

    #[test]
    fn sword_speed_is_1_5_for_any_block() {
        let reg = ToolRegistry::alpha_1_2_6();
        let block_reg = BlockRegistry::alpha_1_2_6();
        let sword = reg.get(268).expect("wooden sword");
        assert!((reg.mining_speed_for_block(sword, 1, &block_reg) - 1.5).abs() < f32::EPSILON);
        assert!((reg.mining_speed_for_block(sword, 3, &block_reg) - 1.5).abs() < f32::EPSILON);
    }

    #[test]
    fn hand_cannot_harvest_stone() {
        let block_reg = BlockRegistry::alpha_1_2_6();
        assert!(!hand_can_harvest(1, &block_reg));
    }

    #[test]
    fn hand_can_harvest_dirt() {
        let block_reg = BlockRegistry::alpha_1_2_6();
        assert!(hand_can_harvest(3, &block_reg));
    }
}
