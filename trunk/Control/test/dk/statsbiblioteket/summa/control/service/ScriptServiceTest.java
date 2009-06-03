package dk.statsbiblioteket.summa.control.service;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.Serializable;

/**
 * Test cases for {@link ScriptService}
 */
public class ScriptServiceTest extends TestCase {

    Service service;
    Configuration conf;

    public void setUp() {
        System.setProperty("summa.control.service.id", "testService");
    }

    public void tearDown() {
        System.clearProperty("summa.control.service.id");
    }

    public static Service createScriptService(Serializable... args) {
        Configuration conf = Configuration.newMemoryBased(args);
        return Configuration.create(ScriptService.class, conf);
    }

    public void testEmptyInlineScript() throws Exception {
        service = createScriptService(ScriptService.CONF_SCRIPT_INLINE,
                                      "");
        assertEquals(Status.CODE.constructed, service.getStatus().getCode());
        service.start();
        assertNotSame(Status.CODE.constructed, service.getStatus().getCode());
        assertNotSame(Status.CODE.crashed, service.getStatus().getCode());

        service.stop();
        Thread.sleep(400); // Give a little bit o' grace time to change state
        assertEquals(Status.CODE.stopped, service.getStatus().getCode());
    }

    public void testSimpleInlineScript() throws Exception {
        service = createScriptService(ScriptService.CONF_SCRIPT_INLINE,
                                      "2 + 2");
        assertEquals(Status.CODE.constructed, service.getStatus().getCode());
        service.start();
        assertNotSame(Status.CODE.constructed, service.getStatus().getCode());
        assertNotSame(Status.CODE.crashed, service.getStatus().getCode());

        service.stop();
        Thread.sleep(400); // Give a little bit o' grace time to change state
        assertEquals(Status.CODE.stopped, service.getStatus().getCode());
    }

    public static void doTestBlockingScript(Service service)
                                                             throws Exception {
        assertEquals(Status.CODE.constructed, service.getStatus().getCode());
        service.start();
        Thread.sleep(200); // Let us sleep past the intermediate 'running' state
        assertEquals(Status.CODE.idle, service.getStatus().getCode());

        // Assert that the scrip indeed seems to be blocking
        Thread.sleep(3000);
        assertEquals(Status.CODE.idle, service.getStatus().getCode());

        service.stop();
        Thread.sleep(400); // Give a little bit o' grace time to change state
        assertEquals(Status.CODE.stopped, service.getStatus().getCode());
    }

    public void testInlineBlockingScript() throws Exception {
        service = createScriptService(ScriptService.CONF_SCRIPT_INLINE,
                                      "while (!stopped) {             \n" +
                                      "  java.lang.Thread.sleep(100); \n" +
                                      "}");
        doTestBlockingScript(service);
    }

    public void testExternalBlockingScript() throws Exception {
        service = createScriptService(ScriptService.CONF_SCRIPT_URL,
                                      "script-service-test.js");
        doTestBlockingScript(service);
    }
}
