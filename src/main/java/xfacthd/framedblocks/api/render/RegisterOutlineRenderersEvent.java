package xfacthd.framedblocks.api.render;

import org.jetbrains.annotations.ApiStatus;
import xfacthd.framedblocks.api.type.IBlockType;

import java.util.function.BiConsumer;

public final class RegisterOutlineRenderersEvent
{
    private final BiConsumer<IBlockType, OutlineRenderer> registrar;

    @ApiStatus.Internal
    public RegisterOutlineRenderersEvent(BiConsumer<IBlockType, OutlineRenderer> registrar)
    {
        this.registrar = registrar;
    }

    /**
     * Register an {@link OutlineRenderer} for the given {@link IBlockType}
     * @param type The {@link IBlockType}, must return true for {@link IBlockType#hasSpecialHitbox()}
     */
    public void register(IBlockType type, OutlineRenderer renderer)
    {
        registrar.accept(type, renderer);
    }
}
