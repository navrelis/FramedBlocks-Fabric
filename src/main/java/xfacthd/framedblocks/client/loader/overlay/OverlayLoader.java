package xfacthd.framedblocks.client.loader.overlay;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.client.model.cube.FramedMarkedCubeGeometry;
import xfacthd.framedblocks.client.model.cube.FramedTargetGeometry;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Serves the models whose JSON contains {@code "loader": "framedblocks_fabric:overlay"}. Such a model wraps a regular
 * model in the {@code "model"} key and renders it slightly scaled up around the (optional) {@code "center"}, in
 * order to avoid z-fighting with the camo model it is rendered on top of.
 * <p>
 * Models are served by a model resolver which takes precedence over the vanilla model loading, the files are read
 * and parsed at the start of each model reload.
 */
public final class OverlayLoader
{
    public static final ResourceLocation ID = Utils.rl("overlay");
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private static final List<ResourceLocation> OVERLAY_MODELS = List.of(
            FramedMarkedCubeGeometry.SLIME_FRAME_LOCATION.id(),
            FramedMarkedCubeGeometry.REDSTONE_FRAME_LOCATION.id(),
            FramedTargetGeometry.OVERLAY_LOCATION.id()
    );
    private static final Vector3f CENTER = new Vector3f(.5F, .5F, .5F);
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1.001F, 1.001F, 1.001F);

    public static void init()
    {
        PreparableModelLoadingPlugin.register(OverlayLoader::loadModels, OverlayLoader::onInitializeModelLoader);
    }

    private static CompletableFuture<Map<ResourceLocation, UnbakedModel>> loadModels(
            ResourceManager resourceManager, Executor executor
    )
    {
        return CompletableFuture.supplyAsync(() ->
        {
            Map<ResourceLocation, UnbakedModel> models = new HashMap<>();
            for (ResourceLocation id : OVERLAY_MODELS)
            {
                ResourceLocation file = MODEL_LISTER.idToFile(id);
                Resource resource = resourceManager.getResource(file).orElse(null);
                if (resource == null)
                {
                    FramedBlocks.LOGGER.error("Overlay model '{}' not found", file);
                    continue;
                }

                try (BufferedReader reader = resource.openAsReader())
                {
                    JsonObject obj = GsonHelper.parse(reader);
                    models.put(id, read(obj));
                }
                catch (IOException | JsonParseException e)
                {
                    FramedBlocks.LOGGER.error("Failed to load overlay model '{}'", file, e);
                }
            }
            return Map.copyOf(models);
        }, executor);
    }

    private static void onInitializeModelLoader(
            Map<ResourceLocation, UnbakedModel> models, ModelLoadingPlugin.Context context
    )
    {
        context.resolveModel().register(ctx -> models.get(ctx.id()));
    }

    private static OverlayGeometry read(JsonObject obj) throws JsonParseException
    {
        String loader = GsonHelper.getAsString(obj, "loader");
        if (!ID.toString().equals(loader))
        {
            throw new JsonSyntaxException("Expected loader '" + ID + "', got '" + loader + "'");
        }

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
