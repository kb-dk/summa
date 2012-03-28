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
package dk.statsbiblioteket.summa.common.shell.notifications;

/**
 * Emitted when the user enters a malformed command line. This notification
 * does not map to a valid command hence {@link #getCommand} will return
 * {@code null}. 
 *
 */
public class SyntaxErrorNotification extends Notification {
    private static final long serialVersionUID =  12401241204821039L;
    public SyntaxErrorNotification(String msg) {
        super(msg);
    }
}




