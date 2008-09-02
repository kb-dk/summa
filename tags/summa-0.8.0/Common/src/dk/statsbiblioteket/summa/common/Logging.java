/* $Id: Logging.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common;

import org.apache.commons.logging.Log;
import dk.statsbiblioteket.util.qa.QAInfo;

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
public class Logging {
    public static enum LogLevel {
        FATAL, ERROR, WARN, INFO, DEBUG, TRACE
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
}
