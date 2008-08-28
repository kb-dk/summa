package dk.statsbiblioteket.summa.common.rpc;

import junit.framework.TestCase;

/**
 *
 */
public class RemoteHelperTest extends TestCase {

    public void testTestNotUrlCodePath () {
        String[] codePath = new String[] {"goo.jar"};

        try {
            RemoteHelper.testCodePath(codePath);
            fail ("Codepath is not a URL");
        } catch (RemoteHelper.InvalidCodePathException e) {
            System.out.println ("Error in codepath as expected: "
                                + e.getMessage());
        }
    }

    public void testTestNotJarCodePath () {
        String[] codePath = new String[] {"http://goo"};

        try {
            RemoteHelper.testCodePath(codePath);
            fail ("Codepath is a .jar");
        } catch (RemoteHelper.InvalidCodePathException e) {
            System.out.println ("Error in codepath as expected: "
                                + e.getMessage());
        }
    }

    public void testTestNonExistingCodePath () {
        String[] codePath = new String[] {"http://example.com/foobar.jar"};

        try {
            RemoteHelper.testCodePath(codePath);
            fail ("Codepath does not represent an existing file");
        } catch (RemoteHelper.InvalidCodePathException e) {
            System.out.println ("Error in codepath as expected: "
                                + e.getMessage());
        }
    }

}
