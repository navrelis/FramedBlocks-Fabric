package xfacthd.framedblocks.platform.util;

import java.util.function.Supplier;

public final class Lazy<T> implements Supplier<T>
{
    private final Supplier<T> factory;
    private volatile Object value = null;
    private volatile boolean computed = false;

    private Lazy(Supplier<T> factory)
    {
        this.factory = factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get()
    {
        if (!computed)
        {
            synchronized (this)
            {
                if (!computed)
                {
                    value = factory.get();
                    computed = true;
                }
            }
        }
        return (T) value;
    }

    public void invalidate()
    {
        synchronized (this)
        {
            computed = false;
            value = null;
        }
    }



    public static <T> Lazy<T> of(Supplier<T> factory)
    {
        return new Lazy<>(factory);
    }

    /**
     * Same as {@link #of(Supplier)}, this implementation is always safe to be used from multiple threads
     */
    public static <T> Lazy<T> concurrentOf(Supplier<T> factory)
    {
        return new Lazy<>(factory);
    }
}
