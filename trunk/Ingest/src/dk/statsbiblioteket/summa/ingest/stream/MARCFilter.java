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
package dk.statsbiblioteket.summa.ingest.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;

/**
 * Takes a Stream as input, splits into MARC-records and creates Summa-Records
 * with the content. As part of the split, the following properties are
 * extracted from the MARC-record:
 * <ul>
 *   <li>recordID</li>
 *   <li>state (deleted or not)</li>
 *   <li>parent/child relations</li>
 * </ul>
 * </p><p>
 * The extraction of recordID and parent/chile-relations is non-trivial due to
 * the nature of MARC, so the input is parsed. A streaming parser is used as
 * performance is prioritized over clarity (the streaming parser has less
 * GC overhead than a full DOM build). 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARCFilter {
    private static Log log = LogFactory.getLog(MARCFilter.class);


 }
