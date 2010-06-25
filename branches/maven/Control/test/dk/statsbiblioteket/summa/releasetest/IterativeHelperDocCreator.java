/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
                                Field.Store.YES, Field.Index.NOT_ANALYZED));
        luceneDoc.add(new Field("title", "Title_" + id,
                                Field.Store.YES, Field.Index.NOT_ANALYZED));
        luceneDoc.add(new Field("onlystore", "Stored_" + id,
                                Field.Store.YES, Field.Index.NO));
        luceneDoc.add(new Field("onlyindex", "Indexed_" + id,
                                Field.Store.NO, Field.Index.NOT_ANALYZED));
        luceneDoc.add(new Field("someField", "SomeField_" + id,
                                Field.Store.YES, Field.Index.NOT_ANALYZED));
        luceneDoc.add(new Field("duplicate", "Static",
                                Field.Store.YES, Field.Index.NOT_ANALYZED));
        payload.getData().put(Payload.LUCENE_DOCUMENT, luceneDoc);
        processedIDs.add(id);
        return true;
    }
}

