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
package dk.statsbiblioteket.summa.control.api.feedback;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;

/**
 * A Message is used by Feedback and consists of the message itself and an
 * optional request for a response.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Message implements Serializable {
    /**
     * Plain messages should just be displayed and requires no response.
     */
    public static final int MESSAGE_PLAIN = 0;
    /**
     * Alerts should be presented more prominently than plain messages and
     * does not require a response.
     */
    public static final int MESSAGE_ALERT = 1;
    /**
     * The message requires a user response.
     */
    public static final int MESSAGE_REQUEST = 2;
    /**
     * The message requires a user response that should not be presented on
     * screen. This could for example be a password.
     */
    public static final int MESSAGE_SECRET_REQUEST = 3;

    private int messageType;
    private String message;
    private Object response = null;

    /**
     * Construct a Message object with the given type and message.
     * @param messageType controls how the feedback should be presented to the
     *                    user and how responses should be given.
     * @param message     the textual message.
     */
    // TODO: Consider default value(s)
    public Message(int messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }

    public int getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }

    public String getResponse() {
        return response.toString();
    }

    /**
     * Used mainly to retrieve the response value from sensitive questions
     * so as password prompts. A byte array is used here to avoid having
     * a copy of passwords or other secrets in the string pool.
     * @return
     */
    public char[] getRawResponse () {
        if (response instanceof char[]) {
            return (char[]) response;
        }
        // response is a string, extract a char array
        String respString = response.toString();
        char[] chars = new char[respString.length()];
        response.toString().getChars(0, chars.length, chars, 0);
        return chars;
    }

    /**
     * Errors during response-input should be handled with an error message
     * and be setting the response to null.
     * @param response the response to the message, if requested. Null in
     *                 case of errors.
     */
    public void setResponse(String response) {
        this.response = response;
    }

    /**
     * Used mainly to set the response for sensitive informations such as
     * password prompts. Using raw byte arrays makes sure we don't end up with
     * a copy of the password in the string pool.
     * @param response the response to the message, if requested. Null in
     *                 case of errors.
     */
    public void setRawResponse (char[] response) {
        this.response = response;
    }

    /**
     * Return the message string of this Message object
     * @return same as {@link #getMessage()} 
     */
    public String toString () {
        return message;
    }
}




