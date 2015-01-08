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
package org.apache.lucene.util;

import org.apache.lucene.search.exposed.ExposedSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lucene works without external dependencies, so no real logging. To tie into Solr, which does use logging, this
 * class acts as a bridge.
 */
public class ELog {

  public static enum LEVEL {error, warn, info, debug, trace} // No fatal for slf4j-compatability

  private static final Map<String, ELog> logs = new HashMap<String, ELog>();

  public static ELog getLog(Class someClass) {
    return getLog(someClass.getCanonicalName());
  }

  public static synchronized ELog getLog(String logName) {
    ELog log = logs.get(logName);
    if (log == null) {
      log = new ELog(logName);
      logs.put(logName, log);
    }
    return log;
  }

  private static final List<LogListener> listeners = new ArrayList<LogListener>();
  public static synchronized boolean addListener(LogListener listener) {
    return listeners.add(listener);
  }
  public static synchronized boolean removeListener(LogListener listener) {
    return listeners.remove(listener);
  }
  public static synchronized void clearListeners() {
    listeners.clear();
  }
  
  public static void notify(String logName, LEVEL level, String message) {
    for (LogListener listener: listeners) {
      listener.message(logName, level, message);
    }
  }
  public static void notify(String logName, LEVEL level, String message, Throwable t) {
    for (LogListener listener: listeners) {
      listener.message(logName, level, message, t);
    }
  }

  private final String logName;
  public ELog(String logName) {
    this.logName = logName;
  }


  public void error(String message) {
    notify(logName, LEVEL.error, message);
  }
  public void error(String message, Throwable t) {
    notify(logName, LEVEL.error, message, t);
  }
  public void warn(String message) {
    notify(logName, LEVEL.warn, message);
  }
  public void warn(String message, Throwable t) {
    notify(logName, LEVEL.warn, message, t);
  }
  public void info(String message) {
    notify(logName, LEVEL.info, message);
  }
  public void info(String message, Throwable t) {
    notify(logName, LEVEL.info, message, t);
  }
  public void debug(String message) {
    notify(logName, LEVEL.debug, message);
  }
  public void debug(String message, Throwable t) {
    notify(logName, LEVEL.debug, message, t);
  }
  public void trace(String message) {
    notify(logName, LEVEL.trace, message);
  }
  public void trace(String message, Throwable t) {
    notify(logName, LEVEL.trace, message, t);
  }

  public static interface LogListener {
    void message(String logName, LEVEL level, String message);
    void message(String logName, LEVEL level, String message, Throwable t);
  }

  static {
    addListener(new LogListener() {
      @Override
      public void message(String logName, LEVEL level, String message) {
        if (ExposedSettings.debug) {
          System.out.println(logName + "(" + level + "): " + message);
        }
      }

      @Override
      public void message(String logName, LEVEL level, String message, Throwable t) {
        if (ExposedSettings.debug) {
          System.out.println(logName + "(" + level + "): " + message);
          t.printStackTrace();
        }
      }
    });
  }
}
