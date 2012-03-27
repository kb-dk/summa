/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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




