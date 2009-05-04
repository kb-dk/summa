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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import java.io.IOException;
import java.text.ParseException;

/**
 * Extracts Facet-setup from IndexDescriptor-XML. This builds upon the
 * LuceneIndexDescriptor in order to get a full amount of default fields,
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetIndexDescriptor extends LuceneIndexDescriptor {
    private static Logger log = Logger.getLogger(FacetIndexDescriptor.class);

    Document dom = null;

    public FacetIndexDescriptor(Configuration configuration) throws IOException{
        super(configuration);
    }

    @Override
    public Document parse(String xml) throws ParseException {
        dom = super.parse(xml);
        return dom;
    }
    public Document getDOM() {
        return dom;
    }
}
