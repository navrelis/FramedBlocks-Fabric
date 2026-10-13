![FramedBlocks Fabric](https://raw.githubusercontent.com/navrelis/FramedBlocksFabric/1.21.1-fabric/docs/images/banner.png)

**Unofficial port.** This is a community port of [FramedBlocks](https://github.com/XFactHD/FramedBlocks) by XFactHD to Fabric. It is not affiliated with or endorsed by the original author. Report bugs of this port at the [port's issue tracker](https://github.com/navrelis/FramedBlocksFabric/issues), not to upstream.

## What it adds

FramedBlocks adds "framed" versions of many block shapes: slabs, panels, slopes, stairs, corner pieces, pillars, double and checkered variants and more, plus functional blocks such as doors, trapdoors, gates, signs, chests, hoppers, tanks, rails and item frames. The registry holds 236 blocks (about 230 framed shapes).

## Camouflaging

A new framed block is an empty wooden frame. Right-click it with a suitable block item to give it that block's appearance. Fluids can be used too. Blocks with block entities and untagged non-solid blocks are not accepted. A Framed Hammer removes the camo again. Whether the camo item is consumed is a server config option.

## Features

- Tools: Framed Hammer, Wrench, Screwdriver, Key and Blueprint
- Glowstone dust to make a framed block emit light, Framed Reinforcement, Phantom Paste (intangible blocks, server config option)
- Framing Saw and Powered Framing Saw to convert framed blocks between shapes
- Client, server and developer-debug configuration

## Requirements

- Minecraft 1.21.1, Java 21
- Fabric Loader 0.19 or newer, Fabric API
- Required on both client and server. Forge Config API Port is bundled.

## Compatibility

Planned integrations: JEI, REI, EMI, Jade, Athena, Diagonal Blocks, Amendments, Searchables, Additional Placements, AtlasViewer. Their state in this port is not verified one by one.

## Known differences

- Create, Applied Energistics 2 and Building Gadgets integrations are removed (no Fabric 1.21.1 builds exist).
- The mod id is `framedblocks_fabric`: worlds and items from the NeoForge version are not compatible.
- Rendering and item/fluid transfer were rewritten for the Fabric APIs.

## Credits and license

All credit for the concept and original code goes to XFactHD and contributors. Port by navrelis. Licensed under LGPL-3.0; the bundled Forge Config API Port by Fuzs is MPL-2.0.

Source: https://github.com/navrelis/FramedBlocksFabric
