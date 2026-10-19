# Skip predicate generator

Generates the `*SkipPredicate` classes in `src/main/java/xfacthd/framedblocks/common/data/skippreds/**` (the ones
marked "machine-generated"). Ported from upstream FramedBlocks (`upstream/1.21` tip: `src/test/java/xfacthd/framedblocks/codegen`)
and extended with the entries of the backported types (Slope Edge Slab/Panel, Prism Corner Slope Panels). It lives
outside all Gradle source sets and only needs a JDK (no other dependencies).

Files:

* `src/xfacthd/framedblocks/codegen/SkipPredicateGenerator.java` - upstream entry point (placeholder type), unused, use `Drv`
* `src/xfacthd/framedblocks/codegen/impl/skippreds/SkipPredicateGeneratorData.java` - `KNOWN_TYPES`: one entry per non-double `BlockType`,
  in enum order, plus the properties and "dir ids" used to decide which types are tested against each other
* `src/xfacthd/framedblocks/codegen/impl/skippreds/SkipPredicateGeneratorImpl.java` - the generator
* `src/xfacthd/framedblocks/codegen/Drv.java` - tiny `main` taking an optional type name
* `src/org/jetbrains/annotations/{Nullable,Contract}.java` - stubs for the two JetBrains annotations used by the generator

## Run (from the repository root)

```bash
# compile (output directory can be anywhere, e.g. a temp folder)
mkdir -p build/skippreds-codegen
javac -d build/skippreds-codegen $(find tools/skippreds-codegen/src -name '*.java')

# regenerate everything (no argument) or one type plus all types that need to test against it
java -cp build/skippreds-codegen xfacthd.framedblocks.codegen.Drv
java -cp build/skippreds-codegen xfacthd.framedblocks.codegen.Drv FRAMED_SLOPE_EDGE_SLAB
```

The working directory MUST be the repository root: the generator reads `./src/main/java/xfacthd/framedblocks/common/data/BlockType.java`
as text (one constant per line, 7th constructor argument = `doubleBlock`) and writes to
`./src/main/java/xfacthd/framedblocks/common/data/skippreds/`. Output uses LF line endings; with `core.autocrlf=true` unchanged
files do not show up in `git status`. After a full regeneration `git diff --stat -- src/main/java/xfacthd/framedblocks/common/data/skippreds`
must be empty for committed predicates.

## Adding a block type

1. Add the constant to `BlockType` (its position decides the position in `KNOWN_TYPES`; the generator aborts with
   `BlockType.X missing from KNOWN_TYPES, expected after BlockType.Y` otherwise; double blocks must NOT be listed).
2. Add an `entry("FRAMED_X", "<skippreds sub package>")` to `SkipPredicateGeneratorData.KNOWN_TYPES` at the same position.
   Use `.props(...)` for the state properties read by the predicate, `.dirs(...)` for the face descriptors
   (`new TestDir(<DirClass>, <name>, null, <dir ids...>)`; types are tested against each other iff they share a dir id) and `.shortName(...)`
   if the class name does not follow from the enum name.
3. Write the matching `getXxxDir` methods in the `<Group>Dirs` class (hand written).
4. Regenerate (see above), register `new XSkipPredicate()` in `SideSkipPredicates`.

Differences to upstream's generator data: dir ids are plain strings (this tree is based on the 1.21 line, not on upstream's `TestDirIds`
constants), `slope_edge_tri` is split into `slope_edge_tri_xz` / `slope_edge_tri_y` like in newer upstream versions, and the
`BlockType` constructor has the extra `specialHitbox` argument (hence `doubleBlock` is read from index 6).
