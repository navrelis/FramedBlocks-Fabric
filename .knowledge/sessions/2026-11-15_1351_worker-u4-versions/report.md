# u4-versions report

## Task: version bumps and release notes (1.1.3+mc1.21.1, 1.0.1+mc1.20.1)

**Part A** branch `claude/infallible-lalande-dc5195` (from mc/1.21.1), commit `b77969fa9`.
Files, same set as 414f93f73: `gradle.properties` (1.1.2 -> 1.1.3+mc1.21.1), `README.md`, `docs/curseforge/description.template.md`, `docs/listing/description.md` (new "## Fixed in 1.1.3" section), `docs/listing/metadata.md`.
Notes are one paragraph in three sentences (saw crashes; layered cube; ported upstream fixes). To amend, append a sentence to the "Fixed in 1.1.3" line in README, template and listing description (same text in all three).

**Part B** branch `claude/u4-1201` (worktree `F:/wt/u4-1201`, from mc/1.20.1), commit `4cb805f7f`.
Files: `gradle.properties` (1.0.0 -> 1.0.1+mc1.20.1), `README.md` (one "Fixed in 1.0.1" bullet).

Checks: git status clean in both worktrees; grep shows new versions and notes. No builds run (not requested).

Open points:
- The 1.20.1 branch has no listing/metadata/curseforge docs, only README. I put the bullet at the end of the Features list (that section says it is taken from the original mod, so the placement is a bit off). `docs/port/map.md` mentions 1.0.0 only as a planning row; untouched.
- The 1.20.1 note is reworded slightly to "A crash ... no longer occurs" to match 1.21.1 style.
