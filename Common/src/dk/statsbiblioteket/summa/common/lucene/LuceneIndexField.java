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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.document.Field;
import org.w3c.dom.Node;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.index.FieldProvider;

/**
 * Extension of IndexField to support Lucene-specific behaviour.
 */
public class LuceneIndexField extends
                              IndexField<Analyzer, Tokenizer, IndexFilter> {
    private Field.TermVector termVector =
            Field.TermVector.WITH_POSITIONS_OFFSETS;

    public LuceneIndexField() {
        super();
    }

    public LuceneIndexField(String name) {
        super(name);
    }

    public LuceneIndexField(Node node, FieldProvider indexFieldFieldProvider)
                                                         throws ParseException {
        super(node, indexFieldFieldProvider);
    }

    public LuceneIndexField(IndexField<Analyzer, Tokenizer, IndexFilter>
            parent) {
        super(parent);
    }

    /* Mutators */

    public Field.TermVector getTermVector() {
        return termVector;
    }

    public void setTermVector(Field.TermVector termVector) {
        this.termVector = termVector;
    }


}
