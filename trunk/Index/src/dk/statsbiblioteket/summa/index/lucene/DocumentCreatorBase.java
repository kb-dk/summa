/* $Id:$
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
package dk.statsbiblioteket.summa.index.lucene;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.index.IndexField;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.document.Field;

/**
 * Convenience base for Document creators.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class DocumentCreatorBase extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(DocumentCreatorBase.class);

    protected LuceneIndexField addFieldToDocument(
            LuceneIndexDescriptor descriptor,
            org.apache.lucene.document.Document luceneDoc, String fieldName,
            String content, Float boost) throws IndexServiceException {
        LuceneIndexField indexField = descriptor.getFieldForIndexing(fieldName);
        if (indexField == null) {
            throw new IndexServiceException(String.format(
                    "The field name '%s' could not be resolved. This should"
                    + " never happen (fallback should be the default "
                    + "field)", fieldName
            ));
        }
        if (!fieldName.equals(indexField.getName())) {
            log.debug("The field name '" + fieldName
                      + "' resolved to index field '"
                      + indexField.getName() + "'");
        }
        log.trace("Creating field '" + fieldName + "' with boost " + boost);
        Field field = new Field(fieldName, content, indexField.getStore(),
                                indexField.getIndex(),
                                indexField.getTermVector());
        if (boost != null) {
            field.setBoost(indexField.getIndexBoost() * boost);
        }

        if (log.isTraceEnabled()) {
            log.trace("Adding field '" + fieldName + "' with "
                      + content.length()
                      + " characters and boost " + field.getBoost()
                      + " to Lucene Document");
        }
        luceneDoc.add(field);
        return indexField;
    }

    protected void addToFreetext(LuceneIndexDescriptor descriptor,
                               org.apache.lucene.document.Document luceneDoc,
                               String fieldName,
                               String content) throws IndexServiceException {
        LuceneIndexField freetext =
                descriptor.getFieldForIndexing(IndexField.FREETEXT);
        if (freetext == null) {
            throw new IndexServiceException(String.format(
                    "The field freetext with name '%s' could not be "
                    + "resolved. This should never happen (fallback "
                    + "should be the default field)",
                    IndexField.FREETEXT));
        }
        if (!IndexField.FREETEXT.equals(freetext.getName())) {
            log.warn("The field '" + IndexField.FREETEXT + "' could not"
                     + " be located, so the content of field '" + fieldName
                     + "' is not added to freetext");
        } else {
            Field freeField = new Field(freetext.getName(), content,
                                        freetext.getStore(),
                                        freetext.getIndex(),
                                        freetext.getTermVector());
            freeField.setBoost(freetext.getIndexBoost());
            log.trace("Adding content from '" + fieldName + "' to freetext");
            luceneDoc.add(freeField);
        }
    }

}
