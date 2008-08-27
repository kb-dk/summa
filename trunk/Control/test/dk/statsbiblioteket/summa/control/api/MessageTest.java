package dk.statsbiblioteket.summa.control.api;
/**
 * Unit tests for {@link Message} class
 */

import junit.framework.TestCase;

import java.util.Arrays;

import dk.statsbiblioteket.summa.control.api.feedback.Message;

public class MessageTest extends TestCase {
    Message message;
    String msgText;
    int msgType;
    char[] msgRaw;
    String respText;
    char[] respRaw;


    public void setUp () {
        msgText = "Hello world";
        msgRaw = new char[]{'r', 'a', 'w', ' ' , 'p', 'o', 'w', 'e', 'r'};
        msgType = Message.MESSAGE_ALERT;
        respText = "response text";
        respRaw = new char[]{'r', 'e', 's', 'p' , 'o', 'n', 's', 'e'}; 
        message = new Message(msgType, msgText);
    }

    public void testGetMessageType() throws Exception {
        assertEquals("Should return message type as passed to constructor",
                     message.getMessageType(), msgType);
    }

    public void testGetMessage() throws Exception {
        assertEquals("Should return message text as passed to constructor",
                     message.getMessage(), msgText);
    }

    public void testGetResponse() throws Exception {
        message.setResponse(respText);
        assertEquals("Message response should be the same as the one set",
                     message.getResponse(), respText);
    }

    public void testGetRawResponse() throws Exception {
        message.setRawResponse(respRaw);
        assertTrue("Raw message response should be the same as the one set",
                  Arrays.equals(respRaw, message.getRawResponse()));
    }

    /**
     * Getting a raw response from a String converts a String to a char array,
     * which should be tested.
     * @throws Exception
     */
    public void testGetRawResponseFromString() throws Exception {
        String testResp = new String(respRaw);
        message.setResponse(testResp);
        assertTrue("Raw message response should match the string response. "
                 + "Set " + Arrays.toString(respRaw) + ", but got "
                 + Arrays.toString(message.getRawResponse()),
                  Arrays.equals(respRaw, message.getRawResponse()));
    }
    
}