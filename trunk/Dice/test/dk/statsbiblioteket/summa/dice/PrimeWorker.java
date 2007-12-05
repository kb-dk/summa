/* $Id: PrimeWorker.java,v 1.3 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:20 $
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
package dk.statsbiblioteket.summa.dice;

import dk.statsbiblioteket.summa.dice.Config;

import java.util.ArrayList;
import java.util.Iterator;
import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 1/09/2006
 * Time: 14:55:46
 * To change this template use File | Settings | File Templates.
 */
public class PrimeWorker extends CachingWorker {

    private static final int primeCertainty = 11; // 1/(2^primeCertainty)

    public PrimeWorker (Config conf) {
        super (conf);
    }

    /**
     * Filter all non-primes from job.getData() and return
     * a job whose data member is a list of the remaining
     * primes.
     */
    protected Job processJob (Job job) {
        Iterator parts = ((Iterable)job.getData()).iterator();

        ArrayList output = new ArrayList();
        while (parts.hasNext()) {
            BigInteger big = (BigInteger) parts.next();
            if (big.isProbablePrime(primeCertainty)) {
                output.add(big);
            }
        }
        return new Job (output, job.getHints(), job.getName());
    }
}
