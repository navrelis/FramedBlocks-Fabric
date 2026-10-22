package xfacthd.framedblocks.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.common.block.IFramedDoubleBlock;
import xfacthd.framedblocks.common.block.door.FramedTrapDoorBlock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForeignDefaultMethodTests implements FabricGameTest
{
    /**
     * NeoForge-on-Fabric layers such as Sporran inject default methods into vanilla classes (IBlockExtension into
     * Block, IBaseRailBlockExtension into BaseRailBlock). A class inheriting two
     * unrelated defaults with the same signature and no concrete implementation in its superclass chain crashes with an
     * AbstractMethodError or IncompatibleClassChangeError. Therefore all of these methods must resolve to a class
     * instead of an interface default. Client-only classes (FluidModel) can't be checked here because they can't be
     * loaded in the server environment of the game tests
     */
    private static final List<Ref> NEOFORGE_BLOCK_DEFAULTS = List.of(
            new Ref(
                    "getAppearance",
                    BlockState.class, BlockAndTintGetter.class, BlockPos.class, Direction.class, BlockState.class, BlockPos.class
            ),
            new Ref("getLightEmission", BlockState.class, BlockGetter.class, BlockPos.class),
            new Ref("getSoundType", BlockState.class, LevelReader.class, BlockPos.class, Entity.class),
            new Ref("getFriction", BlockState.class, LevelReader.class, BlockPos.class, Entity.class),
            new Ref("getExplosionResistance", BlockState.class, BlockGetter.class, BlockPos.class, Explosion.class),
            new Ref("isFlammable", BlockState.class, BlockGetter.class, BlockPos.class, Direction.class),
            new Ref("getFlammability", BlockState.class, BlockGetter.class, BlockPos.class, Direction.class),
            new Ref("getFireSpreadSpeed", BlockState.class, BlockGetter.class, BlockPos.class, Direction.class),
            new Ref("isFireSource", BlockState.class, LevelReader.class, BlockPos.class, Direction.class),
            new Ref("addRunningEffects", BlockState.class, Level.class, BlockPos.class, Entity.class),
            new Ref(
                    "addLandingEffects",
                    BlockState.class, ServerLevel.class, BlockPos.class, BlockState.class, LivingEntity.class, int.class
            ),
            new Ref("hidesNeighborFace", BlockGetter.class, BlockPos.class, BlockState.class, BlockState.class, Direction.class),
            new Ref("onBlockStateChange", LevelReader.class, BlockPos.class, BlockState.class, BlockState.class),
            new Ref("getMapColor", BlockState.class, BlockGetter.class, BlockPos.class, MapColor.class),
            new Ref("getBeaconColorMultiplier", BlockState.class, LevelReader.class, BlockPos.class, BlockPos.class),
            new Ref("shouldDisplayFluidOverlay", BlockState.class, BlockAndTintGetter.class, BlockPos.class, FluidState.class),
            new Ref("canEntityDestroy", BlockState.class, BlockGetter.class, BlockPos.class, Entity.class),
            new Ref("isLadder", BlockState.class, LevelReader.class, BlockPos.class, LivingEntity.class),
            new Ref("makesOpenTrapdoorAboveClimbable", BlockState.class, LevelReader.class, BlockPos.class, BlockState.class),
            // BaseRailBlock
            new Ref("isValidRailShape", RailShape.class)
    );
    /**
     * Overrides of IFramedBlock in IFramedDoubleBlock. A class implementing IFramedDoubleBlock must declare its own
     * concrete overrides of these, otherwise it silently inherits the IFramedBlock-only override of its superclass
     */
    private static final List<String> DOUBLE_BLOCK_OVERRIDES = List.of("getSoundType", "addRunningEffects", "addLandingEffects");

    @GameTest(template = EMPTY_STRUCTURE)
    public void framedClassesDeclareConcreteForeignDefaults(GameTestHelper helper)
    {
        List<String> offenders = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK)
        {
            if (!(block instanceof IFramedBlock))
            {
                continue;
            }

            Class<?> clazz = block.getClass();
            String name = BuiltInRegistries.BLOCK.getKey(block) + " (" + clazz.getName() + ")";
            for (Ref ref : NEOFORGE_BLOCK_DEFAULTS)
            {
                // FramedTrapDoorBlock (and subclasses) deliberately have no isLadder() override to keep the current behaviour
                if (ref.name.equals("isLadder") && block instanceof FramedTrapDoorBlock)
                {
                    continue;
                }

                Method method = ref.resolve(clazz);
                if (method != null && method.getDeclaringClass().isInterface())
                {
                    offenders.add(name + ": " + ref.name + "() is only a default of " + method.getDeclaringClass().getSimpleName());
                }
            }

            if (block instanceof IFramedDoubleBlock)
            {
                for (Ref ref : NEOFORGE_BLOCK_DEFAULTS)
                {
                    if (!DOUBLE_BLOCK_OVERRIDES.contains(ref.name))
                    {
                        continue;
                    }

                    Method method = ref.resolve(clazz);
                    if (method == null || method.getDeclaringClass().isInterface())
                    {
                        continue; // Interface defaults are already reported above
                    }
                    if (!Arrays.asList(method.getDeclaringClass().getInterfaces()).contains(IFramedDoubleBlock.class))
                    {
                        offenders.add(
                                name + ": " + ref.name + "() is declared by " + method.getDeclaringClass().getSimpleName() +
                                " which doesn't implement IFramedDoubleBlock directly"
                        );
                    }
                }
            }
        }

        if (!offenders.isEmpty())
        {
            throw new IllegalStateException(
                    "Missing concrete overrides of methods that are also defined by NeoForge:\n" + String.join("\n", offenders)
            );
        }
        helper.succeed();
    }

    private record Ref(String name, Class<?>... params)
    {
        Method resolve(Class<?> clazz)
        {
            try
            {
                return clazz.getMethod(name, params);
            }
            catch (NoSuchMethodException ignored)
            {
                return null;
            }
        }
    }
}
