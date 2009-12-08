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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.FieldProvider;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.text.ParseException;

/**
 * An IndexDescriptor that keeps a copy of the descriptor-XML. This is used to
 * store the descriptor-XML together with the index, thus making the two parts
 * stay in sync.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StoringIndexDescriptor extends IndexDescriptor<IndexField> {
    private static Log log = LogFactory.getLog(StoringIndexDescriptor.class);
    private String xml; // Do not set this to null as it overrides parse value

    public StoringIndexDescriptor(Configuration configuration)
                                                            throws IOException {
        super(configuration);
    }

    public IndexField createNewField() {
        return new StoringField();
    }

    public IndexField createNewField(Node node) throws ParseException {
        return new StoringField(node, this);
    }

    private static class StoringField extends IndexField {
        private StoringField() {
        }
        private StoringField(Node node, FieldProvider fieldProvider) throws ParseException {
            super(node, fieldProvider);
        }
        @Override
        protected Object getDefaultIndexAnalyzer() {
            return new Object();
        }
        @Override
        protected Object getDefaultQueryAnalyzer() {
            return new Object();
        }
    }

    @Override
    public Document parse(String xml) throws ParseException {
        log.debug("parse(...): Received IndexDescriptor XML of size "
                  + (xml == null ? "null" : xml.length()));
        this.xml = xml;
        return super.parse(xml);
    }

    /**
     * @return the last descriptor-XML received or null if no descriptions
     *         has been received.
     */
    public String getXml() {
        return xml;
    }
}
