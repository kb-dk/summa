/* $Id: MemoryStringPool.java,v 1.3 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:21 $
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
 * CVS:  $Id: MemoryStringPool.java,v 1.3 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import java.text.Collator;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simple implementation of Strings with MemoryPool.
 * The persistent files used by this implementation are compatible with those
 * from {@link DiskStringPool}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class MemoryStringPool extends MemoryPool<String> implements
                                                          CollatorSortedPool {
//    private Log log = LogFactory.getLog(MemoryStringPool.class);

    private Collator collator = null;

    public MemoryStringPool(Collator collator) {
        super(new StringConverter(), collator);
        this.collator = collator;
    }

    /* Mutators */

    public Collator getCollator() {
        return collator;
    }
    public void setCollator(Collator collator) {
        this.collator = collator;
    }
}
