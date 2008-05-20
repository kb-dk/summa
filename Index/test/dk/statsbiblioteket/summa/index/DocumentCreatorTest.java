package dk.statsbiblioteket.summa.index;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocumentCreatorTest extends TestCase {
    public DocumentCreatorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(DocumentCreatorTest.class);
    }

    public void testSimpleTransformation() throws Exception {
        
    }
}
