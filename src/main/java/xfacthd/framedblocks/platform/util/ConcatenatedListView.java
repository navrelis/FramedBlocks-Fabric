package xfacthd.framedblocks.platform.util;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * Unmodifiable view over multiple lists which presents them as one continuous list.
 * Changes made to the backing lists are reflected by the view.
 */
public final class ConcatenatedListView<T> extends AbstractList<T> implements RandomAccess
{
    private final List<? extends List<? extends T>> lists;

    private ConcatenatedListView(List<? extends List<? extends T>> lists)
    {
        this.lists = lists;
    }

    @Override
    public int size()
    {
        int size = 0;
        for (List<? extends T> list : lists)
        {
            size += list.size();
        }
        return size;
    }

    @Override
    public boolean isEmpty()
    {
        for (List<? extends T> list : lists)
        {
            if (!list.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public T get(int index)
    {
        if (index < 0)
        {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        int remaining = index;
        for (List<? extends T> list : lists)
        {
            int size = list.size();
            if (remaining < size)
            {
                return list.get(remaining);
            }
            remaining -= size;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }

    @Override
    public Iterator<T> iterator()
    {
        Iterator<? extends List<? extends T>> listIt = lists.iterator();
        return new Iterator<>()
        {
            private Iterator<? extends T> current = null;

            @Override
            public boolean hasNext()
            {
                while (current == null || !current.hasNext())
                {
                    if (!listIt.hasNext())
                    {
                        return false;
                    }
                    current = listIt.next().iterator();
                }
                return true;
            }

            @Override
            public T next()
            {
                if (!hasNext())
                {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
    }



    public static <T> List<T> of(List<? extends List<? extends T>> lists)
    {
        return new ConcatenatedListView<>(lists);
    }

    @SafeVarargs
    public static <T> List<T> of(List<? extends T>... lists)
    {
        return new ConcatenatedListView<>(List.of(lists));
    }
}
