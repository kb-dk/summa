/* $Id: SummaQueryParserTest.java,v 1.11 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.11 $
 * $Date: 2007/10/11 12:56:24 $
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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.FreeTextAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;

/**
 * SummaQueryParser Tester.
 * @deprecated as part of refactoring and upgrade to the new IndexDescriptor.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, kfc")
public class SummaQueryParserTest extends TestCase {

    public SummaQueryParserTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SummaQueryParserTest.class);
    }

    public void testParse() throws ParseException {

        String[] queries = {"+au:(\"foo bar\" barfoo) -boo:({53 TO 67} \"it's all too easy\")",
                "single",
                "au:Andersen -ti:\"Den grimme Ælling\""};

        SearchDescriptor d = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
        d.loadDescription("/home/findex/persistentFiles/summa_full_index");

        // TODO: Implement this
/*        SummaQueryParser p = new SummaQueryParser(new String[]{"foo", "bar"}, new SimpleAnalyzer(), d, true);
        Query query;
        for (String q : queries){
            System.out.println(q);
            query = p.parse(q);
            assertNotNull("Error parsing" + q);
            System.out.println(query.toString());

            System.out.println(query.toString());

        }


  */
    }

    public void testQuotes() throws ParseException {
        String input = "author_normalised:\"Koontz, Dean\"";
        String output = "author_normalised:Koontz, Dean";

        SearchDescriptor descriptor = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
        descriptor.loadDescription("/home/findex/persistentFiles/summa_full_index");

        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
                new SummaStandardAnalyzer());
        analyzer = new PerFieldAnalyzerWrapper(new SummaStandardAnalyzer());
        for (OldIndexField field : descriptor.getSingleFields()) {
            analyzer.addAnalyzer(field.getName(), field.getType().getAnalyzer());
        }
        for (SearchDescriptor.Group g : descriptor.getGroups().values()) {
            for (OldIndexField field : g.getFields()) {
                analyzer.addAnalyzer(field.getName(), field.getType().getAnalyzer());
            }
        }
        // TODO: Implement this
/*
        SummaQueryParser p = new SummaQueryParser(new String[]{}, analyzer, descriptor, true);
        System.out.println("Input: " + input);
        Query query = p.parse(input);
        System.out.println("Output: " + query);
        assertEquals("Expect correct output", output, query.toString());
        */
    }
    public void testParan() throws ParseException {
        String input = "(author_normalised:\"Koontz, Dean\")";
        String output = "author_normalised:Koontz, Dean";

        SearchDescriptor d = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
        d.loadDescription("/home/findex/persistentFiles/summa_full_index");

        SearchDescriptor descriptor = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
        descriptor.loadDescription("/home/findex/persistentFiles/summa_full_index");

        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
                new SummaStandardAnalyzer());
        analyzer = new PerFieldAnalyzerWrapper(new SummaStandardAnalyzer());
        for (OldIndexField field : descriptor.getSingleFields()) {
            analyzer.addAnalyzer(field.getName(), field.getType().getAnalyzer());
        }
        for (SearchDescriptor.Group g : descriptor.getGroups().values()) {
            for (OldIndexField field : g.getFields()) {
                analyzer.addAnalyzer(field.getName(), field.getType().getAnalyzer());
            }
        }
        // TODO: Implement this
/*
        SummaQueryParser p = new SummaQueryParser(new String[]{}, analyzer, descriptor, true);
        Query query = p.parse(input);
        assertEquals("Expect correct output", output, query.toString());
        */
    }

    public void testQueryParser() throws ParseException {
        String input = "au:jensen";
        String output = "author_person:jensen author_main:jensen author_corporation:jensen au_other:jensen";

        SearchDescriptor d = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
        d.loadDescription("/home/findex/persistentFiles/summa_full_index");

        SearchDescriptor descriptor = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
        descriptor.loadDescription("/home/findex/persistentFiles/summa_full_index");

        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
                new SummaStandardAnalyzer());
        analyzer = new PerFieldAnalyzerWrapper(new SummaStandardAnalyzer());
        for (OldIndexField field : descriptor.getSingleFields()) {
            analyzer.addAnalyzer(field.getName(), field.getType().getAnalyzer());
        }
        for (SearchDescriptor.Group g : descriptor.getGroups().values()) {
            for (OldIndexField field : g.getFields()) {
                analyzer.addAnalyzer(field.getName(), field.getType().getAnalyzer());
            }
        }
        // TODO: Implement this
/*
        SummaQueryParser p = new SummaQueryParser(new String[]{}, analyzer, descriptor, true);
        Query query = p.parse(input);
        assertEquals("Expect correct output", output, query.toString());
        */
    }

    public void testQueryDouble() throws ParseException {
        String input = "nature";
        String output = "+(author_person:jensen author_main:jensen author_corporation:jensen au_other:jensen author_normalized:jensen subject_dk5:jensen subject_other:jensen subject_controlled:jensen su_lc:jensen lsu_oai:jensen su_dk:jensen su_corp:jensen mesh:jensen su_pe:jensen main_titel:jensen title:jensen se:jensen freetext:jensen) +(author_person:hej author_main:hej author_corporation:hej au_other:hej author_normalized:hej subject_dk5:hej subject_other:hej subject_controlled:hej su_lc:hej lsu_oai:hej su_dk:hej su_corp:hej mesh:hej su_pe:hej main_titel:hej title:hej se:hej freetext:hej)";

//        SearchDescriptor d = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
//        d.loadDescription("/home/findex/persistentFiles/summa_full_index");

//        SearchDescriptor descriptor = new SearchDescriptor("/home/findex/persistentFiles/summa_full_index");
//        descriptor.loadDescription("/home/findex/persistentFiles/summa_full_index");
//
//        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
//                new SummaStandardAnalyzer());
//        analyzer = new PerFieldAnalyzerWrapper(new SummaStandardAnalyzer());
//        for (OldIndexField field : descriptor.getSingleFields()) {
//            analyzer.addAnalyzer(field.getId(), field.getType().getAnalyzer());
//        }
//        for (SearchDescriptor.Group g : descriptor.getGroups().values()) {
//            for (OldIndexField field : g.getFields()) {
//                analyzer.addAnalyzer(field.getId(), field.getType().getAnalyzer());
//            }
//        }
        SearchEngineImpl engine = SearchEngineImpl.getInstance();

        SummaQueryParser p = engine.getSummaQueryParser();
        Query query = p.parse(input);
        assertEquals("Expect correct output", output, query.toString());
    }

    public void testFreetext() throws IOException {
        FreeTextAnalyzer ana = new FreeTextAnalyzer();
        Reader r = new StringReader("Monatshefte für Mathematik und Physik");
        TokenStream st = ana.tokenStream("freetext", r);
        Token t;
        while ((t = st.next()) != null){
            System.out.print(t.termText() + " ");
        }
    }

    public void testKeyWord() throws Exception{
        SearchEngineImpl engine = SearchEngineImpl.getInstance();

        SummaQueryParser p = engine.getSummaQueryParser();
        Query q = p.parse("author_normalised:\"Ball, Kenneth\"");
        System.out.println(q.toString());
        q = p.parse("C++");
        System.out.println(q.toString());
        assertEquals("", "hdk", "kskl");
    }


}