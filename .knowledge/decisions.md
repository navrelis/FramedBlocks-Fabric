# Decisions

- Remotes: `upstream` = XFactHD/FramedBlocks, `origin` = navrelis/FramedBlocksFabric (created private; user can make it public). Reason: user approved repo creation, visibility was not specified, private is reversible.
- License stays LGPL v3 (upstream license; cannot be relicensed to plain GPL without reason, and user asked to keep it). Ship both LGPL v3 and the GPL v3 text it references.
- Working tree was empty at session start; restored from HEAD with `git restore`. Nothing overwritten.
- Mod id: `framedblocks_fabric` (user asked for a new id). Java package stays `xfacthd.framedblocks` to keep upstream headers and mergeability. Saved worlds from the NeoForge mod are not compatible.
- Toolchain: Loom 1.15.5 on the existing Gradle 9.2.1 wrapper and JDK 21 (newer Loom needs newer Gradle / possibly Java 25).
- Config: Forge Config API Port 21.1.6, bundled jar-in-jar (MPL-2.0), keeps `ModConfigSpec` code unchanged.
- No Porting Lib: 1.21.1 builds are unreleased betas; own small platform layer instead.
- Rendering: Fabric Rendering API (Sodium 0.6+ implements it natively, no Indium).
- Removed integrations (no Fabric 1.21.1 build): Create, Applied Energistics 2, Building Gadgets.
- Datagen: the committed generated resources are kept and patched; the NeoForge datagen DSL is not ported unless something needs regenerating.
- Precision override B1 (Opus): rendering core, errors are subtle and only visible in game.
- Precision override C2 (Opus): light engine mixins, mistakes cause persistent light corruption in worlds.
