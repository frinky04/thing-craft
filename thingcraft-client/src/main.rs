mod app;
mod crafting;
mod ecs;
mod entity;
mod gameplay;
mod hud;
mod inventory;
mod lighting;
mod mesh;
#[allow(dead_code)]
mod noise;
mod renderer;
mod streaming;
mod time_step;
mod tool;
mod world;

fn main() -> anyhow::Result<()> {
    app::run()
}
