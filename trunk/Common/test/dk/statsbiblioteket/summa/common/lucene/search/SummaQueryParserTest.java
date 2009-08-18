/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.lucene.search;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Profiler;

import java.io.IOException;
import java.util.Random;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class SummaQueryParserTest extends TestCase {
    public SummaQueryParserTest(String name) {
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
        return new TestSuite(SummaQueryParserTest.class);
    }

    public void testGroupExpansion() throws Exception {
        SummaQueryParser qp = getQueryParser();
        assertEquals("The parsed query should expand default groups", qp,
              "(freetext:foo[1.0] <title:foo[1.0] titel:foo[1.0]> id:foo[1.0])",
               "foo");
    }

    public void testIndexDescriptorBoost() throws Exception {
        SummaQueryParser qp = getQueryParser();
        assertEquals("The parsed query should boost author", qp,
                     "author:foo^1.5[1.5]", "author:foo");
        assertEquals("The author boost should be a multiple of boosts", qp,
                     "author:foo^3.0[3.0]", "author:foo boost(author^2)");
    }

    public void testGroupBoost() throws Exception {
        SummaQueryParser qp = getQueryParser();
        assertEquals("The parsed query should boost groups", qp,
      "(freetext:foo[1.0] <title:foo^3.0[3.0] titel:foo^3.0[3.0]> id:foo[1.0])",
               "foo boost(ti^3)");
        assertEquals("The parsed query should boost explicit field", qp,
                     "id:foo^3.0[3.0]",
                     "id:foo boost(id^3)");
    }

    public void testWildcards() throws Exception {
        SummaQueryParser qp = getQueryParser();
        assertEquals("Wildcards on default fields should work", qp,
  "(freetext:hello*[1.0] <title:hello*[1.0] titel:hello*[1.0]> id:hello*[1.0])",
  "Hello*");
        assertEquals("Wildcards on a specific field should work", qp,
                     "foo:bar*[1.0]", "foo:bar*");
    }

    public void testRangeExpansion() throws Exception {
        SummaQueryParser qp = getQueryParser();
        assertEquals("Range query on specific field should work", qp,
                     "foo:[a TO c][1.0]", "foo:[a TO c]");
        assertEquals("Range query on default fields should work", qp,
                     "(freetext:[a TO c][1.0] <title:[a TO c][1.0] "
                     + "titel:[a TO c][1.0]> id:[a TO c][1.0])",
                     "[a TO c]");
    }

    public void testRangeAndBoost() throws Exception {
        SummaQueryParser qp = getQueryParser();
        assertEquals("Boosting on range queries should have no effect", qp,
                     "(freetext:[a TO c][1.0] <title:[a TO c][1.0] "
                     + "titel:[a TO c][1.0]> id:[a TO c][1.0])",
                     "[a TO c] boost(freetext^2)");
    }

    public void testDefaultExpansionSpeed() throws Exception {
        SummaQueryParser qp = getQueryParser();
        Random random = new Random(87);
        int RUNS = 5000;
                         // ~250 q/s
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(RUNS);
        for (int i = 0 ; i < RUNS ; i++) {
            new String(Character.toChars((char)(
                    'a' + random.nextInt(25))));
            profiler.beat();
        }
        System.out.println("Dry-run generated " + RUNS + " queries at "
                           + profiler.getBps(false) + " q/sec");

        random = new Random(87);
        profiler.reset();
        for (int i = 0 ; i < RUNS ; i++) {
            qp.parse(new String(Character.toChars((char)(
                    'a' + random.nextInt(25)))));
            profiler.beat();
        }
        System.out.println("Processed " + RUNS + " queries at "
                           + profiler.getBps(false) + " q/sec");
    }

    public void assertEquals(String message, SummaQueryParser qp,
                             String expected, String query) throws Exception {
        assertEquals(message, expected,
                     SummaQueryParser.queryToString(qp.parse(query)));
    }

    private SummaQueryParser getQueryParser() throws IOException {
        LuceneIndexDescriptor descriptor = new LuceneIndexDescriptor(
                       Resolver.getURL("data/queryparser/IndexDescriptor.xml"));
        Configuration conf = Configuration.newMemoryBased();
        conf.set(SummaQueryParser.CONF_QUERY_TIME_FIELD_BOOSTS, true);
        return new SummaQueryParser(conf, descriptor);
    }

}
