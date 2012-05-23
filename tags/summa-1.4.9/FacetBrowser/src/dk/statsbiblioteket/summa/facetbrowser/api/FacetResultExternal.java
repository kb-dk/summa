/* $Id: FacetResultExternal.java,v 1.7 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.7 $
 * $Date: 2007/10/05 10:20:22 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FacetResultExternal.java,v 1.7 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.api;

import java.util.HashMap;
import java.io.StringWriter;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This facet structure representation is suitable for serializing and other
 * external use. It does not rely on String pool or similar index-specific
 * resources.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetResultExternal extends FacetResultImpl<String> {
    private HashMap<String, String[]> fields;

    public FacetResultExternal(HashMap<String, Integer> maxTags,
                               HashMap<String, Integer> facetIDs,
                               HashMap<String, String[]> fields) {
        super(maxTags, facetIDs);
        this.fields = fields;
    }

    public FacetResult externalize() {
        return this;
    }

    public String getName() {
        return "FacetResult";
    }

    @Override
    protected String getQueryString(String facet, String tag) {
        if (fields.get(facet) == null) {
            throw new IllegalStateException(String.format(
                    "No fields specified in facet '%s'", facet));
        }

        // TODO: Should # be excaped too?
        String cleanTag = getTagString(facet, tag);
        StringWriter sw = new StringWriter(100);
        String[] fields = this.fields.get(facet);
        if (fields.length > 1) {
            sw.append("(");
        }
        for (int i = 0 ; i < fields.length ; i++) {
            sw.append(fields[i]);
            sw.append(":\"");
            sw.append(queryEscapeTag(cleanTag));
            sw.append("\"");
            if (i < fields.length - 1) {
                sw.append(" OR ");
            }
        }
        if (fields.length > 1) {
            sw.append(")");
        }

        return sw.toString();
    }
}


