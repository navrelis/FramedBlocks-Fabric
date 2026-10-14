package xfacthd.framedblocks.common.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.data.camo.block.rotator.BlockCamoRotatorPrototype;
import xfacthd.framedblocks.common.data.camo.block.rotator.BlockCamoRotators;
import xfacthd.framedblocks.common.net.payload.ClientboundCamoRotatorsPayload;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

/**
 * Replacement for NeoForge's registry data maps. The block camo rotator map is read from
 * {@code data/<namespace>/data_maps/block/block_camo_rotators.json} in every loaded data pack (using NeoForge's file
 * format) and resolved against the block registry and block tags once the tags are bound.
 * The resolved map is synced to remote clients.
 */
public final class FramedDataMaps
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CAMO_ROTATORS_FILE = "data_maps/block/block_camo_rotators.json";
    private static final ResourceLocation CAMO_ROTATORS_ID = Utils.rl("block_camo_rotators");
    private static final ResourceLocation RESOLVE_PHASE = Utils.rl("resolve_data_maps");
    // Raw file contents in data pack order (lowest priority first), tags are not bound yet when the reload listener runs
    private static volatile List<RotatorLayer> rotatorLayers = List.of();
    // Resolved map, on a remote client this is the map received from the server
    private static volatile Map<Block, BlockCamoRotatorPrototype> camoRotators = Map.of();

    public static void init()
    {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CamoRotatorLoader());

        // Must run after the default phase to make sure the camo factories are registered before the rotators are rebuilt
        Event<CommonLifecycleEvents.TagsLoaded> tagsLoaded = CommonLifecycleEvents.TAGS_LOADED;
        tagsLoaded.addPhaseOrdering(Event.DEFAULT_PHASE, RESOLVE_PHASE);
        tagsLoaded.register(RESOLVE_PHASE, (registries, client) ->
        {
            // A remote client receives the already resolved map from the server
            if (!client)
            {
                camoRotators = resolveCamoRotators(rotatorLayers);
            }
            // Depends on tags, on a remote client the synced map is received before the new tags are applied
            BlockCamoRotators.reload();
        });

        // Fired for each player on join and for every player after a data pack reload
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
        {
            // The host of an integrated server shares the resolved map in-JVM
            if (!player.server.isSingleplayerOwner(player.getGameProfile()))
            {
                ServerPlayNetworking.send(player, new ClientboundCamoRotatorsPayload(camoRotators));
            }
        });
    }

    public static BlockCamoRotatorPrototype getCamoRotator(Block block)
    {
        return camoRotators.get(block);
    }

    public static void forEachCamoRotator(BiConsumer<Block, BlockCamoRotatorPrototype> consumer)
    {
        camoRotators.forEach(consumer);
    }

    // Must be called on the client thread
    public static void applySyncedCamoRotators(Map<Block, BlockCamoRotatorPrototype> rotators)
    {
        camoRotators = Map.copyOf(rotators);
        BlockCamoRotators.reload();
    }

    private static Map<Block, BlockCamoRotatorPrototype> resolveCamoRotators(List<RotatorLayer> layers)
    {
        Reference2ObjectLinkedOpenHashMap<Block, BlockCamoRotatorPrototype> result = new Reference2ObjectLinkedOpenHashMap<>();
        for (RotatorLayer layer : layers)
        {
            if (layer.replace())
            {
                result.clear();
            }

            // Explicit block entries win over tag entries
            layer.tagValues().forEach((tagId, prototype) ->
            {
                for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(TagKey.create(Registries.BLOCK, tagId)))
                {
                    result.put(holder.value(), prototype);
                }
            });
            layer.blockValues().forEach((blockId, prototype) ->
            {
                if (BuiltInRegistries.BLOCK.containsKey(blockId))
                {
                    result.put(BuiltInRegistries.BLOCK.get(blockId), prototype);
                }
                else
                {
                    LOGGER.debug("Ignoring camo rotator for unknown block '{}'", blockId);
                }
            });

            // Removals run after additions
            for (ResourceLocation tagId : layer.removedTags())
            {
                for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(TagKey.create(Registries.BLOCK, tagId)))
                {
                    result.remove(holder.value());
                }
            }
            for (ResourceLocation blockId : layer.removedBlocks())
            {
                if (BuiltInRegistries.BLOCK.containsKey(blockId))
                {
                    result.remove(BuiltInRegistries.BLOCK.get(blockId));
                }
            }
        }
        return Map.copyOf(result);
    }

    private static final class CamoRotatorLoader implements SimpleSynchronousResourceReloadListener
    {
        @Override
        public ResourceLocation getFabricId()
        {
            return CAMO_ROTATORS_ID;
        }

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager)
        {
            List<RotatorLayer> layers = new ArrayList<>();
            // Packs are listed from lowest to highest priority
            resourceManager.listPacks().forEach(pack ->
            {
                for (String namespace : new TreeSet<>(pack.getNamespaces(PackType.SERVER_DATA)))
                {
                    ResourceLocation location = Utils.rl(namespace, CAMO_ROTATORS_FILE);
                    IoSupplier<InputStream> supplier = pack.getResource(PackType.SERVER_DATA, location);
                    if (supplier != null)
                    {
                        RotatorLayer layer = readLayer(pack, location, supplier);
                        if (layer != null)
                        {
                            layers.add(layer);
                        }
                    }
                }
            });
            rotatorLayers = List.copyOf(layers);
        }

        private static RotatorLayer readLayer(PackResources pack, ResourceLocation location, IoSupplier<InputStream> supplier)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)))
            {
                JsonObject root = GsonHelper.parse(reader);
                boolean replace = GsonHelper.getAsBoolean(root, "replace", false);

                Map<ResourceLocation, BlockCamoRotatorPrototype> blockValues = new LinkedHashMap<>();
                Map<ResourceLocation, BlockCamoRotatorPrototype> tagValues = new LinkedHashMap<>();
                if (root.has("values"))
                {
                    for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(root, "values").entrySet())
                    {
                        readValue(pack, location, entry.getKey(), entry.getValue(), blockValues, tagValues);
                    }
                }

                Set<ResourceLocation> removedBlocks = new TreeSet<>();
                Set<ResourceLocation> removedTags = new TreeSet<>();
                if (root.has("remove"))
                {
                    JsonElement remove = root.get("remove");
                    if (remove.isJsonArray())
                    {
                        for (JsonElement element : remove.getAsJsonArray())
                        {
                            readRemoval(pack, location, GsonHelper.convertToString(element, "remove entry"), removedBlocks, removedTags);
                        }
                    }
                    else
                    {
                        // Object form with additional remover data, which is not used by this data map
                        for (String key : GsonHelper.convertToJsonObject(remove, "remove").keySet())
                        {
                            readRemoval(pack, location, key, removedBlocks, removedTags);
                        }
                    }
                }

                return new RotatorLayer(replace, blockValues, tagValues, List.copyOf(removedBlocks), List.copyOf(removedTags));
            }
            catch (Exception e)
            {
                LOGGER.error("Failed to read camo rotator data map '{}' from pack '{}'", location, pack.packId(), e);
                return null;
            }
        }

        private static void readValue(
                PackResources pack,
                ResourceLocation location,
                String key,
                JsonElement element,
                Map<ResourceLocation, BlockCamoRotatorPrototype> blockValues,
                Map<ResourceLocation, BlockCamoRotatorPrototype> tagValues
        )
        {
            boolean tag = key.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? key.substring(1) : key);
            if (id == null)
            {
                LOGGER.error("Invalid key '{}' in camo rotator data map '{}' from pack '{}'", key, location, pack.packId());
                return;
            }

            // Entries are either the plain value or an object containing the value and a "replace" flag (which has no
            // effect here, entries always replace existing ones)
            JsonElement valueElement = element;
            if (element.isJsonObject())
            {
                JsonObject object = element.getAsJsonObject();
                if (!object.has("value"))
                {
                    LOGGER.error("Missing 'value' for key '{}' in camo rotator data map '{}' from pack '{}'", key, location, pack.packId());
                    return;
                }
                valueElement = object.get("value");
            }

            Map<ResourceLocation, BlockCamoRotatorPrototype> target = tag ? tagValues : blockValues;
            BlockCamoRotatorPrototype.CODEC.parse(JsonOps.INSTANCE, valueElement)
                    .ifSuccess(prototype -> target.put(id, prototype))
                    .ifError(error -> LOGGER.error(
                            "Failed to parse value for key '{}' in camo rotator data map '{}' from pack '{}': {}",
                            key, location, pack.packId(), error.message()
                    ));
        }

        private static void readRemoval(
                PackResources pack, ResourceLocation location, String key, Set<ResourceLocation> blocks, Set<ResourceLocation> tags
        )
        {
            boolean tag = key.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? key.substring(1) : key);
            if (id == null)
            {
                LOGGER.error("Invalid removal '{}' in camo rotator data map '{}' from pack '{}'", key, location, pack.packId());
                return;
            }
            (tag ? tags : blocks).add(id);
        }
    }

    private record RotatorLayer(
            boolean replace,
            Map<ResourceLocation, BlockCamoRotatorPrototype> blockValues,
            Map<ResourceLocation, BlockCamoRotatorPrototype> tagValues,
            List<ResourceLocation> removedBlocks,
            List<ResourceLocation> removedTags
    ) { }



    private FramedDataMaps() { }
}
