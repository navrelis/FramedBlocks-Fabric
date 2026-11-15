# Report u3-1201-indium

## Task: port the Sodium/Continuity capture-crash fix (5ac1c605b) to 1.20.1 (Indium)
**Result: done, commit 3df026520 on branch claude/u3-1201-indium-449408.**

Reproduced first (unfixed code, `runClientTest -Pcompat_runtime=sodium,continuity`): CCE in chunk meshing, game crashed. Stack in `cce-repro-stack.txt`
(`BakedModel.emitBlockQuads` <- `MultiPartBakedModel` <- `ForwardingBakedModel` <- `CtmBakedModel.emitBlockQuads` <- `QuadCapturer.run:80` <- `ModelBridge.getCapturedQuads:201` <- ... <- `TerrainRenderContext.tessellateBlock`). Same class as issues #2/#3.

Changes:
- New `mixin/client/indium/MixinBakedModelIndiumCapture` (interface mixin on `BakedModel.emitBlockQuads`, priority 1500 > Indium's default 1000, HEAD inject routes `QuadCapturer` to vanilla emission), `framedblocks_fabric.indium.mixins.json`, registered in `fabric.mod.json`; `FramedMixinConfigPlugin` applies it only if `indium` is loaded.
- `ModelBridge`: `unwrapOwnModel` (FRAPI 0.92.12 has `WrapperBakedModel`, so verbatim port) + `emitVanillaIntoCapture`.
- `QuadCapturer`: catches RuntimeException/LinkageError, falls back to plain quads, WARN once per model class.
- `build.gradle`/`gradle.properties`: dev-only `continuity` token (3.0.0+1.20.1); client test harness enables Continuity's built-in pack and checks the glass model is wrapped.

Tests (`runClientTest`), all PASS, 0 fallback WARN / CCE in logs:
- Indigo; `sodium`; `sodium,continuity` (CtmBakedModel wraps glass); `jei,sodium,continuity` (183 mod items + 136 camo stacks drawn).
- `gradlew --no-daemon --no-build-cache build`: BUILD SUCCESSFUL. Gametests before (81dde743f): 822 required passed, 26 optional failed; after: 822 required passed, 26 optional failed (identical; the 26 are pre-existing occlusion light-level optionals).

Open points:
- JEI dev runtime did not start on 1.20.1 (`mezz_config` missing). Added `modLocalRuntime net.mezzdev.config:mezz_config-1.20.1-fabric:0.6.6` + `mezz_config_version` (same fix as on 1.21.1); this is a dev-only dependency beyond Continuity, needed for the required jei run.
- A plain `gradlew build` hit a Gradle build-cache lock timeout from another session's process; `--no-build-cache` avoided it.
- Item paths of issues #2/#3 (JEI / Tooltip Overhaul icon) were not separately crashing in repro (chunk path crashed first); they go through the same capture, covered by the item grid in the client test.
