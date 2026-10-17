package xfacthd.framedblocks.platform.model;

import java.util.function.Predicate;

/**
 * Identity-based key for a value stored in a {@link ModelData} instance
 */
public final class ModelProperty<T> implements Predicate<T>
{
    private final Predicate<T> predicate;

    public ModelProperty()
    {
        this(value -> true);
    }

    public ModelProperty(Predicate<T> predicate)
    {
        this.predicate = predicate;
    }

    @Override
    public boolean test(T value)
    {
        return predicate.test(value);
    }
}
