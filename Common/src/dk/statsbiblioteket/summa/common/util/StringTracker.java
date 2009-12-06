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
/*
 * The State and University Library of Denmark
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Tracks memory usage of Strings.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Check for 64-bit (but be aware of 32bit-pointer-mode)
public class StringTracker extends ResourceTrackerImpl<String> {

    /**
     * Basic overhead for a single String.
     * @see
     * {@url http://www.javamex.com/tutorials/memory/string_memory_usage.shtml}
     */
    public static final int SINGLE_ENTRY_OVERHEAD = 38; // 32 bit

    public StringTracker(long maxCountLimit, long memLimit) {
        super(maxCountLimit, memLimit);
    }

    public StringTracker(long minCountLimit, long maxCountLimit, long memLimit) {
        super(minCountLimit, maxCountLimit, memLimit);
    }

    @Override
    long calculateBytes(String element) {
        return element == null ? 0
               : element.length() * 2 + SINGLE_ENTRY_OVERHEAD;
    }
}
