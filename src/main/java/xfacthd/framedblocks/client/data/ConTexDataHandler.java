package xfacthd.framedblocks.client.data;

import com.google.common.base.Preconditions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import xfacthd.framedblocks.common.config.ClientConfig;

import java.util.HashMap;
import java.util.Map;

public final class ConTexDataHandler
{
    private static Map<String, ModelProperty<?>> prelimProperties = new HashMap<>();
    private static boolean locked = false;
    private static ModelProperty<?>[] properties = null;

    public static void lockRegistration()
    {
        Preconditions.checkState(!locked, "ConTexDataHandler is already locked");

        locked = true;
        properties = prelimProperties.entrySet()
                .stream()
                .filter(e -> !ClientConfig.VIEW.isConTexDisabledFor(e.getKey()))
                .map(Map.Entry::getValue)
                .toArray(ModelProperty[]::new);
        prelimProperties = null;
    }

    public static void addConTexProperty(String modId, ModelProperty<?> property)
    {
        Preconditions.checkState(!locked, "ModelProperty registration is locked");
        Preconditions.checkNotNull(property, "ModelProperty must be non-null");
        prelimProperties.put(modId, property);
    }

    public static Object extractConTexData(ModelData modelData)
    {
        for (ModelProperty<?> prop : properties)
        {
            Object data = modelData.get(prop);
            if (data != null)
            {
                return data;
            }
        }
        return null;
    }



    private ConTexDataHandler() { }
}
