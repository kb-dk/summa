package dk.statsbiblioteket.summa.support.suggest;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * Test cases for {@link SuggestSearchNode}
 */
public class SuggestSearchNodeTest extends TestCase {

    Configuration conf;
    SuggestSearchNode node;

    public void setUp() throws Exception {

    }

    public void tearDown() throws Exception {

    }

    public void testEmptyConfigInstantiation() throws Exception {
        node = new SuggestSearchNode(Configuration.newMemoryBased());
    }

}
