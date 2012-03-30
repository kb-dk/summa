package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;

import java.util.List;
import java.util.Map;

public class FacetQueryTransformerTest extends TestCase {
    public FacetQueryTransformerTest(String name) {
        super(name);
    }

    private FacetQueryTransformer fqt = new FacetQueryTransformer(Configuration.newMemoryBased());

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(FacetQueryTransformerTest.class);
    }

    public void testTrivial() throws ParseException {
        assertConvert("", "");
        assertConvert("foo:bar",         "foo,bar,false");
        assertConvert("foo:bar zoo:moo", "foo,bar,false zoo,moo,false");
    }

    public void testTrivialNegative() throws ParseException {
        assertConvert("-foo:bar",          "foo,bar,true");
        assertConvert("-foo:bar -zoo:moo", "foo,bar,true zoo,moo,true");
        assertConvert("-foo:bar zoo:moo",  "foo,bar,true zoo,moo,false");
        assertConvert("foo:bar -zoo:moo",  "foo,bar,false zoo,moo,true");
    }

    public void testSubGroup() throws ParseException {
        assertConvert("foo:bar -(zoo:moo OR bil:ted)", "foo,bar,false zoo,moo,true bil,ted,true");
        assertConvert("-(zoo:moo OR bil:ted) foo:bar", "zoo,moo,true bil,ted,true foo,bar,false");
        assertConvert("foo:bar (zoo:moo bil:ted)",    "foo,bar,false zoo,moo,false bil,ted,false");
        assertConvert("(zoo:moo bil:ted) foo:bar",     "zoo,moo,false bil,ted,false foo,bar,false");
    }

    public void testNonconforming() throws ParseException {
        assertConvert("foo:bar -(zoo:moo bil:ted)", null);
        assertConvert("foo:bar (zoo:moo OR bil:ted)", null);
    }


    public void testRealWorldExamples() throws ParseException {
        assertConvert("(-(ContentType:\"Book / eBook\" OR ContentType:\"Book\" OR ContentType:\"eBook\" OR "
                      + "ContentType:\"Microform\" OR ContentType:\"Microfilm\")"
                      + " -Language:\"Portuguese\")"
                      + " -ContentType:\"Newspaper Article\"",
                      "ddd");
    }
    private void assertConvert(String query, String expected) throws ParseException {
        Map<String, List<String>> processed = fqt.convertQueryToFacet(query);
        List<String> calls = processed == null ? null : processed.get("s.fvf");
        if (processed == null && expected != null) {
            fail("Transformation gave a result '" + processed + "' while null was expected");
        }
        if (calls == null) {
            if (!("".equals(expected) || expected == null)) {
                fail("Expected '" + expected + "' from query '" + query + "' but got no result");
            }
            return;
        }
        String actual = Strings.join(calls, " ");
        assertEquals("Conversion of '" + query + "' should work as expected", expected, actual);
    }
}
