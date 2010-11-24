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

