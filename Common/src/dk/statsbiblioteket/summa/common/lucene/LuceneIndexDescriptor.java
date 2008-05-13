/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.common.lucene;

import java.text.ParseException;

import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.w3c.dom.Node;

/**
 * Trivial implementation of a Lucene IndexDescriptor.
 */
public class LuceneIndexDescriptor
        extends IndexDescriptor<IndexField<Analyzer, Tokenizer, IndexFilter>> {

    public IndexField<Analyzer, Tokenizer, IndexFilter> createNewField() {
        return new IndexField<Analyzer, Tokenizer, IndexFilter>();
    }

    public IndexField<Analyzer, Tokenizer, IndexFilter>
                                                     createNewField(Node node)
                                                         throws ParseException {
        return new IndexField<Analyzer, Tokenizer, IndexFilter>(node, this);
    }

    // TODO: Create default fields (including freetext)
}
