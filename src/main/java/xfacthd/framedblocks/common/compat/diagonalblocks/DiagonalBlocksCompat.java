package xfacthd.framedblocks.common.compat.diagonalblocks;

import fuzs.diagonalblocks.api.v2.DiagonalBlockType;
import fuzs.diagonalblocks.api.v2.DiagonalBlockTypes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.model.wrapping.RegisterModelWrappersEvent;
import xfacthd.framedblocks.api.model.wrapping.WrapHelper;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.api.block.render.FramedBlockColor;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.pane.FramedPaneBlock;
import xfacthd.framedblocks.common.block.pillar.FramedFenceBlock;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.platform.event.ModEvents;

import java.util.Optional;

public final class DiagonalBlocksCompat
{
    private static boolean loaded = false;

    public static void init()
    {
        if (Platform.isModLoaded("diagonalblocks"))
        {
            try
            {
                GuardedAccess.init();
                if (Platform.isClient())
                {
                    GuardedClientAccess.init();
                }
                loaded = true;
            }
            catch (Throwable t)
            {
                FramedBlocks.LOGGER.error("Failed to initialized Diagonal Blocks integration", t);
            }
        }
    }

    public static void commonSetup()
    {
        if (loaded)
        {
            try
            {
                GuardedAccess.commonSetup();
            }
            catch (Throwable t)
            {
                FramedBlocks.LOGGER.error("Failed to add Diagonal Blocks variants to the framed block entity type", t);
            }
        }
    }

    public static boolean isFramedFence(BlockState state)
    {
        return (loaded && GuardedAccess.isFramedFence(state)) || state.getBlock() instanceof FramedFenceBlock;
    }

    public static boolean isFramedPane(BlockState state)
    {
        return (loaded && GuardedAccess.isFramedPane(state)) || state.getBlock() instanceof FramedPaneBlock;
    }



    private static final class GuardedAccess
    {
        public static void init()
        {
            DiagonalBlockTypes.FENCE.registerBlockFactory(
                    Utils.getKeyOrThrow(FBContent.BLOCK_FRAMED_FENCE).location(),
                    FramedDiagonalFenceBlock::new
            );
            DiagonalBlockTypes.WINDOW.registerBlockFactory(
                    Utils.getKeyOrThrow(FBContent.BLOCK_FRAMED_PANE).location(),
                    FramedDiagonalGlassPaneBlock::new
            );
            DiagonalBlockTypes.WINDOW.disableBlockFactory(Utils.getKeyOrThrow(FBContent.BLOCK_FRAMED_BARS).location());
            DiagonalBlockTypes.WALL.disableBlockFactory(Utils.getKeyOrThrow(FBContent.BLOCK_FRAMED_WALL).location());
        }

        public static void commonSetup()
        {
            // Diagonal Blocks may register its blocks before or after this mod, so handle both already registered
            // and future entries
            RegistryEntryAddedCallback.allEntries(BuiltInRegistries.BLOCK, GuardedAccess::onBlockAdded);
        }

        private static void onBlockAdded(Holder.Reference<Block> holder)
        {
            ResourceLocation id = holder.key().location();
            if (isDiagonalVariant(DiagonalBlockTypes.FENCE, FBContent.BLOCK_FRAMED_FENCE, id) ||
                isDiagonalVariant(DiagonalBlockTypes.WINDOW, FBContent.BLOCK_FRAMED_PANE, id))
            {
                FBContent.BE_TYPE_FRAMED_BLOCK.value().addSupportedBlock(holder.value());
            }
        }

        private static boolean isDiagonalVariant(DiagonalBlockType type, Holder<Block> srcBlock, ResourceLocation id)
        {
            return id.equals(getBlockName(type, srcBlock));
        }

        public static boolean isFramedFence(BlockState state)
        {
            return state.getBlock() instanceof FramedDiagonalFenceBlock;
        }

        public static boolean isFramedPane(BlockState state)
        {
            return state.getBlock() instanceof FramedDiagonalGlassPaneBlock;
        }

        private static ResourceLocation getBlockName(DiagonalBlockType type, Holder<Block> srcBlock)
        {
            ResourceLocation srcName = Utils.getKeyOrThrow(srcBlock).location();
            return type.id(srcName.getNamespace() + "/" + srcName.getPath());
        }

        private static Optional<Holder.Reference<Block>> getBlock(DiagonalBlockType type, Holder<Block> srcBlock)
        {
            return BuiltInRegistries.BLOCK.getHolder(ResourceKey.create(Registries.BLOCK, getBlockName(type, srcBlock)));
        }



        private GuardedAccess() { }
    }

    private static final class GuardedClientAccess
    {
        public static void init()
        {
            ModEvents.addListener(RegisterModelWrappersEvent.class, GuardedClientAccess::onRegisterModelWrappers);
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerBlockColors(client.getBlockColors()));
        }

        private static void onRegisterModelWrappers(final RegisterModelWrappersEvent event)
        {
            GuardedAccess.getBlock(DiagonalBlockTypes.FENCE, FBContent.BLOCK_FRAMED_FENCE).ifPresent(
                    holder -> WrapHelper.wrap(holder, FramedDiagonalFenceGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK)
            );
            GuardedAccess.getBlock(DiagonalBlockTypes.WINDOW, FBContent.BLOCK_FRAMED_PANE).ifPresent(
                    holder -> WrapHelper.wrap(holder, FramedDiagonalPaneGeometry::new, WrapHelper.IGNORE_WATERLOGGED_LOCK)
            );
        }

        private static void registerBlockColors(BlockColors colors)
        {
            GuardedAccess.getBlock(DiagonalBlockTypes.FENCE, FBContent.BLOCK_FRAMED_FENCE).ifPresent(
                    holder -> colors.register(FramedBlockColor.INSTANCE, holder.value())
            );
            GuardedAccess.getBlock(DiagonalBlockTypes.WINDOW, FBContent.BLOCK_FRAMED_PANE).ifPresent(
                    holder -> colors.register(FramedBlockColor.INSTANCE, holder.value())
            );
        }
    }



    private DiagonalBlocksCompat() { }
}
