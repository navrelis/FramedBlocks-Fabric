package xfacthd.framedblocks.platform.model;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class RenderTypeHelper
{
    public static final int CHUNK_LAYER_COUNT = 5;
    private static final BlendMode[] BLEND_MODES = new BlendMode[] {
            BlendMode.SOLID, BlendMode.CUTOUT_MIPPED, BlendMode.CUTOUT, BlendMode.TRANSLUCENT, BlendMode.TRANSLUCENT
    };

    /**
     * {@return the index of the given chunk render type in {@link RenderType#chunkBufferLayers()} or -1 if the
     * given type is not a chunk render type}
     */
    public static int getChunkLayerId(RenderType renderType)
    {
        if (renderType == RenderType.solid()) return 0;
        if (renderType == RenderType.cutoutMipped()) return 1;
        if (renderType == RenderType.cutout()) return 2;
        if (renderType == RenderType.translucent()) return 3;
        if (renderType == RenderType.tripwire()) return 4;
        return -1;
    }

    public static RenderType getChunkLayer(int layerId)
    {
        return switch (layerId)
        {
            case 0 -> RenderType.solid();
            case 1 -> RenderType.cutoutMipped();
            case 2 -> RenderType.cutout();
            case 3 -> RenderType.translucent();
            case 4 -> RenderType.tripwire();
            default -> throw new IllegalArgumentException("Invalid chunk layer ID: " + layerId);
        };
    }

    /**
     * {@return the {@link BlendMode} to use for quads which are to be rendered in the given chunk render type}
     */
    public static BlendMode getBlendMode(RenderType chunkRenderType)
    {
        int id = getChunkLayerId(chunkRenderType);
        return id < 0 ? BlendMode.DEFAULT : BLEND_MODES[id];
    }

    /**
     * {@return the {@link BlendMode} to use for item quads which Forge would render with the given item render type}
     * The Fabric item render context only distinguishes translucent from non-translucent quads, entity cutout
     * and solid item render types therefore map to {@link BlendMode#CUTOUT}
     */
    public static BlendMode getItemBlendMode(RenderType itemRenderType)
    {
        if (itemRenderType == Sheets.translucentCullBlockSheet() || itemRenderType == Sheets.translucentItemSheet())
        {
            return BlendMode.TRANSLUCENT;
        }
        if (itemRenderType == Sheets.cutoutBlockSheet() || itemRenderType == Sheets.solidBlockSheet())
        {
            return BlendMode.CUTOUT;
        }
        int id = getChunkLayerId(itemRenderType);
        return id < 0 ? BlendMode.DEFAULT : BLEND_MODES[id];
    }

    public static RenderType getEntityRenderType(RenderType chunkRenderType, boolean cull)
    {
        if (chunkRenderType != RenderType.translucent())
        {
            return Sheets.cutoutBlockSheet();
        }
        return cull || !Minecraft.useShaderTransparency() ? Sheets.translucentCullBlockSheet() : Sheets.translucentItemSheet();
    }

    public static RenderType getMovingBlockRenderType(RenderType renderType)
    {
        if (renderType == RenderType.translucent())
        {
            return RenderType.translucentMovingBlock();
        }
        return renderType;
    }

    public static RenderType getFallbackItemRenderType(ItemStack stack, BakedModel model, boolean cull)
    {
        if (stack.getItem() instanceof BlockItem blockItem)
        {
            ChunkRenderTypeSet renderTypes = ModelBridge.getRenderTypes(
                    model, blockItem.getBlock().defaultBlockState(), RandomSource.create(42), ModelData.EMPTY
            );
            if (renderTypes.contains(RenderType.translucent()))
            {
                return getEntityRenderType(RenderType.translucent(), cull);
            }
            return Sheets.cutoutBlockSheet();
        }
        return cull ? Sheets.translucentCullBlockSheet() : Sheets.translucentItemSheet();
    }



    private RenderTypeHelper() { }
}
