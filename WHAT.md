# ThingCraft
A Rust wgpu recreation of Minecraft Alpha (Alpha 1.2.6).

- **High performance, within reason:** Keeping the code easy, minimal, and understandable is the highest priority. Take performance wins where we can get them.
- **Modern architecture:** Use hindsight to inform our design decisions. Minecraft is one of the most "understood games", we can use that to our advantage.
- **Progressive Rebuild:** Start with the most important elements, and progressively build up. Leave complicated thing like Networking till later. HOWEVER*
- **Build Smart:** Just because we're leaving networking till later, doesn't mean it should not be considered early. Build with future intention.
- **No Slop, More Docs:** Keep docs of everything. Make sure other developers are informed of pitfalls and specific design decisions. Keep ROADMAP.md up-to-date!
- **Have Fun:** This project is a pet project! So have fun making it.
- **Resources:** Check the resources folder for anything you need. The original textures, resource pack, and 498 decompiled Java files with human-readable names are now in resources/decomp/src/. 
- **Source Code Note:** Steal the numbers, not the structure. Use the decompiled source to verify exact values and behaviors, then reimplement them using our modern ECS architecture and data-driven design.

# Preferences

- **Use bevy_ecs (standalone, without the full Bevy engine)**
Entities are just IDs. Components are plain data structs. Systems are functions.
- **Strictly Separate Tick Rate from Frame Rate**
While early Minecraft actually did use a 20 TPS fixed-timestep loop, it forced heavy lifting (chunk generation, meshing, and lighting) onto the main thread, causing tick lag to completely stall the render loop. Avoid this trap. Design your architecture so the render loop runs completely independently of the game logic tick, using state interpolation for smooth visuals even if the game logic stutters.
- **Block Data Design**
Notch used block ID + 4-bit metadata, which became a nightmare. Use a block registry. Data-drive everything. Don't hardcode `if block == STONE` scattered everywhere.
- **Threading**
Minecraft was almost entirely single-threaded. However, there is plenty of opportunities for concurrency with Rust. Be smart about it.
- **Beware the Floating Point Origin**
Minecraft originally used 32-bit floats for player positions and world coordinates. As players traveled far from 0, 0 (the "Far Lands" effect), floating-point precision loss caused physics collision and rendering math to break down.
The Rust Fix: Store absolute world coordinates using 64-bit floats (f64) for entities, or strictly separate coordinates into ChunkPos (a pair of integers) and LocalPos (a float representing position within that specific chunk). You can cast to f32 in the GPU shader relative to the camera position to maintain visual precision.
- **Multithread the Lighting Engine**
In early Minecraft, breaking a block that let sunlight into a dark cave triggered a recursive lighting flood-fill algorithm on the main thread. This caused massive stuttering.
The Rust Fix: Treat lighting as a queue-based cellular automaton. When a block changes, push its coordinates to a lighting update queue. Process this queue asynchronously on a background thread, then dispatch the updated chunk data back to the mesher. Just be aware of a common Rust/ECS pitfall here: if your background thread needs to read the block data to calculate light propagation, it might lock the ECS storage, causing the main thread to stall anyway. You will likely want to give your lighting thread read-only access to a secondary chunk data structure (like an `Arc<RwLock<...>>` or a double-buffered chunk map) rather than making it query the ECS directly for every single block's opacity.
- **A Heads-Up on ECS and Networking**
Since we are using bevy_ecs, a great way to "build smart" for future networking is to strictly separate your inputs/commands from your state execution. If a player clicks to place a block, don't have the input system place the block directly. Have it emit a PlaceBlockRequest event. Your game logic then processes that event. When you add multiplayer later, an incoming network packet just emits that exact same PlaceBlockRequest event.

# Checklist
Check and update both completion docs:
- `BOOSTRAP_COMPLETION.md` for implementation/bootstrap milestones.
- `GAMEPLAY_COMPLETION.md` for Alpha 1.2.6 gameplay parity.

# Coordinate + Angle Conventions (Read Before Porting Alpha Math)

- **World axes:** `+Y` is up. Horizontal plane is `X/Z`.
- **Player transform origin:** `Transform64.position` is **feet-space** while physics is active.
- **Camera origin:** `camera_y = feet_y + eye_height - sneak_offset`.
- **Yaw/Pitch units:** radians.
- **Yaw positive direction:** positive yaw rotates view toward `+X`.
- **Pitch positive direction:** positive pitch looks up (`+Y` component increases).

Forward/look direction used across gameplay code:

```rust
x = sin(yaw) * cos(pitch);
y = sin(pitch);
z = cos(yaw) * cos(pitch);
```

This is the canonical direction mapping and should be reused for:
- raycasts
- first-person interaction traces
- throw/projectile/drop impulses

Porting checklist for Alpha formulas:
- **Do not copy signs blindly** from decompiled code. First map Alpha's local convention to ThingCraft's canonical direction mapping above.
- If target code uses player position in feet-space, convert eye-space constants (`offsetY`, eye-relative effects) explicitly.
- When matching visual effects (pickup, hand/item transforms), verify whether Alpha behavior is physics-driven vs render-only before adding gameplay forces.
- Add a narrow regression test for each imported formula (at minimum: yaw sign and pitch sign expectations).
