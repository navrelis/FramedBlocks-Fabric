package xfacthd.framedblocks.gametest.cmdtests;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.gametest.cmdtests.client.ModelPerformanceTest;
import xfacthd.framedblocks.gametest.cmdtests.tests.*;
import xfacthd.framedblocks.platform.Platform;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class SpecialTestCommand
{
    public static final String ROOT_NAME = "fbtest";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu_MM_dd-kk_mm_ss");
    private static final Path EXPORT_DIR = Path.of("./logs/test");

    public static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            @SuppressWarnings("unused") Commands.CommandSelection environment
    )
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                .then(Commands.literal("skippredicates")
                        .then(Commands.literal("errors")
                                .executes(async(
                                        SkipPredicateErrors.NAME, SkipPredicateErrors::testSkipPredicates
                                ))
                        )
                        .then(Commands.literal("consistency")
                                .executes(async(
                                        SkipPredicateConsistency.NAME, SkipPredicateConsistency::testSkipPredicates
                                ))
                        )
                        .then(Commands.literal("redundancy")
                                .executes(async(
                                        SkipPredicateRedundancy.NAME, SkipPredicateRedundancy::testSkipPredicates
                                ))
                        )
                )
                .then(Commands.literal("recipecollision")
                        .executes(async(RecipeCollisions.NAME, RecipeCollisions::checkForRecipeCollisions))
                )
                .then(Commands.literal("recipepresent")
                        .executes(RecipePresent::checkForRecipePresence)
                )
                .then(Commands.literal("chunkban")
                        .then(Commands.argument("confirm", StringArgumentType.string())
                                .then(Commands.argument("state", BlockStateArgument.block(buildContext))
                                        .executes(ctx -> ChunkBanTest.startChunkBanTest(ctx, true))
                                )
                                .executes(ctx-> ChunkBanTest.startChunkBanTest(ctx, false))
                        )
                )
                .then(Commands.literal("stateinfo")
                        .executes(BlockStateInfo::dumpBlockStateInfo)
                );

        if (Platform.isClient())
        {
            root.then(Commands.literal("modelperf")
                    .executes(async(ModelPerformanceTest.NAME, ModelPerformanceTest::testModelPerformance))
            );
        }

        dispatcher.register(root);
    }

    private static Command<CommandSourceStack> async(String testName, AsyncCommand cmd)
    {
        return ctx ->
        {
            ctx.getSource().sendSuccess(() -> Component.literal("Starting " + testName + " test"), false);

            Consumer<Component> appender = msg -> ctx.getSource().getServer().submit(() ->
                    ctx.getSource().sendSystemMessage(msg)
            );
            runGuardedOffThread(testName, appender, () -> cmd.run(ctx, appender));

            return Command.SINGLE_SUCCESS;
        };
    }

    public static void runGuardedOffThread(String testName, Consumer<Component> msgQueueAppender, Runnable test)
    {
        Util.backgroundExecutor().submit(() ->
        {
            try
            {
                test.run();
            }
            catch (Throwable t)
            {
                msgQueueAppender.accept(Component.literal(
                        "Encountered an uncaught error while testing " + testName + ". See log for details"
                ));
                FramedBlocks.LOGGER.error("Encountered an error while testing {}", testName, t);
            }
        });
    }

    public static Component writeResultToFile(String filePrefix, String data)
    {
        return writeResultToFile(filePrefix, "txt", data);
    }

    public static Component writeResultToFile(String filePrefix, String fileType, String data)
    {
        try
        {
            Files.createDirectories(EXPORT_DIR);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        String dateTime = FORMATTER.format(LocalDateTime.now());
        Path path = EXPORT_DIR.resolve("%s_%s.%s".formatted(filePrefix, dateTime, fileType));

        try
        {
            Files.writeString(path, data, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String pathText = path.toAbsolutePath().normalize().toString();
            Component pathComponent = Component.literal(pathText).withStyle(style ->
                    style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, pathText))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy the path")))
                            .applyFormat(ChatFormatting.UNDERLINE)
            );
            return Component.literal("Tests results exported to ").append(pathComponent);
        }
        catch (IOException e)
        {
            FramedBlocks.LOGGER.error("Encountered an error while exporting test results", e);
            return Component.literal("Export of test results failed with error: %s: %s".formatted(
                    e.getClass().getSimpleName(), e.getMessage()
            ));
        }
    }

    public interface AsyncCommand
    {
        void run(CommandContext<CommandSourceStack> ctx, Consumer<Component> msgQueueAppender);
    }



    private SpecialTestCommand() { }
}
