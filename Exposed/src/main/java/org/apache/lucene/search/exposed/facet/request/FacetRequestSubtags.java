/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package org.apache.lucene.search.exposed.facet.request;

import org.apache.lucene.search.exposed.facet.ParseHelper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class FacetRequestSubtags implements SubtagsConstraints {
  final int maxTags;
  final int minCount;
  final int minTotalCount;
  final SUBTAGS_ORDER order;
  final FacetRequestSubtags subtags;

  public FacetRequestSubtags(int maxTags, int minCount, int minTotalCount, SUBTAGS_ORDER order) {
    this.maxTags = maxTags;
    this.minCount = minCount;
    this.minTotalCount = minTotalCount;
    this.order = order;
    this.subtags = null;
  }

  public FacetRequestSubtags(XMLStreamReader reader, SubtagsConstraints defaults) throws XMLStreamException {
    int maxTags = defaults.getMaxTags();
    int minCount = defaults.getMinCount();
    int minTotalCount = defaults.getMinTotalCount();
    SUBTAGS_ORDER order = defaults.getSubtagsOrder();

    final String request = "Not available";
    for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
      String attribute = reader.getAttributeLocalName(i);
      String value = reader.getAttributeValue(i);
      if ("maxtags".equals(attribute)) {
        maxTags = ParseHelper.getInteger(request, "maxtags", value);
        if (maxTags == -1) {
          maxTags = Integer.MAX_VALUE;
        }
      } else if ("mincount".equals(attribute)) {
        minCount = ParseHelper.getInteger(request, "mincount", value);
      } else if ("mintotalcount".equals(attribute)) {
        minTotalCount = ParseHelper.getInteger(request, "mintotalcount", value);
      } else if ("suborder".equals(attribute)) {
        order = SUBTAGS_ORDER.fromString(value);
      }
    }
    this.maxTags = maxTags;
    this.minCount = minCount;
    this.minTotalCount = minTotalCount;
    this.order = order;

    reader.nextTag();
    FacetRequestSubtags subtags = null;
    while (!ParseHelper.atEnd(reader, "subtags")) {
      if (ParseHelper.atStart(reader, "subtags")) {
        subtags = new FacetRequestSubtags(reader, this);
      }
      reader.nextTag();
    }
    this.subtags = subtags;
  }

  @Override
  public int getMaxTags() {
    return maxTags;
  }

  @Override
  public int getMinCount() {
    return minCount;
  }

  @Override
  public int getMinTotalCount() {
    return minTotalCount;
  }

  @Override
  public SUBTAGS_ORDER getSubtagsOrder() {
    return order;
  }

  @Override
  public SubtagsConstraints getDeeperLevel() {
    return subtags == null ? this : subtags;
  }
}
