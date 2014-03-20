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
package org.apache.solr.exposed;

import org.apache.lucene.util.ELog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridges the Lucene parts of Exposed to the standard logging system in the Solr setup.
 */
public class ExposedLogBridge implements ELog.LogListener {
  private final Map<String, Logger> logs = new HashMap<String, Logger>();

  public ExposedLogBridge() {
    ELog.addListener(this);
  }

  @Override
  public void message(String logName, ELog.LEVEL level, String message) {
    Logger log;
    synchronized (logs) {
      log = logs.get(logName);
      if (log == null) {
        log = LoggerFactory.getLogger(logName);
        logs.put(logName, log);
      }
    }
    switch (level) {
      case error:
        log.error(logName + ": " + message);
        break;
      case info:
        log.info(logName + ": " + message);
        break;
      case warn:
        log.warn(logName + ": " + message);
        break;
      case debug:
        log.debug(logName + ": " + message);
        break;
      case trace:
        log.trace(logName + ": " + message);
        break;
      default:
        log.error("Unknown log level '" + level + "' for message '" + message + "'");
    }
  }

  @Override
  public void message(String logName, ELog.LEVEL level, String message, Throwable t) {
    Logger log;
    synchronized (logs) {
      log = logs.get(logName);
      if (log == null) {
        log = LoggerFactory.getLogger(logName);
        logs.put(logName, log);
      }
    }
    switch (level) {
      case error:
        log.error(logName + ": " + message, t);
        break;
      case info:
        log.info(logName + ": " + message, t);
        break;
      case warn:
        log.warn(logName + ": " + message, t);
        break;
      case debug:
        log.debug(logName + ": " + message, t);
        break;
      case trace:
        log.trace(logName + ": " + message, t);
        break;
      default:
        log.error("Unknown log level '" + level + "' for message '" + message + "'", t);
    }
  }
}
