package dk.statsbiblioteket.summa.common.lucene.search;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

public class DisjunctionQueryParserTest extends TestCase {
    public DisjunctionQueryParserTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(DisjunctionQueryParserTest.class);
    }

/*    public void testBasic() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
            IndexDescriptor.CONF_ABSOLUTE_LOCATION, "/home/"**
            IndexDescriptor.CO
        );
        LuceneIndexDescriptor id = new LuceneIndexDescriptor(conf);
        DisjunctionQueryParser qp = new DisjunctionQueryParser(id);
    } */
}
