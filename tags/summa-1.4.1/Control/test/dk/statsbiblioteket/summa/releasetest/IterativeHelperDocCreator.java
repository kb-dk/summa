/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.releasetest;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * A filter used by {@link IterativeTest} that takes a rudimentary Record
 * and converts it to a test Document.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class IterativeHelperDocCreator extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(IterativeHelperDocCreator.class);

    public static List<String> processedIDs = new ArrayList<String>(100);

    @SuppressWarnings({"UnusedDeclaration"})
    public IterativeHelperDocCreator(Configuration conf) {
        super(conf);
    }

    @Override
    protected boolean processPayload(Payload payload) {
        log.debug("Processing " + payload);
        Document luceneDoc = new Document();
        String id = payload.getId();
        if (id == null) {
            throw new IllegalArgumentException("No id defined for Payload");
        }
        luceneDoc.add(new Field(IndexUtils.RECORD_FIELD, id, 
                                Field.Store.YES, Field.Index.UN_TOKENIZED));
        luceneDoc.add(new Field("title", "Title_" + id,
                                Field.Store.YES, Field.Index.UN_TOKENIZED));
        luceneDoc.add(new Field("onlystore", "Stored_" + id,
                                Field.Store.YES, Field.Index.NO));
        luceneDoc.add(new Field("onlyindex", "Indexed_" + id,
                                Field.Store.NO, Field.Index.UN_TOKENIZED));
        luceneDoc.add(new Field("someField", "SomeField_" + id,
                                Field.Store.YES, Field.Index.UN_TOKENIZED));
        luceneDoc.add(new Field("duplicate", "Static",
                                Field.Store.YES, Field.Index.UN_TOKENIZED));
        payload.getData().put(Payload.LUCENE_DOCUMENT, luceneDoc);
        processedIDs.add(id);
        return true;
    }
}
