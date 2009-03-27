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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * Adds the given key and value to the meta data for the Payloads.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class DummyFilter extends ObjectFilterImpl {
//    private static Log log = LogFactory.getLog(DummyFilter.class);

    public static final String CONF_KEY =   "key";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String CONF_VALUE = "value";

    private String key;
    private String value;

    public DummyFilter(Configuration conf) {
        super(conf);
        key =   conf.getString(CONF_KEY);
        value = conf.getString(CONF_VALUE);
    }

    @Override
    protected boolean processPayload(Payload payload) {
        payload.getData().put(key, value);
        return true;
    }
}
