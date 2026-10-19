package xfacthd.framedblocks.gametest.clienttest;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records every WARN, ERROR and FATAL log event together with its logger, message and throwable. The output of the
 * self-test (which logs through the {@code SelfTest} logger) is additionally recorded at all levels
 */
public final class LogCapture extends AbstractAppender
{
    public static final String HARNESS_LOGGER = "FBClientTest";
    private static final String SELF_TEST_LOGGER_SUFFIX = ".SelfTest";
    private static final String SELF_TEST_COMPLETE_MARKER = "Self test completed";
    private static final int MAX_TRACE_LINES = 12;
    private static final String OUR_PACKAGE = "xfacthd.framedblocks";
    private static final Pattern LOG_LINE = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[([^/\\]]+)/(WARN|ERROR|FATAL)] \\(([^)]*)\\) (.*)$");
    private static final String HARNESS_PACKAGE = "xfacthd.framedblocks.gametest.clienttest";
    private static LogCapture instance = null;

    private final List<LoggedEvent> events = Collections.synchronizedList(new ArrayList<>());
    private final List<String> selfTestLines = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger selfTestCompletions = new AtomicInteger();

    private LogCapture()
    {
        super("FramedBlocksClientTestCapture", null, null, true, Property.EMPTY_ARRAY);
    }

    public static synchronized void attach()
    {
        if (instance != null)
        {
            return;
        }

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        instance = new LogCapture();
        instance.start();
        config.addAppender(instance);
        config.getRootLogger().addAppender(instance, null, null);
        context.updateLoggers();
    }

    public static LogCapture get()
    {
        return instance;
    }

    @Override
    public void append(LogEvent event)
    {
        String loggerName = event.getLoggerName();
        if (HARNESS_LOGGER.equals(loggerName))
        {
            return;
        }

        boolean selfTest = loggerName != null && loggerName.endsWith(SELF_TEST_LOGGER_SUFFIX);
        boolean important = event.getLevel().isMoreSpecificThan(Level.WARN);
        if (!selfTest && !important)
        {
            return;
        }

        String message = event.getMessage().getFormattedMessage();
        if (selfTest)
        {
            selfTestLines.add(event.getLevel().name() + " " + message);
            if (message.contains(SELF_TEST_COMPLETE_MARKER))
            {
                selfTestCompletions.incrementAndGet();
            }
        }
        if (!important)
        {
            return;
        }

        List<String> trace = new ArrayList<>();
        boolean ourFrame = false;
        Throwable thrown = event.getThrown();
        int depth = 0;
        while (thrown != null && depth < 8)
        {
            trace.add((depth == 0 ? "" : "Caused by: ") + thrown);
            for (StackTraceElement element : thrown.getStackTrace())
            {
                String className = element.getClassName();
                if (className.startsWith(OUR_PACKAGE) && !className.startsWith(HARNESS_PACKAGE))
                {
                    ourFrame = true;
                }
                if (trace.size() < MAX_TRACE_LINES * (depth + 1))
                {
                    trace.add("    at " + element);
                }
            }
            thrown = thrown.getCause();
            depth++;
        }

        events.add(new LoggedEvent(event.getLevel().name(), loggerName, event.getThreadName(), message, trace, ourFrame));
    }

    public List<LoggedEvent> getEvents()
    {
        synchronized (events)
        {
            return new ArrayList<>(events);
        }
    }

    /**
     * Reads WARN and above events from the log file which were logged before the capture was attached and are
     * therefore missing from the captured events, as only the first line of each is available these have no trace
     */
    public List<LoggedEvent> readEarlyEvents(Path logFile)
    {
        List<LoggedEvent> early = new ArrayList<>();
        if (!Files.isRegularFile(logFile))
        {
            return early;
        }

        Set<String> known = new HashSet<>();
        getEvents().forEach(event -> known.add(firstLine(event.message())));
        try
        {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++)
            {
                Matcher matcher = LOG_LINE.matcher(lines.get(i));
                if (!matcher.matches() || matcher.group(3).equals(HARNESS_LOGGER))
                {
                    continue;
                }
                StringBuilder message = new StringBuilder(matcher.group(4));
                for (int j = i + 1; j < lines.size() && j <= i + MAX_TRACE_LINES && !lines.get(j).startsWith("["); j++)
                {
                    message.append("\n").append(lines.get(j).strip());
                }
                if (known.add(firstLine(message.toString())))
                {
                    early.add(new LoggedEvent(matcher.group(2), matcher.group(3), matcher.group(1), message.toString(), List.of(), false));
                }
            }
        }
        catch (IOException e)
        {
            // The log file is only a supplement, missing it must not break the report
        }
        return early;
    }

    private static String firstLine(String message)
    {
        int end = message.indexOf("\n");
        return end < 0 ? message : message.substring(0, end);
    }

    public List<String> getSelfTestLines()
    {
        synchronized (selfTestLines)
        {
            return new ArrayList<>(selfTestLines);
        }
    }

    public int getSelfTestCompletions()
    {
        return selfTestCompletions.get();
    }

    public record LoggedEvent(
            String level, String logger, String thread, String message, List<String> trace, boolean ourFrame
    )
    {
        public boolean isFromOurCode()
        {
            String lower = message.toLowerCase();
            return (logger != null && logger.startsWith(OUR_PACKAGE))
                    || ourFrame
                    || lower.contains("framedblocks_fabric")
                    || lower.contains(OUR_PACKAGE);
        }
    }
}
