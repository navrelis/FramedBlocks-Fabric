package xfacthd.framedblocks.platform.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Minimal synchronous in-process event dispatcher for the events fired by FramedBlocks itself.
 * Listeners are matched on the exact class of the posted event and invoked in registration order.
 */
public final class ModEvents
{
    private static final Map<Class<?>, List<Consumer<?>>> LISTENERS = new ConcurrentHashMap<>();

    public static <T> void addListener(Class<T> type, Consumer<T> listener)
    {
        LISTENERS.computeIfAbsent(type, $ -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public static <T> T post(T event)
    {
        List<Consumer<?>> listeners = LISTENERS.get(event.getClass());
        if (listeners != null)
        {
            for (Consumer<?> listener : listeners)
            {
                ((Consumer<T>) listener).accept(event);
            }
        }
        return event;
    }



    private ModEvents() { }
}
