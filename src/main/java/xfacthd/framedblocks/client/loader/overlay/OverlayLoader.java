package xfacthd.framedblocks.client.loader.overlay;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.api.util.Utils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Replacement for the Forge geometry loader {@code framedblocks:overlay}.
 * <p>
 * Fabric has no custom JSON model loaders. Instead, {@link #load(ResourceManager, Executor)} finds all block model
 * JSONs in the mod's namespace which declare {@code "loader": "framedblocks_fabric:overlay"} on every resource
 * reload and parses them exactly like the Forge loader did. {@link #resolve(Map, ModelResolver.Context)} then serves
 * the parsed models to the model loader in place of the JSON files (see
 * {@link xfacthd.framedblocks.client.model.ModelEventsBridge})
 * <p>
 * JSON format: {@code "model"} (required, a regular block model object, may use {@code "parent"}),
 * {@code "center"} (optional, array of three floats in pixels; the model is moved so that this point ends up at the
 * block center and scaled up very slightly)
 */
public final class OverlayLoader
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = Utils.rl("overlay");
    private static final String LOADER_ID = ID.toString();
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private static final Vector3f CENTER = new Vector3f(.5F, .5F, .5F);
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1.001F, 1.001F, 1.001F);

    /**
     * Data loader of the model loading plugin, finds and parses all overlay models. Runs off-thread
     * @return the overlay models by model location (i.e. {@code framedblocks_fabric:block/redstone_frame})
     */
    public static CompletableFuture<Map<ResourceLocation, OverlayGeometry>> load(ResourceManager resourceManager, Executor executor)
    {
        return CompletableFuture.supplyAsync(() -> loadOverlayModels(resourceManager), executor);
    }

    /**
     * {@return the overlay model for the model location requested by the given context or {@code null} if it is
     * not an overlay model}
     */
    @Nullable
    public static UnbakedModel resolve(Map<ResourceLocation, OverlayGeometry> overlayModels, ModelResolver.Context context)
    {
        return overlayModels.get(context.id());
    }

    private static Map<ResourceLocation, OverlayGeometry> loadOverlayModels(ResourceManager resourceManager)
    {
        Map<ResourceLocation, OverlayGeometry> models = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "models",
                file -> file.getNamespace().equals(FramedConstants.MOD_ID) && file.getPath().endsWith(".json")
        );
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet())
        {
            ResourceLocation file = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader())
            {
                JsonObject obj = GsonHelper.parse(reader);
                if (!obj.has("loader") || !LOADER_ID.equals(GsonHelper.getAsString(obj, "loader")))
                {
                    continue;
                }

                ResourceLocation modelId = MODEL_LISTER.fileToId(file);
                OverlayGeometry geometry = read(obj);
                geometry.wrapped().name = modelId.toString();
                models.put(modelId, geometry);
            }
            catch (IOException | JsonParseException | IllegalArgumentException e)
            {
                LOGGER.error("Failed to load model file {}", file, e);
            }
        }
        return models;
    }

    public static OverlayGeometry read(JsonObject obj) throws JsonParseException
    {
        BlockModel model = BlockModel.fromString(GsonHelper.getAsJsonObject(obj, "model").toString());

        Vector3f offset = OverlayGeometry.VEC_ZERO;
        Vector3f scale = new Vector3f(DEFAULT_SCALE);

        if (obj.has("center"))
        {
            JsonArray arr = GsonHelper.getAsJsonArray(obj, "center");
            if (arr.size() != 3)
            {
                throw new JsonSyntaxException("Invalid center array, expected exactly three elements");
            }

            Vector3f center = new Vector3f(
                    GsonHelper.convertToFloat(arr.get(0), "center[0]") / 16F,
                    GsonHelper.convertToFloat(arr.get(1), "center[1]") / 16F,
                    GsonHelper.convertToFloat(arr.get(2), "center[2]") / 16F
            );

            offset = new Vector3f(CENTER);
            offset.sub(center);

            // Slightly skew the scale to avoid z-fighting on off-center models
            Vector3f scaleAdd = new Vector3f(.01F, .01F, .01F);
            scaleAdd.mul(Mth.abs(offset.x()), Mth.abs(offset.y()), Mth.abs(offset.z()));
            scale.add(scaleAdd);
        }

        return new OverlayGeometry(model, offset, scale);
    }



    private OverlayLoader() { }
}
