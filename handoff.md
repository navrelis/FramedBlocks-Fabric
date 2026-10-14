# Handoff: FramedBlocks → Fabric 1.21.1 port
Updated: 2026-10-14 (session 6, end). Branch `1.21.1-fabric`, pushed to `origin`.

All planned tasks are done (see `.knowledge/plan.md`). Final state, checks, manual test checklist and known limitations: `.knowledge/report.md`. Requirements, decisions and the delegation log: `.knowledge/requirements.md`, `decisions.md`, `log.md`.

Next steps, if work continues:
1. Work through the manual test checklist (`.knowledge/report.md` §5) in game, with Indigo and Sodium, and on a dedicated server.
2. Optional improvements from report §7: Loom/Gradle upgrade to lift the JEI and MezzConfig pins and replace JEI APIs marked for removal; saw tooltip energy unit; make the repository public (LGPL source availability) before distributing jars.
3. Rebuild the local code graph with `graphify update .` (not committed, ~98 MB).

Working rules from earlier sessions still apply: build with `JAVA_TOOL_OPTIONS=-Duser.language=en ./gradlew build` (runs all game tests: expect 900 required passed, 27 optional failed); never accept the Minecraft EULA; never use `@Local(name = ...)` against Minecraft classes; no attribution lines in commits.
