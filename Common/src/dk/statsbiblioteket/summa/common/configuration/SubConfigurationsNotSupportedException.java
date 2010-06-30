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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.util.qa.QAInfo;
import sun.reflect.generics.reflectiveObjects.*;

/**
 * Exception used when requesting sub configuration from a storage that dosn't
 * support this.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class SubConfigurationsNotSupportedException extends Exception {
    public static final long serialVersionUID = 68748318356L;
    private String msg;
    private Throwable e;

    public SubConfigurationsNotSupportedException(String msg) {
        this.msg = msg;
    }

    public SubConfigurationsNotSupportedException(String msg, Throwable e) {
        this.msg = msg;
        this.e = e;
    }

    public String getMsg() {
        return msg;
    }

    public Throwable getE() {
        return e;
    }

    public String toString() {
        return msg + "\n" + e.getCause();
    }
}
