use bevy_ecs::prelude::Resource;

use crate::inventory::{ItemStack, PLAYER_CRAFT_INPUT_SLOT_COUNT};

const EMPTY_SLOT: i32 = -1;

#[derive(Debug, Clone)]
struct CraftingRecipe {
    width: usize,
    height: usize,
    ingredients: Vec<i32>,
    result: ItemStack,
}

impl CraftingRecipe {
    fn matches(&self, inventory: [i32; 9]) -> bool {
        for x in 0..=(3 - self.width) {
            for y in 0..=(3 - self.height) {
                if self.matches_at(inventory, x, y, true) || self.matches_at(inventory, x, y, false)
                {
                    return true;
                }
            }
        }
        false
    }

    fn matches_at(&self, inventory: [i32; 9], x: usize, y: usize, mirrored: bool) -> bool {
        for i in 0..3 {
            for j in 0..3 {
                let local_x = i as i32 - x as i32;
                let local_y = j as i32 - y as i32;
                let mut expected = EMPTY_SLOT;
                if local_x >= 0
                    && local_y >= 0
                    && local_x < self.width as i32
                    && local_y < self.height as i32
                {
                    let local_x = usize::try_from(local_x).expect("bounded");
                    let local_y = usize::try_from(local_y).expect("bounded");
                    let idx = if mirrored {
                        self.width - local_x - 1 + local_y * self.width
                    } else {
                        local_x + local_y * self.width
                    };
                    expected = self.ingredients[idx];
                }
                if inventory[i + j * 3] != expected {
                    return false;
                }
            }
        }
        true
    }

    fn size(&self) -> usize {
        self.width * self.height
    }
}

#[derive(Resource, Debug, Clone)]
pub struct CraftingRegistry {
    recipes: Vec<CraftingRecipe>,
}

impl CraftingRegistry {
    #[must_use]
    pub fn alpha_1_2_6() -> Self {
        let mut builder = CraftingRegistryBuilder::default();

        builder.register_tool_recipes();
        builder.register_weapon_recipes();
        builder.register_mineral_recipes();
        builder.register_food_recipes();
        builder.register_block_recipes();
        builder.register_armor_recipes();

        builder.register_shaped(
            item(339, 3),
            &["###"],
            &[(b'#' as char, 338)], // reeds -> paper
        );
        builder.register_shaped(item(340, 1), &["#", "#", "#"], &[(b'#' as char, 339)]);
        builder.register_shaped(block(85, 2), &["###", "###"], &[(b'#' as char, 280)]);
        builder.register_shaped(
            block(84, 1),
            &["###", "#X#", "###"],
            &[(b'#' as char, 5), (b'X' as char, 264)],
        );
        builder.register_shaped(
            block(47, 1),
            &["###", "XXX", "###"],
            &[(b'#' as char, 5), (b'X' as char, 340)],
        );
        builder.register_shaped(block(80, 1), &["##", "##"], &[(b'#' as char, 332)]);
        builder.register_shaped(block(82, 1), &["##", "##"], &[(b'#' as char, 337)]);
        builder.register_shaped(block(45, 1), &["##", "##"], &[(b'#' as char, 336)]);
        builder.register_shaped(block(89, 1), &["###", "###", "###"], &[(b'#' as char, 348)]);
        builder.register_shaped(block(35, 1), &["###", "###", "###"], &[(b'#' as char, 287)]);
        builder.register_shaped(
            block(46, 1),
            &["X#X", "#X#", "X#X"],
            &[(b'X' as char, 289), (b'#' as char, 12)],
        );
        builder.register_shaped(block(44, 3), &["###"], &[(b'#' as char, 4)]);
        builder.register_shaped(block(65, 1), &["# #", "###", "# #"], &[(b'#' as char, 280)]);
        builder.register_shaped(item(324, 1), &["##", "##", "##"], &[(b'#' as char, 5)]);
        builder.register_shaped(item(330, 1), &["##", "##", "##"], &[(b'#' as char, 265)]);
        builder.register_shaped(
            item(323, 1),
            &["###", "###", " X "],
            &[(b'#' as char, 5), (b'X' as char, 280)],
        );
        builder.register_shaped(block(5, 4), &["#"], &[(b'#' as char, 17)]);
        builder.register_shaped(item(280, 4), &["#", "#"], &[(b'#' as char, 5)]);
        builder.register_shaped(
            block(50, 4),
            &["X", "#"],
            &[(b'X' as char, 263), (b'#' as char, 280)],
        );
        builder.register_shaped(item(281, 4), &["# #", " # "], &[(b'#' as char, 5)]);
        builder.register_shaped(
            block(66, 16),
            &["X X", "X#X", "X X"],
            &[(b'X' as char, 265), (b'#' as char, 280)],
        );
        builder.register_shaped(item(328, 1), &["# #", "###"], &[(b'#' as char, 265)]);
        builder.register_shaped(
            block(91, 1),
            &["A", "B"],
            &[(b'A' as char, 86), (b'B' as char, 50)],
        );
        builder.register_shaped(
            item(342, 1),
            &["A", "B"],
            &[(b'A' as char, 54), (b'B' as char, 328)],
        );
        builder.register_shaped(
            item(343, 1),
            &["A", "B"],
            &[(b'A' as char, 61), (b'B' as char, 328)],
        );
        builder.register_shaped(item(333, 1), &["# #", "###"], &[(b'#' as char, 5)]);
        builder.register_shaped(item(325, 1), &["# #", " # "], &[(b'#' as char, 265)]);
        builder.register_shaped(
            item(259, 1),
            &["A ", " B"],
            &[(b'A' as char, 265), (b'B' as char, 318)],
        );
        builder.register_shaped(item(297, 1), &["###"], &[(b'#' as char, 296)]);
        builder.register_shaped(block(53, 4), &["#  ", "## ", "###"], &[(b'#' as char, 5)]);
        builder.register_shaped(
            item(346, 1),
            &["  #", " #X", "# X"],
            &[(b'#' as char, 280), (b'X' as char, 287)],
        );
        builder.register_shaped(block(67, 4), &["#  ", "## ", "###"], &[(b'#' as char, 4)]);
        builder.register_shaped(
            item(321, 1),
            &["###", "#X#", "###"],
            &[(b'#' as char, 280), (b'X' as char, 35)],
        );
        builder.register_shaped(
            item(322, 1),
            &["###", "#X#", "###"],
            &[(b'#' as char, 41), (b'X' as char, 260)],
        );
        builder.register_shaped(
            block(69, 1),
            &["X", "#"],
            &[(b'X' as char, 280), (b'#' as char, 4)],
        );
        builder.register_shaped(
            block(76, 1),
            &["X", "#"],
            &[(b'X' as char, 331), (b'#' as char, 280)],
        );
        builder.register_shaped(
            item(347, 1),
            &[" # ", "#X#", " # "],
            &[(b'#' as char, 266), (b'X' as char, 331)],
        );
        builder.register_shaped(
            item(345, 1),
            &[" # ", "#X#", " # "],
            &[(b'#' as char, 265), (b'X' as char, 331)],
        );
        builder.register_shaped(block(77, 1), &["#", "#"], &[(b'#' as char, 1)]);
        builder.register_shaped(block(70, 1), &["###"], &[(b'#' as char, 1)]);
        builder.register_shaped(block(72, 1), &["###"], &[(b'#' as char, 5)]);

        builder.finalize()
    }

    #[must_use]
    pub fn player_result(
        &self,
        slots: [Option<ItemStack>; PLAYER_CRAFT_INPUT_SLOT_COUNT],
    ) -> Option<ItemStack> {
        let mut grid = [EMPTY_SLOT; 9];
        for y in 0..2 {
            for x in 0..2 {
                let slot_index = x + y * 2;
                let alpha_id =
                    slots[slot_index].map_or(EMPTY_SLOT, |stack| stack.alpha_item_id() as i32);
                grid[x + y * 3] = alpha_id;
            }
        }
        self.result_for_grid(grid)
    }

    #[must_use]
    pub fn result_for_grid(&self, grid: [i32; 9]) -> Option<ItemStack> {
        for recipe in &self.recipes {
            if recipe.matches(grid) {
                return Some(recipe.result);
            }
        }
        None
    }
}

#[derive(Default)]
struct CraftingRegistryBuilder {
    recipes: Vec<CraftingRecipe>,
}

impl CraftingRegistryBuilder {
    fn finalize(mut self) -> CraftingRegistry {
        self.recipes.sort_by(|a, b| b.size().cmp(&a.size()));
        CraftingRegistry {
            recipes: self.recipes,
        }
    }

    fn register_shaped(&mut self, result: ItemStack, pattern: &[&str], keys: &[(char, u16)]) {
        let height = pattern.len();
        let width = pattern.first().map_or(0, |row| row.len());
        let mut ingredients = vec![EMPTY_SLOT; width * height];
        for (y, row) in pattern.iter().enumerate() {
            for (x, c) in row.chars().enumerate() {
                let id = keys
                    .iter()
                    .find_map(|(key, value)| (*key == c).then_some(*value as i32))
                    .unwrap_or(EMPTY_SLOT);
                ingredients[x + y * width] = id;
            }
        }
        self.recipes.push(CraftingRecipe {
            width,
            height,
            ingredients,
            result,
        });
    }

    fn register_tool_recipes(&mut self) {
        let patterns = [
            &["XXX", " # ", " # "][..],
            &["X", "#", "#"][..],
            &["XX", "X#", " #"][..],
            &["XX", " #", " #"][..],
        ];
        let materials = [5_u16, 4, 265, 264, 266];
        let outputs = [
            [270_u16, 274, 257, 278, 285],
            [269_u16, 273, 256, 277, 284],
            [271_u16, 275, 258, 279, 286],
            [290_u16, 291, 292, 293, 294],
        ];
        for (material_index, material) in materials.iter().enumerate() {
            for row in 0..outputs.len() {
                self.register_shaped(
                    alpha_item(outputs[row][material_index], 1),
                    patterns[row],
                    &[(b'#' as char, 280), (b'X' as char, *material)],
                );
            }
        }
    }

    fn register_weapon_recipes(&mut self) {
        let materials = [5_u16, 4, 265, 264, 266];
        let swords = [268_u16, 272, 267, 276, 283];
        for (material_index, material) in materials.iter().enumerate() {
            self.register_shaped(
                alpha_item(swords[material_index], 1),
                &["X", "X", "#"],
                &[(b'#' as char, 280), (b'X' as char, *material)],
            );
        }
        self.register_shaped(
            item(261, 1),
            &[" #X", "# X", " #X"],
            &[(b'X' as char, 287), (b'#' as char, 280)],
        );
        self.register_shaped(
            item(262, 4),
            &["X", "#", "Y"],
            &[
                (b'X' as char, 318),
                (b'#' as char, 280),
                (b'Y' as char, 288),
            ],
        );
    }

    fn register_mineral_recipes(&mut self) {
        let pairs = [(41_u16, 266_u16), (42_u16, 265_u16), (57_u16, 264_u16)];
        for (block_id, item_id) in pairs {
            self.register_shaped(
                block(block_id as u8, 1),
                &["###", "###", "###"],
                &[(b'#' as char, item_id)],
            );
            self.register_shaped(item(item_id, 9), &["#"], &[(b'#' as char, block_id)]);
        }
    }

    fn register_food_recipes(&mut self) {
        self.register_shaped(
            item(282, 1),
            &["Y", "X", "#"],
            &[(b'X' as char, 39), (b'Y' as char, 40), (b'#' as char, 281)],
        );
        self.register_shaped(
            item(282, 1),
            &["Y", "X", "#"],
            &[(b'X' as char, 40), (b'Y' as char, 39), (b'#' as char, 281)],
        );
    }

    fn register_block_recipes(&mut self) {
        self.register_shaped(block(54, 1), &["###", "# #", "###"], &[(b'#' as char, 5)]);
        self.register_shaped(block(61, 1), &["###", "# #", "###"], &[(b'#' as char, 4)]);
        self.register_shaped(block(58, 1), &["##", "##"], &[(b'#' as char, 5)]);
    }

    fn register_armor_recipes(&mut self) {
        let patterns = [
            &["XXX", "X X"][..],
            &["X X", "XXX", "XXX"][..],
            &["XXX", "X X", "X X"][..],
            &["X X", "X X"][..],
        ];
        let materials = [334_u16, 51_u16, 265_u16, 264_u16, 266_u16];
        let outputs = [
            [298_u16, 302, 306, 310, 314],
            [299_u16, 303, 307, 311, 315],
            [300_u16, 304, 308, 312, 316],
            [301_u16, 305, 309, 313, 317],
        ];
        for (material_index, material) in materials.iter().enumerate() {
            for row in 0..outputs.len() {
                self.register_shaped(
                    item(outputs[row][material_index], 1),
                    patterns[row],
                    &[(b'X' as char, *material)],
                );
            }
        }
    }
}

fn block(block_id: u8, count: u8) -> ItemStack {
    ItemStack::block(block_id, count)
}

fn item(item_id: u16, count: u8) -> ItemStack {
    ItemStack::item(item_id, count)
}

fn alpha_item(id: u16, count: u8) -> ItemStack {
    if is_tool_id(id) {
        ItemStack::tool(id)
    } else if id <= u16::from(u8::MAX) {
        ItemStack::block(id as u8, count)
    } else {
        ItemStack::item(id, count)
    }
}

fn is_tool_id(id: u16) -> bool {
    matches!(
        id,
        256 | 257
            | 258
            | 267
            | 268
            | 269
            | 270
            | 271
            | 272
            | 273
            | 274
            | 275
            | 276
            | 277
            | 278
            | 279
            | 283
            | 284
            | 285
            | 286
            | 290
            | 291
            | 292
            | 293
            | 294
    )
}

#[cfg(test)]
mod tests {
    use crate::inventory::ItemStack;

    use super::CraftingRegistry;

    #[test]
    fn player_two_by_two_maps_into_alpha_three_by_three() {
        let registry = CraftingRegistry::alpha_1_2_6();
        let mut grid = [None; 4];
        grid[0] = Some(ItemStack::block(17, 1));
        assert_eq!(registry.player_result(grid), Some(ItemStack::block(5, 4)));
    }

    #[test]
    fn mirrored_recipe_matches_like_alpha() {
        let registry = CraftingRegistry::alpha_1_2_6();
        let mut grid = [-1; 9];
        // Mirrored wooden axe in a 3x3 grid.
        grid[0] = 5;
        grid[1] = 5;
        grid[3] = 280;
        grid[4] = 5;
        grid[6] = 280;
        assert_eq!(registry.result_for_grid(grid), Some(ItemStack::tool(271)));
    }
}
