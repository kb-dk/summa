/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for doing conditional logging. Consider the case with a method
 * <code>
 *      private void setStatus (String status);
 * </code>
 * For convenience an implementation might want to log every time the status is
 * set. This will add two method calls per state change which clutters the code.
 * <br/>
 * With these logging methods you can implement {@code setStatus} like
 * <code>
 *      import org.apache.commons.logging.Log;
 *
 *      private void setStatus (String status, Log log, Logging.Level level) {
 *          this.status = status
 *          Logging.log ("Set status to " + status, log, level);
 *      }
 * </code>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, mke")
public class Logging {

    /**
     * The name of the process log: "process". Logging on trace-level will
     * provide a full dump of the Payload or Record in question for every
     * log message. Logging on debug-level will provide a full dump if a
     * warn-level message is given.
     */
    public static final String PROCESS_LOG_NAME = "process";
    public static final Log processLog = LogFactory.getLog(PROCESS_LOG_NAME);
    private static final int MAX_CONTENT = 1000;

    public static final String FATAL_LOG_NAME = "fatal";
    public static final Log fatalLog = LogFactory.getLog(FATAL_LOG_NAME);

    public static enum LogLevel {
        FATAL(6), ERROR(5), WARN(4), INFO(3), DEBUG(2), TRACE(1);
        private final int level;
        private LogLevel(int level) {
            this.level = level;
        }
        public int getLevel() {
            return level;
        }
    }

    /**
     * Special logging for process-related messages. Process-related messages
     * differ from normal messages by being related to specific Payloads or
     * Records, rather than the classes that handles these.
     * </p><p>
     * FATAL: Not used.<br />
     * ERROR: Not used.<br />
     * WARN:  When a Payload is discarded due to problems.<br />
     * INFO:  Not used.<br />
     * DEBUG: When a Payload is created.
     * TRACE: Different stages that the Payload has passed, such as the Filters
     *        that processed it, processing time etc.<br />
     * </p><p>
     * @param origin  where the message was from, e.g. "ClassName.methodName".
     * @param message the log message.
     * @param level   the log level.
     * @param payload the Payload related to the message.
     */
    public static void logProcess(String origin, String message, LogLevel level, Payload payload) {
        logProcess(origin, message, level, payload, null);
    }

    /**
     * Special logging for process-related messages. Process-related messages
     * differ from normal messages by being related to specific Payloads or
     * Records, rather than the classes that handles these.
     * </p><p>
     * FATAL: Not used.<br />
     * ERROR: Not used.<br />
     * WARN:  When a Payload is discarded due to problems.<br />
     * INFO:  Not used.<br />
     * DEBUG: When a Payload is created.
     * TRACE: Different stages that the Payload has passed, such as the Filters
     *        that processed it, processing time etc.<br />
     * </p><p>
     * @param origin  where the message was from, e.g. "ClassName.methodName".
     * @param message the log message.
     * @param level   the log level.
     * @param payload the Payload related to the message.
     * @param cause   what caused this message.
     */
    public static void logProcess(String origin, String message, LogLevel level, Payload payload, Throwable cause) {
        String fullMessage;
        if ((level == LogLevel.WARN) || isProcessLogLevel(LogLevel.DEBUG) || isProcessLogLevel(LogLevel.TRACE)) {
            String snippet = getContentSnippet(payload);
            fullMessage = (origin == null ? "" : origin + ": ") + message + ". " + payload + ". Content:"
                          + (snippet.length() <= 10 ? " " : "\n") + snippet;
        } else {
            fullMessage = (origin == null ? "" : origin + ": ") + message + ". " + payload;
        }

        if (cause == null) {
            log(fullMessage, processLog, level);
        } else {
            log(fullMessage, processLog, level, cause);
        }
    }

    /**
     * Special logging for process-related messages. Process-related messages
     * differ from normal messages by being related to specific Payloads or
     * Records, rather than the classes that handles these.
     * </p><p>
     * FATAL: Not used.<br />
     * ERROR: Not used.<br />
     * WARN:  When a Payload is discarded due to problems.<br />
     * INFO:  Not used.<br />
     * DEBUG: When a Payload is created.
     * TRACE: Different stages that the Payload has passed, such as the Filters
     *        that processed it, processing time etc.<br />
     * </p><p>
     * @param origin  where the message was from, e.g. "ClassName.methodName".
     * @param message the log message.
     * @param level   the log level.
     * @param record  the Record related to the message.
     * @param cause   what caused this message.
     */
    @SuppressWarnings("IfMayBeConditional")
    public static void logProcess(String origin, String message, LogLevel level, Record record, Throwable cause) {
        String fullMessage;
        if (level == LogLevel.WARN || isProcessLogLevel(LogLevel.DEBUG) || isProcessLogLevel(LogLevel.TRACE)) {
//        if ((level == LogLevel.WARN && isProcessLogLevel(LogLevel.DEBUG) || level == LogLevel.TRACE)) {
//        if ((level == LogLevel.WARN && isProcessLogLevel(LogLevel.DEBUG) || level == LogLevel.TRACE)) {
            fullMessage = (origin == null ? "" : origin + ": ") + message + ". " + record + ". Content:\n"
                          + getContentSnippet(record);
        } else {
            fullMessage = (origin == null ? "" : origin + ": ") + message + ". " + record;
        }

        if (cause == null) {
            log(fullMessage,processLog, level);
        } else {
            log(fullMessage,processLog, level, cause);
        }
    }

    private static String getContentSnippet(Payload payload) {
        if (payload == null || payload.getRecord() == null) {
            return "null";
        }
        return getContentSnippet(payload.getRecord());
    }

    private static String getContentSnippet(Record record) {
        return record == null ? "[No Record]" : getSnippet(record.getContentAsUTF8());
    }

    public static String getSnippet(String content) {
        return getSnippet(content, MAX_CONTENT);
    }

    public static String getSnippet(String content, int maxLength) {
        if (content == null) {
            return "[null]";
        }
        return content.length() > maxLength ? content.substring(0, maxLength) : content;
    }

    public static void logProcess(String origin, String message, LogLevel level, String id) {
        logProcess(origin, message, level, id, null);
    }

    public static void logProcess(String origin, String message, LogLevel level, String id, Throwable cause) {
        String fullMessage = (origin == null ? "" : origin + ": ") + message + ". " + id;

        if (cause == null) {
            log(fullMessage,processLog, level);
        } else {
            log(fullMessage,processLog, level, cause);
        }
    }

    /**
     * @param level is logging done on this level?
     * @return true if process logging is done on the stated level.
     */
    public static boolean isProcessLogLevel(LogLevel level) {
        switch (level) {
            case FATAL: return processLog.isFatalEnabled();
            case ERROR: return processLog.isErrorEnabled();
            case WARN:  return processLog.isWarnEnabled();
            case INFO:  return processLog.isInfoEnabled();
            case DEBUG: return processLog.isDebugEnabled();
            case TRACE: return processLog.isTraceEnabled();
            default: return true; // Better to log extra than miss messages
        }
    }

    public static void log (String message, Log log, LogLevel level) {
        switch (level) {
            case FATAL:
                log.fatal (message);
                break;
            case ERROR:
                log.error (message);
                break;
            case WARN:
                log.warn (message);
                break;
            case INFO:
                log.info (message);
                break;
            case DEBUG:
                log.debug (message);
                break;
            case TRACE:
                log.trace (message);
                break;
            default:
                log.info ("[Unknown log level " + level +"]: " + message);
        }
    }

    public static void log (String message, Log log, LogLevel level, Throwable cause) {
        switch (level) {
            case FATAL:
                log.fatal (message, cause);
                break;
            case ERROR:
                log.error (message, cause);
                break;
            case WARN:
                log.warn (message, cause);
                break;
            case INFO:
                log.info (message, cause);
                break;
            case DEBUG:
                log.debug (message, cause);
                break;
            case TRACE:
                log.trace (message, cause);
                break;
            default:
                log.info ("[Unknown log level " + level +"]: " + message, cause);
        }
    }

    public static void log (Throwable cause, Log log, LogLevel level) {
        switch (level) {
            case FATAL:
                log.fatal (cause);
                break;
            case ERROR:
                log.error (cause);
                break;
            case WARN:
                log.warn (cause);
                break;
            case INFO:
                log.info (cause);
                break;
            case DEBUG:
                log.debug (cause);
                break;
            case TRACE:
                log.trace (cause);
                break;
            default:
                log.info ("[Unknown log level " + level +"]", cause);
        }
    }

    /**
     * All fatal logging should be done through a fatal method as they ensures that the right logger is used ("fatal").
     * @param origin  the sender of the message. This is normally "Class.method".
     * @param message the message to log.
     */
    public static void fatal(String origin, String message) {
        fatal(null, origin, message, null);
    }

    /**
     * All fatal logging should be done through a fatal method as they ensures that the right logger is used ("fatal").
     * @param copy    the message is also logged to this logger.
     * @param origin  the sender of the message. This is normally "Class.method".
     * @param message the message to log.
     */
    public static void fatal(Log copy, String origin, String message) {
        fatal(copy, origin, message, null);
    }

    /**
     * All fatal logging should be done through a fatal method as they ensures that the right logger is used ("fatal").
     * @param origin  the sender of the message. This is normally "Class.method".
     * @param message the message to log.
     * @param cause   the cause of the problem.
     */
    public static void fatal(String origin, String message, Throwable cause) {
        fatal(null, origin, message, cause);
    }

    /**
     * All fatal logging should be done through a fatal method as they ensures that the right logger is used ("fatal").
     * @param copy    the message is also logged to this logger.
     * @param origin  the sender of the message. This is normally "Class.method".
     * @param message the message to log.
     * @param cause   the cause of the problem.
     */
    public static void fatal(Log copy, String origin, String message, Throwable cause) {
        String problem = (origin == null ? "" : origin + ": ") + message;
        if (cause == null) {
            fatalLog.fatal(problem);
            if (copy != null) {
                copy.fatal(problem);
            }
        } else {
            fatalLog.fatal(problem, cause);
            if (copy != null) {
                copy.fatal(problem, cause);
            }
        }
    }

    /**
     * Helper for doing conditional logging with format Strings. The level of the given logger is compared to the wanted
     * logLevel and a String.format is called on message with the given values iff the resulting message String will be
     * logged by the logger.
     * </p><p>
     * As long as the resolving of the values is computationally cheap, this method is recommended for logging.
     * @param logger   where to log.
     * @param logLevel the level to log on.
     * @param message  a {@link String#format}-syntax compatible format String.
     * @param values   will be passed as {@code }String.format(message, values)}.
     */
    public static void log(Log logger, LogLevel logLevel, String message, Object... values) {
        log(logger, logLevel, null, message, values);
    }

    /**
     * Helper for doing conditional logging with format Strings. The level of the given logger is compared to the wanted
     * logLevel and a String.format is called on message with the given values iff the resulting message String will be
     * logged by the logger.
     * </p><p>
     * As long as the resolving of the values is computationally cheap, this method is recommended for logging.
     * @param logger   where to log.
     * @param logLevel the level to log on.
     * @param cause    passed on to logger.
     * @param message  a {@link String#format}-syntax compatible format String.
     * @param values   will be passed as {@code }String.format(message, values)}.
     */
    public static void log(Log logger, LogLevel logLevel, Throwable cause, String message, Object... values) {
        if (logger.isTraceEnabled()
            || (logger.isDebugEnabled() && logLevel.getLevel() >= LogLevel.DEBUG.getLevel())
            || (logger.isInfoEnabled() && logLevel.getLevel() >= LogLevel.INFO.getLevel())
            || (logger.isWarnEnabled() && logLevel.getLevel() >= LogLevel.WARN.getLevel())
            || (logger.isErrorEnabled() && logLevel.getLevel() >= LogLevel.ERROR.getLevel())
            || (logger.isFatalEnabled() && logLevel.getLevel() >= LogLevel.FATAL.getLevel())) {
            String mes;
            try {
                mes = values == null || values.length == 0 ? message : String.format(message, values);
            } catch (Exception e) {
                logger.warn("Logging.log: Unable to use format String '" + message + "' with values ["
                            + (values == null ? "null" : Strings.join(values, ", ")) + "]. "
                            + "Using unformatted message", e);
                mes = message;
            }
            if (cause == null) {
                log(mes, logger, logLevel);
            } else {
                log(mes, logger, logLevel, cause);
            }
        }
    }
}
