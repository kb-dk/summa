/* $Id: CoordinateComparator.java,v 1.3 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.3 $
 * $Date: 2007/12/04 10:26:43 $
 * $Author: bam $
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
package dk.statsbiblioteket.summa.clusterextractor.math;

import java.util.Map;
import java.util.Comparator;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * CoordinateComparator compares {@link Map}.Entry<String, Number>.
 * The coordinates (map entries) are compared by the double value of their
 * Number key.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class CoordinateComparator implements Comparator<Map.Entry<String, Number>> {

    /**
     * Compare the two arguments for order.
     * The map entries are compared by the double value of their Number key.
     * Return a negative integer, zero, or a positive integer as
     * the first argument is less than, equal to, or greater than the second.
     */
    public int compare(Map.Entry<String, Number> o1, Map.Entry<String, Number> o2) {
        double diff = (o1.getValue().doubleValue() - o2.getValue().doubleValue());
        if (diff<0) {return -1;}
        if (diff>0) {return 1;}
        return o1.getKey().compareTo(o2.getKey());
    }
}



