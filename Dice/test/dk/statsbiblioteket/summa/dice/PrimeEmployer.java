/* $Id: PrimeEmployer.java,v 1.3 2007/10/04 13:28:20 te Exp $
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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 1/09/2006
 * Time: 14:38:15
 * Purposedly inefficient implementation of of a prime generating scheme
 */
public class PrimeEmployer extends EmployerBase {

    BigInteger count;

    private static final BigInteger jobCount = new BigInteger("100");
    private static final BigInteger chunkSize = new BigInteger("100");

    public PrimeEmployer (Config conf) throws RemoteException {
        super (conf);
        count = new BigInteger("0");
    }

    public Job fetchJob () {
        if (count.divide(chunkSize).equals (jobCount)) {
            return null;
        }

        ArrayList data = new ArrayList (chunkSize.intValue());
        BigInteger end = count.add(chunkSize);

        for (; !count.equals(end); count = count.add(new BigInteger("1"))) {
            data.add (count);
        }
        Job job = new Job (data, null, count.toString());

        // Add a little fake latency
        try {
            Thread.sleep (1);
        } catch (InterruptedException e) {
            // Do nothing
        }
        System.out.println ("Created job "  + job);
        return job;
    }

}
