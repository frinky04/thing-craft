# Rules

- **High performance, within reason:** Keeping the code easy, minimal, and understandable is the highest priority. Take performance wins where we can get them.
- **Modern architecture:** Use hindsight to inform our design decisions. Minecraft is one of the most "understood games", we can use that to our advantage.
- **Progressive Rebuild:** Start with the most important elements, and progressively build up. Leave complicated thing like Networking till later. HOWEVER:
- **Build Smart:** Just because we're leaving networking till later, doesn't mean it should not be considered early. Build with future intention.
- **No Slop, More Docs:** Keep docs of everything. Make sure other developers are informed of pitfalls and specific design decisions. Keep ROADMAP.md up-to-date!
- **Resources:** Check the resources folder for anything you need. The original textures, resource pack, and 498 decompiled Java files with human-readable names are now in resources/decomp/src/. 
- **Source Code Note:** Steal the numbers, not the structure. Use the decompiled source to verify exact values and behaviors, then reimplement them using our modern ECS architecture and data-driven design.
- **Avoid God Classes:** Minecraft Java relied heavily on singletons and "God classes" like World.java, which ended up being thousands of lines long and responsible for spawning particles, playing sounds, saving to disk, and simulating physics. Use an Entity Component System (ECS) like hecs or flecs. World should just be a container for components.
- **What to Read:** WHAT.md is our north-star! Read it if you are lost.
- **Completion Docs Split:** Keep both `BOOSTRAP_COMPLETION.md` (bootstrap/implementation) and `GAMEPLAY_COMPLETION.md` (Alpha gameplay parity) updated.