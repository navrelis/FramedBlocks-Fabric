<p align="center">
  <img src="docs/images/banner.png" alt="FramedBlocks Fabric (Unofficial)" width="100%">
</p>

# FramedBlocks Fabric (Unofficial)

> **This is an unofficial, community port.** It is not affiliated with and not endorsed by the original author, XFactHD. Please report problems with this port in [this repository](https://github.com/navrelis/FramedBlocksFabric/issues), not to the upstream project.

FramedBlocks Fabric is a port of [FramedBlocks](https://github.com/XFactHD/FramedBlocks) by XFactHD (upstream version 10.5.3, NeoForge) to the Fabric mod loader for Minecraft 1.21.1. FramedBlocks adds "framed" versions of many block shapes. A framed block starts out as an empty wooden frame and can be given the appearance of almost any other block, which makes it useful for building with shapes that vanilla Minecraft does not offer in every material.

## Features

The following is taken from the registered content and language file of the original mod version this port is based on.

- **About 230 framed block shapes.** The block registry contains 236 blocks: 231 framed shapes that have an item, three wall-mounted torch variants that share the item of their floor torch, the Framing Saw and the Powered Framing Saw. Shapes include cubes, slabs, panels, slopes, stairs, corner and prism pieces, pillars, slope edges, slope panel/slab corners, pyramids, checkered and layered variants, collapsible blocks and double blocks.
- **Functional blocks:** doors, iron doors, trapdoors, gates, fences, walls, ladders, levers, buttons, pressure plates, signs and hanging signs, torches, lanterns, flower pots, item frames, bookshelves, chests, hoppers, secret storage, tanks, bars, panes, a lightning rod, a target block, a bouncy cube, a redstone block, rails and rail slopes.
- **Camouflage ("camo").** Right-click a framed block with a suitable block item to apply that block's appearance (the framed block also takes over the sound of the camo block). Fluids can also be applied as a camo. Blocks with block entities and untagged non-solid blocks are rejected. A Framed Hammer removes the camo again. Whether the camo item is consumed is a server config option.
- **Tools:** Framed Hammer (toggle per-block options such as waterlogging, and remove camo), Framed Wrench (rotate blocks and switch some shape variants), Framed Screwdriver (rotate the camo texture), Framed Key (lock the state of a block), Framed Blueprint (copy a framed block including its camo; the required materials must be in the inventory).
- **Block upgrades:** glowstone dust makes a framed block emit light, Framed Reinforcement reinforces it (removed again with a pickaxe that can mine obsidian), Phantom Paste makes it intangible (server config option).
- **Framing Saw:** converts framed blocks into other framed shapes using a material-value system, with a searchable recipe screen. A Powered Framing Saw is included; the original powers it with FE energy, and how the port handles this has not been verified.
- **Configuration** for client rendering, overlays, server rules and developer debugging, using the bundled Forge Config API Port.

## Requirements

- Minecraft 1.21.1
- Java 21
- [Fabric Loader](https://fabricmc.net/use/) 0.19 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Forge Config API Port is bundled with the mod and does not need to be installed separately.

The mod has to be installed on both the client and the server.

## Installation

1. Install Fabric Loader for Minecraft 1.21.1 and Fabric API.
2. Download the mod jar and place it in the `mods` folder of the client and, for multiplayer, of the server.
3. Start the game.

## Compatibility

Integrations planned in this port, if the respective mod is installed:

JEI, REI, EMI, Jade, Athena, Diagonal Blocks, Amendments, Searchables, Additional Placements, AtlasViewer.

The integrations were carried over from the original mod. Their status in this port has not been verified one by one; see the issue tracker for reports.

Not included, because no Fabric build for Minecraft 1.21.1 of these mods exists: Create, Applied Energistics 2, Building Gadgets. Their integrations were removed from the port.

## Differences from the NeoForge original

- Runs on Fabric Loader and Fabric API instead of NeoForge.
- The mod id is `framedblocks_fabric` instead of `framedblocks`. **Worlds and items from the NeoForge version are not compatible** because block and item ids differ.
- Rendering was rebuilt on the Fabric Rendering API.
- NeoForge capabilities (item and fluid handling) were replaced by the Fabric Transfer API.
- Integrations with Create, Applied Energistics 2 and Building Gadgets were removed, including the AE2 Framing Saw Pattern item.
- New pixel-art logo.

## Building from source

Requires JDK 21.

```
./gradlew build
```

On Windows use `gradlew.bat build`. The mod jar is written to `build/libs`. The logo images in `docs/images` can be regenerated with `node tools/logo/generate.mjs`.

## License and credits

- FramedBlocks is Copyright (C) XFactHD and contributors, licensed under the GNU Lesser General Public License v3.0 ([`LICENSE`](LICENSE), [`LICENSE.GPL`](LICENSE.GPL)).
- The Fabric port is Copyright (C) 2026 navrelis, licensed under the same license.
- The bundled Forge Config API Port by Fuzs is licensed under the MPL-2.0.
- See [`NOTICE.md`](NOTICE.md) for details about the modifications.

Original mod: https://github.com/XFactHD/FramedBlocks. Credit for the concept, the content and the original code goes to XFactHD and the upstream contributors.

## Reporting issues

Report bugs of this port at https://github.com/navrelis/FramedBlocksFabric/issues. Please include the Minecraft, Fabric Loader, Fabric API and mod versions, the log file and, for rendering issues, the list of other installed mods. Do not report issues of this port to the upstream project.
