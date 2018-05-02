package it.menzani.logger.api;

import it.menzani.logger.EvaluationException;
import it.menzani.logger.LogEntry;
import it.menzani.logger.impl.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractLogger implements Logger {
    private static final String API_MESSAGE_PREFIX = "[Logger] ";

    private final Set<Filter> filters = new HashSet<>();
    private Formatter formatter = new TimestampFormatter(LocalDateTime::now, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    private final Set<Consumer> consumers = new HashSet<>();

    {
        addConsumer(new ConsoleConsumer());
    }

    protected Set<Filter> getFilters() {
        return filters;
    }

    protected Set<Consumer> getConsumers() {
        return consumers;
    }

    public AbstractLogger setFilters(Filter... filters) {
        this.filters.clear();
        Collections.addAll(this.filters, filters);
        return this;
    }

    public AbstractLogger setFormatter(Formatter formatter) {
        this.formatter = formatter;
        return this;
    }

    public AbstractLogger setConsumers(Consumer... consumers) {
        this.consumers.clear();
        Collections.addAll(this.consumers, consumers);
        return this;
    }

    public AbstractLogger addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public AbstractLogger addConsumer(Consumer consumer) {
        consumers.add(consumer);
        return this;
    }

    public AbstractLogger withVerbosity(Level level) {
        addFilter(new LevelFilter(level));
        return this;
    }

    public AbstractLogger withDefaultVerbosity() {
        withVerbosity(StandardLevel.INFORMATION);
        return this;
    }

    public AbstractLogger disable() {
        addFilter(new RejectAllFilter());
        return this;
    }

    @Override
    public void trace(LazyMessage lazyMessage) {
        log(StandardLevel.TRACE, lazyMessage);
    }

    @Override
    public void debug(LazyMessage lazyMessage) {
        log(StandardLevel.DEBUG, lazyMessage);
    }

    @Override
    public void fine(LazyMessage lazyMessage) {
        log(StandardLevel.FINE, lazyMessage);
    }

    @Override
    public void info(LazyMessage lazyMessage) {
        log(StandardLevel.INFORMATION, lazyMessage);
    }

    @Override
    public void header(LazyMessage lazyMessage) {
        log(StandardLevel.HEADER, lazyMessage);
    }

    @Override
    public void warn(LazyMessage lazyMessage) {
        log(StandardLevel.WARNING, lazyMessage);
    }

    @Override
    public void fail(LazyMessage lazyMessage) {
        log(StandardLevel.FAILURE, lazyMessage);
    }

    @Override
    public void throwable(Throwable t, LazyMessage lazyMessage) {
        throwable(StandardLevel.FAILURE, t, lazyMessage);
    }

    @Override
    public void throwable(Level level, Throwable t, LazyMessage lazyMessage) {
        log(level, lazyMessage);
        log(level, () -> throwableToString(t));
    }

    @Override
    public void fatal(LazyMessage lazyMessage) {
        log(StandardLevel.FATAL, lazyMessage);
    }

    @Override
    public void log(Level level, LazyMessage lazyMessage) {
        doLog(new LogEntry(level, null, lazyMessage));
    }

    @Override
    public void trace(Object message) {
        log(StandardLevel.TRACE, message);
    }

    @Override
    public void debug(Object message) {
        log(StandardLevel.DEBUG, message);
    }

    @Override
    public void fine(Object message) {
        log(StandardLevel.FINE, message);
    }

    @Override
    public void info(Object message) {
        log(StandardLevel.INFORMATION, message);
    }

    @Override
    public void header(Object message) {
        log(StandardLevel.HEADER, message);
    }

    @Override
    public void warn(Object message) {
        log(StandardLevel.WARNING, message);
    }

    @Override
    public void fail(Object message) {
        log(StandardLevel.FAILURE, message);
    }

    @Override
    public void throwable(Throwable t, Object message) {
        throwable(StandardLevel.FAILURE, t, message);
    }

    @Override
    public void throwable(Level level, Throwable t, Object message) {
        log(level, message);
        log(level, throwableToString(t));
    }

    @Override
    public void fatal(Object message) {
        log(StandardLevel.FATAL, message);
    }

    @Override
    public void log(Level level, Object message) {
        doLog(new LogEntry(level, message, null));
    }

    static String throwableToString(Throwable t) {
        Writer writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    protected abstract void doLog(LogEntry entry);

    protected static Predicate<Filter> newFilterFunction(LogEntry entry) {
        return filter -> {
            try {
                return filter.reject(entry);
            } catch (Exception e) {
                printLoggerError(Filter.class, filter);
                e.printStackTrace();
                return true;
            }
        };
    }

    protected String doFormat(LogEntry entry) {
        try {
            return formatter.format(entry);
        } catch (EvaluationException e) {
            String levelMarker = entry.getLevel().getMarker();
            throwable(ReservedLevel.LOGGER, e.getCause(), "Could not evaluate lazy message at level: " + levelMarker);
        } catch (Exception e) {
            printLoggerError(Formatter.class, formatter);
            e.printStackTrace();
        }
        return null;
    }

    protected static java.util.function.Consumer<Consumer> newConsumerFunction(String entry, Level level) {
        return consumer -> {
            try {
                consumer.consume(entry, level);
            } catch (Exception e) {
                printLoggerError(Consumer.class, consumer);
                e.printStackTrace();
            }
        };
    }

    private static void printLoggerError(Class<?> apiClass, Object implObject) {
        System.err.println(API_MESSAGE_PREFIX + "Could not pass log entry to " +
                apiClass.getSimpleName() + ": " + implObject.getClass().getName());
    }
}
