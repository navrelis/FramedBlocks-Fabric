<p align="center">
  <img src="docs/images/banner.png" alt="FramedBlocks Fabric (Unofficial)" width="100%">
</p>

# FramedBlocks Fabric (Unofficial)

> **This is an unofficial, community port.** It is not affiliated with and not endorsed by the original author, XFactHD. Please report problems with this port in [this repository](https://github.com/navrelis/FramedBlocks-Fabric/issues), not to the upstream project.

> **Status: work in progress.** The Minecraft 1.20.1 port is being set up. The content described below is that of the original mod version this port is based on and is not yet verified in this port.

FramedBlocks Fabric is a port of [FramedBlocks](https://github.com/XFactHD/FramedBlocks) by XFactHD (upstream version 9.4.3, Minecraft Forge 1.20.1) to the Fabric mod loader for Minecraft 1.20.1. FramedBlocks adds "framed" versions of many block shapes. A framed block starts out as an empty wooden frame and can be given the appearance of almost any other block, which makes it useful for building with shapes that vanilla Minecraft does not offer in every material.

## Features

The following is taken from the original mod version this port is based on.

- **Around 200 framed block shapes**, among them cubes, slabs, panels, slopes, stairs, corner and prism pieces, pillars, slope edges, pyramids, checkered variants, collapsible blocks and double blocks.
- **Functional blocks:** doors, trapdoors, gates, fences, walls, ladders, levers, buttons, pressure plates, signs and hanging signs, torches, flower pots, item frames, bookshelves, chests, secret storage, bars, panes, rails and rail slopes.
- **Camouflage ("camo").** Right-click a framed block with a suitable block item to apply that block's appearance. Fluids can also be applied as a camo. A Framed Hammer removes the camo again. Whether the camo item is consumed is a server config option.
- **Tools:** Framed Hammer, Framed Wrench, Framed Screwdriver, Framed Key and Framed Blueprint.
- **Block upgrades:** glowstone dust makes a framed block emit light, Framed Reinforcement reinforces it, and an intangibility marker item makes it passable (server config option).
- **Framing Saw:** converts framed blocks into other framed shapes using a material-value system, with a searchable recipe screen. A Powered Framing Saw is included.
- **Configuration** for client rendering, overlays, server rules and developer debugging, using the bundled Forge Config API Port.

## Requirements

- Minecraft 1.20.1
- Java 17
- [Fabric Loader](https://fabricmc.net/use/) 0.19.5 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.92.12 or newer for 1.20.1
- Forge Config API Port is bundled with the mod and does not need to be installed separately.
- Optional: Team Reborn Energy (mod id `team_reborn_energy`, the common energy API on Fabric) for energy input of the Powered Framing Saw. It is not bundled.
- Optional: [Indium](https://modrinth.com/mod/indium) when using Sodium. Sodium 0.5.x for 1.20.1 does not implement the Fabric Rendering API itself, so framed blocks need Indium to render correctly with it.

The mod has to be installed on both the client and the server.

## Installation

1. Install Fabric Loader for Minecraft 1.20.1 and Fabric API.
2. Download the mod jar and place it in the `mods` folder of the client and, for multiplayer, of the server.
3. Start the game.

## Compatibility

Integrations with the following mods are optional and only active if the respective mod is installed:

JEI, REI, EMI, Jade, Supplementaries, Diagonal Fences, Diagonal Windows, Team Reborn Energy.

Not included: the integrations with Building Gadgets, ConTex, CTM, NoCubes, XyCraft and Starlight were removed from the port. The Create integration is not part of the port yet.

## Differences from the Forge original

- Runs on Fabric Loader and Fabric API instead of Forge.
- The mod id is `framedblocks_fabric` instead of `framedblocks`. **Worlds and items from the Forge version are not compatible** because block and item ids differ.
- Rendering was rebuilt on the Fabric Rendering API.
- Forge capabilities (item and fluid handling) were replaced by the Fabric Transfer API.
- The Powered Framing Saw uses Team Reborn Energy instead of Forge FE energy.
- New pixel-art logo.

## Building from source

Requires JDK 17 or newer to start the Gradle wrapper. The Gradle daemon needs JDK 25 (Fabric Loom 1.18), which is provisioned automatically through the Daemon JVM criteria in `gradle/gradle-daemon-jvm.properties`, and the mod itself is compiled for Java 17 through a Java toolchain.

```
./gradlew build
```

On Windows use `gradlew.bat build`. The mod jar is written to `build/libs`. The logo images in `docs/images` can be regenerated with `node tools/logo/generate.mjs`. The resource patch script `tools/port/patch-resources.mjs` applied the namespace and tag changes to the data and asset files and can be re-run safely.

## License and credits

- FramedBlocks is Copyright (C) XFactHD and contributors, licensed under the GNU Lesser General Public License v3.0 ([`LICENSE`](LICENSE), [`LICENSE.GPL`](LICENSE.GPL)).
- The Fabric port is Copyright (C) 2026 navrelis, licensed under the same license.
- The bundled Forge Config API Port by Fuzs is licensed under the MPL-2.0.
- See [`NOTICE.md`](NOTICE.md) for details about the modifications.

Original mod: https://github.com/XFactHD/FramedBlocks. Credit for the concept, the content and the original code goes to XFactHD and the upstream contributors.

## Reporting issues

Report bugs of this port at https://github.com/navrelis/FramedBlocks-Fabric/issues. Please include the Minecraft, Fabric Loader, Fabric API and mod versions, the log file and, for rendering issues, the list of other installed mods. Do not report issues of this port to the upstream project.
