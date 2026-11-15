package xfacthd.framedblocks.gametest.clienttest;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xfacthd.framedblocks.api.camo.block.SimpleBlockCamoContainer;
import xfacthd.framedblocks.api.util.CamoList;
import xfacthd.framedblocks.api.util.FramedConstants;
import xfacthd.framedblocks.client.screen.FramingSawScreen;
import xfacthd.framedblocks.client.screen.PoweredFramingSawScreen;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.crafting.FramingSawRecipe;
import xfacthd.framedblocks.common.crafting.FramingSawRecipeCache;
import xfacthd.framedblocks.common.menu.FramingSawMenu;
import xfacthd.framedblocks.common.net.payload.ServerboundSelectFramingSawRecipePayload;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Uses the Framing Saw and the Powered Framing Saw like a player in a real client: opens both screens by right click,
 * moves every item of the mod into the input and additive slots with number key swaps through the network (with client
 * side prediction), selects recipes the way the screen and the JEI/REI/EMI transfer handlers do, and renders the
 * tooltips of all recipe buttons and of the status line while doing so. A failing action or a crash while rendering
 * fails the run
 */
public final class SawChecks
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCapture.HARNESS_LOGGER);
    private static final int ORIGIN_OFFSET_X = 40;
    private static final int HOTBAR_SLOT = 0;
    private static final int EMPTY_HAND_SLOT = 8;
    private static final int INVENTORY_MENU_HOTBAR_START = 36;
    private static final int WAIT_TIMEOUT_TICKS = 200;
    private static final int RECIPES_X = 48;
    private static final int RECIPES_Y = 22;
    private static final int RECIPE_SIZE = 18;
    private static final int RECIPE_COLS = 8;
    private static final int RECIPE_ROWS = 6;

    private final Minecraft mc = Minecraft.getInstance();
    private final ClientTestReport report;
    private final Consumer<String> failHandler;
    private final Runnable doneHandler;
    private final List<String> problems = new ArrayList<>();
    private final Method renderTooltip;
    private final Field leftPos;
    private final Field topPos;
    private final Field statusTooltipArea;

    private State state = State.INIT;
    private CompletableFuture<BlockPos> pending = null;
    private BlockPos origin = null;
    private Kind kind = Kind.FRAMING_SAW;
    private List<Runnable> actions = List.of();
    private int actionIndex = 0;
    private int stateTicks = 0;
    private int hoverIndex = 0;
    private int baseRecipe = -1;

    public SawChecks(ClientTestReport report, Consumer<String> failHandler, Runnable doneHandler)
    {
        this.report = report;
        this.failHandler = failHandler;
        this.doneHandler = doneHandler;
        try
        {
            renderTooltip = AbstractContainerScreen.class.getDeclaredMethod("renderTooltip", GuiGraphics.class, int.class, int.class);
            renderTooltip.setAccessible(true);
            leftPos = AbstractContainerScreen.class.getDeclaredField("leftPos");
            leftPos.setAccessible(true);
            topPos = AbstractContainerScreen.class.getDeclaredField("topPos");
            topPos.setAccessible(true);
            statusTooltipArea = PoweredFramingSawScreen.class.getDeclaredField("statusTooltipArea");
            statusTooltipArea.setAccessible(true);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Saw checks can't access the screen internals", e);
        }
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
        {
            if (screen instanceof FramingSawScreen || screen instanceof PoweredFramingSawScreen)
            {
                ScreenEvents.afterRender(screen).register(this::afterRender);
            }
        });
    }

    public void step(boolean fromTick)
    {
        if (!fromTick || state == State.DONE)
        {
            return;
        }
        stateTicks++;
        switch (state)
        {
            case INIT -> init();
            case WAIT_BLOCKS -> waitBlocks();
            case OPEN -> open();
            case RUN -> run();
            case DONE -> { }
        }
    }

    // region states
    private void init()
    {
        if (pending == null)
        {
            pending = new CompletableFuture<>();
            mc.getSingleplayerServer().execute(() ->
            {
                try
                {
                    pending.complete(prepareOnServer());
                }
                catch (Throwable t)
                {
                    LOGGER.error("Saw checks could not prepare the saws", t);
                    pending.completeExceptionally(t);
                    failHandler.accept("Saw checks could not prepare the saws: " + t);
                }
            });
            return;
        }
        if (pending.isDone() && !pending.isCompletedExceptionally())
        {
            origin = pending.join();
            enter(State.WAIT_BLOCKS);
        }
    }

    private BlockPos prepareOnServer()
    {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX() + ORIGIN_OFFSET_X, spawn.getZ());
        BlockPos base = new BlockPos(spawn.getX() + ORIGIN_OFFSET_X, surface, spawn.getZ());
        for (int dx = -2; dx <= 4; dx++)
        {
            for (int dz = -2; dz <= 4; dz++)
            {
                level.setBlockAndUpdate(base.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                for (int dy = 0; dy < 4; dy++)
                {
                    level.setBlockAndUpdate(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(base, FBContent.BLOCK_FRAMING_SAW.value().defaultBlockState());
        level.setBlockAndUpdate(base.east(2), FBContent.BLOCK_POWERED_FRAMING_SAW.value().defaultBlockState());

        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        player.teleportTo(level, base.getX() + 1.5, base.getY(), base.getZ() + 2.5, 180F, 45F);
        return base;
    }

    private void waitBlocks()
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }
        boolean present = mc.level.getBlockState(origin).is(FBContent.BLOCK_FRAMING_SAW.value()) &&
                mc.level.getBlockState(origin.east(2)).is(FBContent.BLOCK_POWERED_FRAMING_SAW.value());
        if (present && stateTicks > 20)
        {
            enter(State.OPEN);
        }
        else if (stateTicks > WAIT_TIMEOUT_TICKS)
        {
            failHandler.accept("Saw checks: the client did not receive the saw blocks");
            state = State.DONE;
        }
    }

    private void open()
    {
        if (stateTicks == 1)
        {
            mc.setScreen(null);
            setHotbar(EMPTY_HAND_SLOT, ItemStack.EMPTY);
            mc.player.getInventory().selected = EMPTY_HAND_SLOT;
            return;
        }
        if (stateTicks == 3)
        {
            BlockPos pos = kind == Kind.FRAMING_SAW ? origin : origin.east(2);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            return;
        }
        if (stateTicks > 3 && isExpectedScreen(mc.screen))
        {
            actions = buildActions();
            actionIndex = 0;
            enter(State.RUN);
        }
        else if (stateTicks > WAIT_TIMEOUT_TICKS)
        {
            failHandler.accept("Saw checks: right click did not open the " + kind + " screen, screen is " + mc.screen);
            state = State.DONE;
        }
    }

    private void run()
    {
        if (!isExpectedScreen(mc.screen))
        {
            problems.add(kind + ": screen closed unexpectedly after action " + actionIndex + ", screen is " + mc.screen);
            nextKindOrFinish();
            return;
        }
        if (actionIndex >= actions.size())
        {
            nextKindOrFinish();
            return;
        }
        Runnable action = actions.get(actionIndex++);
        try
        {
            action.run();
        }
        catch (Throwable t)
        {
            LOGGER.error("Saw check action {} of {} threw", actionIndex - 1, kind, t);
            problems.add(kind + ": action " + (actionIndex - 1) + " threw " + t);
        }
    }

    private void nextKindOrFinish()
    {
        report.note("Saw checks " + kind + ": " + actions.size() + " actions run");
        mc.setScreen(null);
        if (kind == Kind.FRAMING_SAW)
        {
            kind = Kind.POWERED_FRAMING_SAW;
            enter(State.OPEN);
            return;
        }
        state = State.DONE;
        mc.player.getInventory().selected = 0;
        if (problems.isEmpty())
        {
            LOGGER.info("Saw checks: OK");
            report.note("Saw checks: OK");
        }
        else
        {
            for (String problem : problems)
            {
                LOGGER.error("Saw checks: FAILED, {}", problem);
                report.failure("Saw checks: " + problem);
            }
        }
        doneHandler.run();
    }

    private void enter(State next)
    {
        state = next;
        stateTicks = 0;
    }
    // endregion

    // region actions
    private List<Runnable> buildActions()
    {
        List<RecipeHolder<FramingSawRecipe>> recipes = FramingSawRecipeCache.get(true).getRecipes();
        if (recipes.isEmpty())
        {
            problems.add(kind + ": the client recipe cache is empty");
            return List.of();
        }

        // A recipe that one framed cube does not divide into evenly, the saw then reports MATERIAL_LCM for one cube
        baseRecipe = 0;
        for (int i = 0; i < recipes.size(); i++)
        {
            FramingSawRecipe recipe = recipes.get(i).value();
            int amount = recipe.getMaterialAmount();
            if (recipe.getAdditives().isEmpty() && amount < FramingSawRecipe.CUBE_MATERIAL_VALUE && FramingSawRecipe.CUBE_MATERIAL_VALUE % amount != 0)
            {
                baseRecipe = i;
                break;
            }
        }

        List<Runnable> list = new ArrayList<>();
        ItemStack cube = new ItemStack(FBContent.BLOCK_FRAMED_CUBE.value());
        list.add(() -> setHotbar(HOTBAR_SLOT, cube.copy()));
        list.add(() -> swap(FramingSawMenu.SLOT_INPUT));
        list.add(() -> selectRecipe(baseRecipe));

        int rotation = 0;
        for (ItemStack stack : testStacks())
        {
            int recipe = rotation % recipes.size();
            int additive = FramingSawMenu.SLOT_ADDITIVE_FIRST + (rotation % FramingSawRecipe.MAX_ADDITIVE_COUNT);
            rotation += 7;

            list.add(() -> setHotbar(HOTBAR_SLOT, stack.copy()));
            // The base state is one framed cube in the input with a recipe the cube doesn't satisfy
            list.add(() -> swap(FramingSawMenu.SLOT_INPUT));
            list.add(() -> selectRecipe(recipe));
            list.add(() -> swap(FramingSawMenu.SLOT_INPUT));
            list.add(() -> selectRecipe(baseRecipe));
            list.add(() -> swap(additive));
            list.add(() -> swap(additive));
        }
        return list;
    }

    private List<ItemStack> testStacks()
    {
        List<ItemStack> stacks = new ArrayList<>();
        CamoList camo = CamoList.of(new SimpleBlockCamoContainer(Blocks.STONE.defaultBlockState(), FBContent.FACTORY_BLOCK.value()));
        int index = 0;
        for (Item item : BuiltInRegistries.ITEM)
        {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FramedConstants.MOD_ID))
            {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            stacks.add(stack.copyWithCount(stack.getMaxStackSize()));
            // Single items and camo stacks only for every 8th item and the framed cube to keep the run short
            if (index++ % 8 != 0 && item != FBContent.BLOCK_FRAMED_CUBE.value().asItem())
            {
                continue;
            }
            if (stack.getMaxStackSize() > 1)
            {
                stacks.add(stack.copy());
            }
            ItemStack camoStack = stack.copy();
            camoStack.set(FBContent.DC_TYPE_CAMO_LIST.value(), camo);
            stacks.add(camoStack);
        }
        return stacks;
    }

    private void setHotbar(int slot, ItemStack stack)
    {
        mc.player.getInventory().setItem(slot, stack);
        mc.gameMode.handleCreativeModeItemAdd(stack, INVENTORY_MENU_HOTBAR_START + slot);
    }

    private void swap(int sawSlot)
    {
        AbstractContainerMenu menu = mc.player.containerMenu;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, sawSlot, HOTBAR_SLOT, ClickType.SWAP, mc.player);
    }

    private void selectRecipe(int idx)
    {
        // Same as the recipe buttons of the screen and the recipe transfer handlers of JEI, REI and EMI
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu.clickMenuButton(mc.player, idx))
        {
            ClientPlayNetworking.send(new ServerboundSelectFramingSawRecipePayload(menu.containerId, idx));
        }
        if (mc.screen instanceof FramingSawScreen screen)
        {
            // Move through all recipe pages over time
            int left = getInt(leftPos, screen);
            int top = getInt(topPos, screen);
            screen.mouseScrolled(left + RECIPES_X + 1, top + RECIPES_Y + 1, 0, (actionIndex % 80) < 40 ? -1 : 1);
        }
    }
    // endregion

    // region tooltip rendering
    private void afterRender(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        if (state != State.RUN || !isExpectedScreen(screen))
        {
            return;
        }
        int left = getInt(leftPos, screen);
        int top = getInt(topPos, screen);
        List<int[]> points = new ArrayList<>();
        if (screen instanceof FramingSawScreen)
        {
            for (int row = 0; row < RECIPE_ROWS; row++)
            {
                for (int col = 0; col < RECIPE_COLS; col++)
                {
                    points.add(new int[] { left + RECIPES_X + col * RECIPE_SIZE + 9, top + RECIPES_Y + row * RECIPE_SIZE + 9 });
                }
            }
            // Loose additive warning
            points.add(new int[] { left + 28, top + 54 });
        }
        else
        {
            try
            {
                Rect2i area = (Rect2i) statusTooltipArea.get(screen);
                if (area.getWidth() > 0)
                {
                    points.add(new int[] { area.getX() + 1, area.getY() + 1 });
                }
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException(e);
            }
            points.add(new int[] { left + 10, top + 30 });
        }
        if (points.isEmpty())
        {
            return;
        }
        int[] point = points.get(hoverIndex++ % points.size());
        try
        {
            renderTooltip.invoke(screen, graphics, point[0], point[1]);
        }
        catch (InvocationTargetException e)
        {
            // Let it propagate like an exception from the screen's own tooltip rendering
            if (e.getCause() instanceof RuntimeException re)
            {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private boolean isExpectedScreen(Screen screen)
    {
        return kind == Kind.FRAMING_SAW ? screen instanceof FramingSawScreen : screen instanceof PoweredFramingSawScreen;
    }

    private static int getInt(Field field, Object owner)
    {
        try
        {
            return field.getInt(owner);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }
    // endregion

    private enum State
    {
        INIT,
        WAIT_BLOCKS,
        OPEN,
        RUN,
        DONE
    }

    private enum Kind
    {
        FRAMING_SAW,
        POWERED_FRAMING_SAW
    }
}
