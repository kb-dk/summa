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
        author = "mke")
public final class Logging {
    /**
     * Private constructor to make sure there are not instances of this class.
     */
    private Logging() { }

    /**
     * The name of the process log: "process". Logging on trace-level will
     * provide a full dump of the Payload or Record in question for every
     * log message. Logging on debug-level will provide a full dump if a
     * warn-level message is given.
     */
    public static final String PROCESS_LOG_NAME = "process";
    /** Process log logger instance. */
    private static final Log PROCESS_LOG = LogFactory.getLog(PROCESS_LOG_NAME);
    /** Maximal content size. */
    private static final int MAX_CONTENT = 1000;

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
     * @param origin  Where the message was from, e.g. "ClassName.methodName".
     * @param message The log message.
     * @param level   The log level.
     * @param payload The Payload related to the message.
     */
    public static void logProcess(final String origin, final String message,
                                  final LogLevel level, final Payload payload) {
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
     * @param origin  Where the message was from, e.g. "ClassName.methodName".
     * @param message The log message.
     * @param level   The log level.
     * @param payload the Payload related to the message.
     * @param cause   What caused this message.
     */
    public static void logProcess(final String origin, final String message,
                                  final LogLevel level, final Payload payload,
                                  final Throwable cause) {
        String fullMessage;
        if ((level == LogLevel.WARN && isProcessLogLevel(LogLevel.DEBUG)
             || level == LogLevel.TRACE)) {
            fullMessage = (origin == null ? "" : origin + ": ") + message + ". "
                          + payload + ". Content:\n"
                          + getContentSnippet(payload);
        } else {
            fullMessage = (origin == null ? "" : origin + ": ") + message + ". "
                          + payload;
        }

        if (cause == null) {
            log(fullMessage, PROCESS_LOG, level);
        } else {
            log(fullMessage, PROCESS_LOG, level, cause);
        }
    }

    /**
     * Get content snippet.
     * @param payload The Payload.
     * @return Content snippet.
     */
    private static String getContentSnippet(final Payload payload) {
        if (payload == null || payload.getRecord() == null) {
            return "null";
        }
        String content = payload.getRecord().getContentAsUTF8();
        return content.length() > MAX_CONTENT ?
        content.substring(0, MAX_CONTENT) : content;
    }

    /**
     * Creates a process log entry.
     * @param origin The origin of the log.
     * @param message The message.
     * @param level The log level.
     * @param id A id.
     */
    public static void logProcess(final String origin, final String message,
                                  final LogLevel level, final String id) {
        logProcess(origin, message, level, id, null);
    }

    /**
     * Creates a process log entry.
     * @param origin The origin of the log.
     * @param message The message.
     * @param level The log level.
     * @param id The log id.
     * @param cause The cause.
     */
    public static void logProcess(final String origin, final String message,
                                  final LogLevel level, final String id,
                                  final Throwable cause) {
        String fullMessage = (origin == null ? "" : origin + ": ")
                             + message + ". " + id;

        if (cause == null) {
            log(fullMessage, PROCESS_LOG, level);
        } else {
            log(fullMessage, PROCESS_LOG, level, cause);
        }
    }

    /**
     * @param level Is logging done on this level?
     * @return True if process logging is done on the stated level.
     */
    public static boolean isProcessLogLevel(final LogLevel level) {
        switch (level) {
            case FATAL: return PROCESS_LOG.isFatalEnabled();
            case ERROR: return PROCESS_LOG.isErrorEnabled();
            case WARN:  return PROCESS_LOG.isWarnEnabled();
            case INFO:  return PROCESS_LOG.isInfoEnabled();
            case DEBUG: return PROCESS_LOG.isDebugEnabled();
            case TRACE: return PROCESS_LOG.isTraceEnabled();
            default: return true; // Better to log extra than miss messages
        }
    }

    /**
     * Enum class for log level.
     */
    public static enum LogLevel {
        /** Log levels. */
        FATAL, ERROR, WARN, INFO, DEBUG, TRACE
    }

    /**
     * Logs a message to the appropriated log level.
     * @param message The message.
     * @param log The log to write to.
     * @param level The level.
     */
    public static void log(final String message, final Log log,
                           final LogLevel level) {
        switch (level) {
            case FATAL:
                log.fatal(message);
                break;
            case ERROR:
                log.error(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case INFO:
                log.info(message);
                break;
            case DEBUG:
                log.debug(message);
                break;
            case TRACE:
                log.trace(message);
                break;
            default:
                log.info("[Unknown log level " + level +"]: " + message);
        }
    }

    /**
     * Logs a message to the appropriated log level.
     * @param message The message.
     * @param log The log to write to.
     * @param level The level.
     * @param cause The log cause.
     */
    public static void log(final String message, final Log log,
                           final LogLevel level, final Throwable cause) {
        switch (level) {
            case FATAL:
                log.fatal(message, cause);
                break;
            case ERROR:
                log.error(message, cause);
                break;
            case WARN:
                log.warn(message, cause);
                break;
            case INFO:
                log.info(message, cause);
                break;
            case DEBUG:
                log.debug(message, cause);
                break;
            case TRACE:
                log.trace(message, cause);
                break;
            default:
                log.info("[Unknown log level " + level + "]: " + message,
                         cause);
        }
    }

    /**
     * Logs a message to the appropriated log level.
     * @param log The log to write to.
     * @param level The level.
     * @param cause The log cause.
     */
    public static void log(final Throwable cause, final Log log,
                           final LogLevel level) {
        switch (level) {
            case FATAL:
                log.fatal(cause);
                break;
            case ERROR:
                log.error(cause);
                break;
            case WARN:
                log.warn(cause);
                break;
            case INFO:
                log.info(cause);
                break;
            case DEBUG:
                log.debug(cause);
                break;
            case TRACE:
                log.trace(cause);
                break;
            default:
                log.info("[Unknown log level " + level + "]", cause);
        }
    }
}
