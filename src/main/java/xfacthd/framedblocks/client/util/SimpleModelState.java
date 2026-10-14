package xfacthd.framedblocks.client.util;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ModelState;

public record SimpleModelState(Transformation rotation, boolean uvLocked) implements ModelState
{
    @Override
    public Transformation getRotation()
    {
        return rotation;
    }

    @Override
    public boolean isUvLocked()
    {
        return uvLocked;
    }
}
