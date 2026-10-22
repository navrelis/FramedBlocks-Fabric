![FramedBlocks Fabric](https://raw.githubusercontent.com/navrelis/FramedBlocks-Fabric/1.21.1-fabric/docs/images/banner.png)

**Unofficial port.** This is a community port of [FramedBlocks](https://github.com/XFactHD/FramedBlocks) by XFactHD to Fabric. It is not affiliated with or endorsed by the original author. Report bugs of this port at the [port's issue tracker](https://github.com/navrelis/FramedBlocks-Fabric/issues), not to upstream.

## What it adds

FramedBlocks adds "framed" versions of many block shapes: slabs, panels, slopes, stairs, corner pieces, pillars, double and checkered variants and more, plus functional blocks such as doors, trapdoors, gates, signs, chests, hoppers, tanks, rails and item frames. The registry holds 269 blocks (over 230 framed shapes, 232 of them with an item of their own).

## Fixed in 1.1.2

Placing or using framed blocks no longer crashes the game with NeoForge-on-Fabric layers such as Sporran (AbstractMethodError caused by conflicting default methods). All block hooks that NeoForge also defines now have concrete implementations.

## Fixed in 1.1.1

Framed blocks and items no longer crash the game in modpacks. Three crashes are fixed: Sodium 0.8 together with a mod that wraps block models (such as Continuity), Continuity on its own (a framed block without camo could recurse on its own model), and mods that add NeoForge block methods to vanilla blocks (NeoForge-on-Fabric layers such as Sporran). If another mod's model throws while its quads are read, the mod now logs one warning and uses the plain quads instead of crashing.

## New in 1.1.0

28 new framed shapes. 12 are backported from newer upstream FramedBlocks versions: slope edge slab, slope edge panel, small and large prism corner slope panels (outer and inner), half board, divided board, corner board, inner corner board, double corner board and the framed banner (placed on a wall it becomes the wall banner). 16 are new in this port: arch, inner arch, crown molding with inner and outer corner, octagonal pillar, window frame, table, chair, stool, diagonal brace, corbel, wall shelf, barrel (27 slots), crafting table and redstone lamp (emits light level 15 when powered).

## Camouflaging

A new framed block is an empty wooden frame. Right-click it with a suitable block item to give it that block's appearance. Fluids can be used too. Blocks with block entities and untagged non-solid blocks are not accepted. A Framed Hammer removes the camo again. Whether the camo item is consumed is a server config option.

## Features

- Tools: Framed Hammer, Wrench, Screwdriver, Key and Blueprint
- Glowstone dust to make a framed block emit light, Framed Reinforcement, Phantom Paste (intangible blocks, server config option)
- Framing Saw and Powered Framing Saw to convert framed blocks between shapes. The Powered Framing Saw works without energy by default; with Team Reborn Energy installed it accepts energy from other mods (never from the top, never output), and the server config option `poweredSawRequiresEnergy` can make energy mandatory.
- Client, server and developer-debug configuration

## Requirements

- Minecraft 1.21.1, Java 21
- Fabric Loader 0.19 or newer, Fabric API
- Required on both client and server. Forge Config API Port is bundled.
- Optional: Team Reborn Energy (not bundled)

## Compatibility

Optional integrations, only active if the other mod is installed: JEI, REI, EMI, Jade, Diagonal Blocks, Amendments, Searchables, Additional Placements, AtlasViewer, Team Reborn Energy. All compile against the respective mod's API for Minecraft 1.21.1 Fabric. In a world, JEI, REI, EMI and Jade load the FramedBlocks plugin (automated client test); the others have not been tested in game yet. Please report problems on the issue tracker.

## Known differences

- Create, Applied Energistics 2 and Building Gadgets integrations are removed (no Fabric 1.21.1 builds exist).
- The mod id is `framedblocks_fabric`: worlds and items from the NeoForge version are not compatible.
- Rendering and item/fluid transfer were rewritten for the Fabric APIs.
- The Athena integration is removed: Fabric Athena computes connected textures while emitting quads, so there is nothing to forward. Camos with connected textures depend on the general model capture instead.

## Credits and license

All credit for the concept and original code goes to XFactHD and contributors. Port by navrelis. Licensed under LGPL-3.0; the bundled Forge Config API Port by Fuzs is MPL-2.0.

Source: https://github.com/navrelis/FramedBlocks-Fabric
