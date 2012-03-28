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

/**
 * Exception used when requesting sub configuration from a
 * {@link dk.statsbiblioteket.summa.common.configuration.Configuration} that
 * does not support sub configurations.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class SubConfigurationsNotSupportedException extends Exception {
    public static final long serialVersionUID = 68748318356L;
    private String msg;
    private Throwable e;

    /**
     * Constructor for SubConfigurationsNotSupportedException.
     * @param msg The error message of this exception.
     */
    public SubConfigurationsNotSupportedException(String msg) {
        this.msg = msg;
    }

    /**
     * Constructor for SubConfigurationsNotSupportedException.
     * @param msg The error message for this exception.
     * @param e The {@link Throwable} which resulted in this exception being
     * thrown.
     */
    public SubConfigurationsNotSupportedException(String msg, Throwable e) {
        this.msg = msg;
        this.e = e;
    }

    /**
     * Getter method for the error message.
     * @return The error message.
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Return the throwable attached to this exception.
     * @return The throwable attached to this exception.
     */
    public Throwable getE() {
        return e;
    }

    /**
     * Pretty print this exception.
     * @return String describing this object.
     */
    @Override
    public String toString() {
        return msg + "\n" + e.getCause();
    }
}
