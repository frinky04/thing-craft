mod app;
mod ecs;
mod lighting;
mod mesh;
mod renderer;
mod streaming;
mod time_step;
mod world;

fn main() -> anyhow::Result<()> {
    app::run()
}
