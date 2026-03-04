mod app;
mod ecs;
mod entity;
mod hud;
mod inventory;
mod lighting;
mod mesh;
#[allow(dead_code)]
mod noise;
mod renderer;
mod streaming;
mod time_step;
mod world;

fn main() -> anyhow::Result<()> {
    app::run()
}
