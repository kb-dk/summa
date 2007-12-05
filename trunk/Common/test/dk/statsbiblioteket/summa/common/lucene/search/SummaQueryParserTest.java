/* $Id: SummaQueryParserTest.java,v 1.2 2007/10/04 13:28:22 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.index.IndexDefaults;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.util.ArrayList;

/**
 * SummaQueryParser Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaQueryParserTest extends TestCase {


    SearchDescriptor descriptor;
    String[]  defaultFields;
    public SummaQueryParserTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        // add a group

        descriptor = new SearchDescriptor(File.createTempFile("search", "xml").getPath());
        IndexField.Alias a = new IndexField.Alias("fo", "da");
        ArrayList<IndexField.Alias> l = new ArrayList<IndexField.Alias>();
        l.add(a);
        descriptor.createGroup("au",l);
        // create a couple off fields on the group.

        IndexField f = new IndexField(new IndexDefaults());
        f.setName("mainAuthor"); f.addAlias(new IndexField.Alias("ffo","da"));
        descriptor.addFieldToGroup(f, "au");

        IndexField f2 = new IndexField(new IndexDefaults());
        f2.setName("secAuthor"); f2.addAlias(new IndexField.Alias("lfo","da"));
        descriptor.addFieldToGroup(f2, "au");


        //single field:
        IndexField f3 = new IndexField(new IndexDefaults());
        f3.setName("title"); f3.addAlias(new IndexField.Alias("titel","da"));
        descriptor.addUnGroupedField(f3);

        defaultFields = new String[]{"au","title"};


    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public void testParse() throws Exception{
        SummaQueryParser p = new SummaQueryParser(defaultFields, new SummaStandardAnalyzer(), descriptor);
        System.out.println(p.parse("Test search"));

    }


    public static Test suite() {
        return new TestSuite(SummaQueryParserTest.class);
    }
}
