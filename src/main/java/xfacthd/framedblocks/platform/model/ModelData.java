package xfacthd.framedblocks.platform.model;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Immutable container for additional data passed to models, transported to the chunk meshing threads through the
 * block entity render data (see {@link ModelDataHelper#get(net.minecraft.world.level.BlockGetter, net.minecraft.core.BlockPos)})
 */
public final class ModelData
{
    private static final ModelProperty<?>[] NO_KEYS = new ModelProperty[0];
    private static final Object[] NO_VALUES = new Object[0];
    public static final ModelData EMPTY = new ModelData(NO_KEYS, NO_VALUES);

    private final ModelProperty<?>[] keys;
    private final Object[] values;

    private ModelData(ModelProperty<?>[] keys, Object[] values)
    {
        this.keys = keys;
        this.values = values;
    }

    public Set<ModelProperty<?>> getProperties()
    {
        return Set.of(keys);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public boolean has(ModelProperty<?> prop)
    {
        ModelProperty<?>[] keys = this.keys;
        for (int i = 0; i < keys.length; i++)
        {
            if (keys[i] == prop)
            {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(ModelProperty<T> prop)
    {
        ModelProperty<?>[] keys = this.keys;
        for (int i = 0; i < keys.length; i++)
        {
            if (keys[i] == prop)
            {
                return (T) values[i];
            }
        }
        return null;
    }

    public Builder derive()
    {
        return new Builder(this);
    }

    public static Builder builder()
    {
        return new Builder(null);
    }



    public static final class Builder
    {
        private ModelProperty<?>[] keys;
        private Object[] values;
        private int size;

        private Builder(@Nullable ModelData parent)
        {
            if (parent != null && parent.keys.length > 0)
            {
                this.size = parent.keys.length;
                this.keys = Arrays.copyOf(parent.keys, size + 2);
                this.values = Arrays.copyOf(parent.values, size + 2);
            }
            else
            {
                this.size = 0;
                this.keys = new ModelProperty[2];
                this.values = new Object[2];
            }
        }

        public <T> Builder with(ModelProperty<T> prop, T value)
        {
            Preconditions.checkState(prop.test(value), "Value is invalid for this property");

            for (int i = 0; i < size; i++)
            {
                if (keys[i] == prop)
                {
                    values[i] = value;
                    return this;
                }
            }
            if (size == keys.length)
            {
                keys = Arrays.copyOf(keys, size * 2);
                values = Arrays.copyOf(values, size * 2);
            }
            keys[size] = prop;
            values[size] = value;
            size++;
            return this;
        }

        public ModelData build()
        {
            if (size == 0)
            {
                return EMPTY;
            }
            return new ModelData(Arrays.copyOf(keys, size), Arrays.copyOf(values, size));
        }
    }
}
