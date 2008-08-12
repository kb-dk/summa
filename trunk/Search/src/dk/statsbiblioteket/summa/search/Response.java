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
package dk.statsbiblioteket.summa.search;

import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A response from a SearchNode contains SearchNode specific content.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment="Better class description needed ")
public interface Response extends Serializable {
    /**
     * @return the name of the response. Responses instantiated from the same
     *         class are required to return the same name, so that name
     *         equality means that {@link #merge} can be safely called.
     *         Names are required to be unique across classes.
     */
    public String getName();

    /**
     * Merge the content from other into this Response, sorting and trimming
     * when necessary.
     * @param other the Response to merge into this.
     * @throws ClassCastException if other was not assignable to this.
     */
    public void merge(Response other) throws ClassCastException;

    /**
     * The XML returned should be an XML-snippet: UTF-8 is used and no header
     * should be included. A proper response could be {@code
<myresponse>
    <hits total="87">
    <hit>Foo</hit>
    ...
</myresponse>
    }
     * @return the content of Response as XML.
     */
    public String toXML();
}
