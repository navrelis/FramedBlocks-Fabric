package xfacthd.framedblocks.codegen;

import xfacthd.framedblocks.codegen.impl.skippreds.SkipPredicateGeneratorImpl;

/**
 * Command line driver for the skip predicate generator.
 * Usage: {@code Drv [BLOCK_TYPE]}; without argument all predicates are regenerated.
 */
public final class Drv
{
    public static void main(String[] args)
    {
        SkipPredicateGeneratorImpl.generateAndExportClasses(args.length > 0 ? args[0] : null);
    }

    private Drv() { }
}
