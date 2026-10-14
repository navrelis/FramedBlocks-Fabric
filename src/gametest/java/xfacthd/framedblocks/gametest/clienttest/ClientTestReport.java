package xfacthd.framedblocks.gametest.clienttest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects the results of the client smoke test and writes {@code report.md} and {@code result.json}
 */
public final class ClientTestReport
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int MAX_EXCERPT_LINES = 60;

    private final Instant start = Instant.now();
    private final AtomicBoolean written = new AtomicBoolean();
    private final Map<String, String> timings = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, String> environment = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, String> modelOutputs = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<String> skipped = Collections.synchronizedList(new ArrayList<>());
    private final List<String> screenshots = Collections.synchronizedList(new ArrayList<>());
    private final List<String> notes = Collections.synchronizedList(new ArrayList<>());
    private final List<String> failures = Collections.synchronizedList(new ArrayList<>());
    private final List<String> crashReports = Collections.synchronizedList(new ArrayList<>());
    private volatile String phase = "startup";
    private volatile String renderer = "unknown";
    private volatile Verdict finalVerdict = Verdict.FAIL;

    public void setPhase(String phase)
    {
        this.phase = phase;
    }

    public String phase()
    {
        return phase;
    }

    public void setRenderer(String renderer)
    {
        this.renderer = renderer;
    }

    public void environment(String key, String value)
    {
        environment.put(key, value);
    }

    public void timing(String key, String value)
    {
        timings.put(key, value);
    }

    public void skipped(String entry)
    {
        skipped.add(entry);
    }

    public void screenshot(String fileName)
    {
        screenshots.add(fileName);
    }

    public void note(String note)
    {
        notes.add(note);
    }

    public void failure(String failure)
    {
        failures.add(failure);
    }

    public void modelOutput(String name, String content)
    {
        modelOutputs.put(name, content);
    }

    public void crashReport(String name)
    {
        crashReports.add(name);
    }

    public boolean isWritten()
    {
        return written.get();
    }

    public Verdict finalVerdict()
    {
        return finalVerdict;
    }

    /**
     * @return {@code true} if the report was written by this call, {@code false} if it had already been written
     */
    public boolean write(Verdict requested, Path dir, Path logFile)
    {
        if (!written.compareAndSet(false, true))
        {
            return false;
        }

        LogCapture capture = LogCapture.get();
        List<LogCapture.LoggedEvent> events = new ArrayList<>();
        if (capture != null)
        {
            events.addAll(capture.readEarlyEvents(logFile));
            events.addAll(capture.getEvents());
        }
        List<String> selfTestLines = capture != null ? capture.getSelfTestLines() : List.of();
        int selfTestRuns = capture != null ? capture.getSelfTestCompletions() : 0;

        List<LogCapture.LoggedEvent> ours = new ArrayList<>();
        List<LogCapture.LoggedEvent> others = new ArrayList<>();
        List<LogCapture.LoggedEvent> benign = new ArrayList<>();
        for (LogCapture.LoggedEvent event : events)
        {
            if (isKnownBenign(event))
            {
                benign.add(event);
            }
            else if (event.isFromOurCode())
            {
                ours.add(event);
            }
            else
            {
                others.add(event);
            }
        }
        List<String> selfTestFindings = selfTestLines.stream().filter(line -> !line.startsWith("INFO")).toList();

        Verdict verdict = requested;
        List<String> reasons = new ArrayList<>(failures);
        if (verdict == Verdict.PASS)
        {
            if (!ours.isEmpty())
            {
                reasons.add(ours.size() + " WARN+ log event(s) from FramedBlocks code");
            }
            if (!crashReports.isEmpty())
            {
                reasons.add(crashReports.size() + " crash report(s) written during the run");
            }
            long fatalCount = events.stream().filter(event -> event.level().equals("FATAL")).count();
            if (fatalCount > 0)
            {
                reasons.add(fatalCount + " FATAL log event(s), the game most likely crashed");
            }
            if (!selfTestFindings.isEmpty())
            {
                reasons.add(selfTestFindings.size() + " self-test finding(s)");
            }
            if (!reasons.isEmpty())
            {
                verdict = Verdict.FAIL;
            }
        }
        finalVerdict = verdict;

        try
        {
            Files.createDirectories(dir);
            Duration duration = Duration.between(start, Instant.now());
            Data data = new Data(verdict, reasons, duration, ours, others, benign, selfTestLines, selfTestFindings, selfTestRuns);
            Files.writeString(dir.resolve("report.md"), buildMarkdown(data));
            Files.writeString(dir.resolve("result.json"), GSON.toJson(buildJson(data)));
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to write client test report", e);
        }
        return true;
    }

    private static boolean isKnownBenign(LogCapture.LoggedEvent event)
    {
        String message = event.message();
        String lower = message.toLowerCase();
        if (lower.contains("modmenu") || lower.contains("mod menu"))
        {
            return true;
        }
        if (message.contains("Reference map") && message.contains("forgeconfigapiport"))
        {
            return true;
        }
        return message.startsWith("Error loading class:") && (
                message.contains("ca/spottedleaf/starlight")
                        || message.contains("com/electronwill/nightconfig/core/io/IoUtils")
                        || message.contains("de/siphalor/amecs")
        );
    }

    private String buildMarkdown(Data data)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("# FramedBlocks client smoke test\n\n");
        sb.append("**Result: ").append(data.verdict).append("**\n\n");
        sb.append("- Renderer: ").append(renderer).append('\n');
        sb.append("- Duration: ").append(data.duration.toSeconds()).append(" s\n");
        sb.append("- Last phase: ").append(phase).append('\n');
        sb.append("- Started: ").append(start).append("\n");
        synchronized (environment)
        {
            environment.forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append('\n'));
        }
        if (!data.reasons.isEmpty())
        {
            sb.append("\n## Failure reasons\n\n");
            data.reasons.forEach(reason -> sb.append("- ").append(reason).append('\n'));
        }

        sb.append("\n## Timings\n\n");
        synchronized (timings)
        {
            if (timings.isEmpty())
            {
                sb.append("none recorded\n");
            }
            timings.forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append('\n'));
        }

        sb.append("\n## Skipped blocks (").append(skipped.size()).append(")\n\n");
        appendList(sb, skipped);

        sb.append("\n## Self-test (").append(data.selfTestRuns).append(" completed run(s) seen)\n\n");
        sb.append("Findings (WARN and above): ").append(data.selfTestFindings.size()).append("\n\n");
        appendList(sb, data.selfTestFindings);
        sb.append("\n<details><summary>Full self-test log</summary>\n\n```\n");
        data.selfTestLines.forEach(line -> sb.append(line).append('\n'));
        sb.append("```\n\n</details>\n");

        sb.append("\n## Model command output\n\n");
        synchronized (modelOutputs)
        {
            if (modelOutputs.isEmpty())
            {
                sb.append("none\n");
            }
            modelOutputs.forEach((name, content) ->
                    sb.append("### ").append(name).append("\n\n```\n").append(excerpt(content)).append("\n```\n\n")
            );
        }

        sb.append("\n## Log events from FramedBlocks code (").append(data.ours.size()).append(")\n\n");
        appendEvents(sb, data.ours);
        sb.append("\n## Other log events (").append(data.others.size()).append(")\n\n");
        appendEvents(sb, data.others);
        sb.append("\n## Known benign log events (").append(data.benign.size()).append(")\n\n");
        appendEvents(sb, data.benign);

        sb.append("\n## Crash reports (").append(crashReports.size()).append(")\n\n");
        appendList(sb, crashReports);

        sb.append("\n## Screenshots (").append(screenshots.size()).append(")\n\n");
        appendList(sb, screenshots);

        sb.append("\n## Harness notes\n\n");
        appendList(sb, notes);
        return sb.toString();
    }

    private JsonObject buildJson(Data data)
    {
        JsonObject root = new JsonObject();
        root.addProperty("result", data.verdict.name());
        root.addProperty("renderer", renderer);
        root.addProperty("durationSeconds", data.duration.toSeconds());
        root.addProperty("lastPhase", phase);
        root.add("failureReasons", toArray(data.reasons));
        root.add("environment", toObject(environment));
        root.add("timings", toObject(timings));
        root.add("skippedBlocks", toArray(skipped));
        root.addProperty("selfTestRuns", data.selfTestRuns);
        root.add("selfTestFindings", toArray(data.selfTestFindings));
        root.add("selfTestLog", toArray(data.selfTestLines));
        root.add("modelOutputs", toObject(modelOutputs));
        root.add("logEventsOurCode", eventsToJson(data.ours));
        root.add("logEventsOther", eventsToJson(data.others));
        root.add("logEventsKnownBenign", eventsToJson(data.benign));
        root.add("crashReports", toArray(crashReports));
        root.add("screenshots", toArray(screenshots));
        root.add("notes", toArray(notes));
        return root;
    }

    private static JsonArray toArray(List<String> list)
    {
        JsonArray array = new JsonArray();
        synchronized (list)
        {
            list.forEach(array::add);
        }
        return array;
    }

    private static JsonObject toObject(Map<String, String> map)
    {
        JsonObject obj = new JsonObject();
        synchronized (map)
        {
            map.forEach(obj::addProperty);
        }
        return obj;
    }

    private static JsonArray eventsToJson(List<LogCapture.LoggedEvent> events)
    {
        JsonArray array = new JsonArray();
        for (LogCapture.LoggedEvent event : events)
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("level", event.level());
            obj.addProperty("logger", event.logger());
            obj.addProperty("thread", event.thread());
            obj.addProperty("message", event.message());
            obj.add("trace", toArray(event.trace()));
            array.add(obj);
        }
        return array;
    }

    private static void appendList(StringBuilder sb, List<String> list)
    {
        synchronized (list)
        {
            if (list.isEmpty())
            {
                sb.append("none\n");
            }
            list.forEach(entry -> sb.append("- ").append(entry).append('\n'));
        }
    }

    private static void appendEvents(StringBuilder sb, List<LogCapture.LoggedEvent> events)
    {
        if (events.isEmpty())
        {
            sb.append("none\n");
            return;
        }
        for (LogCapture.LoggedEvent event : events)
        {
            sb.append("- **").append(event.level()).append("** `").append(event.logger()).append("` [")
                    .append(event.thread()).append("] ").append(event.message().replace("\n", "\n  ")).append('\n');
            if (!event.trace().isEmpty())
            {
                sb.append("  ```\n");
                event.trace().forEach(line -> sb.append("  ").append(line).append('\n'));
                sb.append("  ```\n");
            }
        }
    }

    private static String excerpt(String content)
    {
        String[] lines = content.split("\n");
        if (lines.length <= MAX_EXCERPT_LINES)
        {
            return content;
        }
        return String.join("\n", List.of(lines).subList(0, MAX_EXCERPT_LINES)) +
                "\n... (" + (lines.length - MAX_EXCERPT_LINES) + " more lines, see the copied file)";
    }

    public enum Verdict
    {
        PASS,
        FAIL,
        TIMEOUT
    }

    private record Data(
            Verdict verdict,
            List<String> reasons,
            Duration duration,
            List<LogCapture.LoggedEvent> ours,
            List<LogCapture.LoggedEvent> others,
            List<LogCapture.LoggedEvent> benign,
            List<String> selfTestLines,
            List<String> selfTestFindings,
            int selfTestRuns
    ) { }
}
