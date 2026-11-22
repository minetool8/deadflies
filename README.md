# Dead Flies

Minecraft **1.20.1 / Forge 47+** client-side mod. Tiny fly **particles** (2 px long, 1 px high, no entities)
buzz around dead bodies from **Doomsday Decoration** (`doomsday_decoration`), with a looping positional buzzing sound.

- 10-20 flies per body (configurable, fixed per body position)
- Flies fly curved loops, fast darts, hover, land on the body, crawl and take off again
- Flies ignore collisions (they may clip through walls)
- Works with servers that don't have the mod installed

## Build (GitHub)
Push to GitHub. The workflow in `.github/workflows/build.yml` builds the jar on every push;
download it from **Actions -> latest run -> Artifacts -> deadflies-jar**.
Push a tag like `v1.0.0` to also publish the jar as a GitHub Release.

## Local build
Needs JDK 17 and Gradle 8.1.1:

    gradle build

Jar ends up in `build/libs/`.

## Finding the right body blocks
Doomsday Decoration has ~1100 blocks. Bodies are detected by id keywords (see
`config/deadflies-client.toml`). In game:

    /deadflies matches        list blocks currently treated as bodies
    /deadflies find <text>    search Doomsday Decoration block ids
    /deadflies test           spawn a test swarm at your feet (no body needed)
    /deadflies cleartest      remove test swarms
    /deadflies status         active swarms / flies

Add exact ids to `extraBlocks` in the config (or to the `deadflies:bodies` block tag with a datapack).
