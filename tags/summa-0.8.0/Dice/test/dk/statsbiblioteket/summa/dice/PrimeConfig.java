/* $Id: PrimeConfig.java,v 1.3 2007/10/04 13:28:20 te Exp $
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

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 6/09/2006
 * Time: 11:11:50
 * To change this template use File | Settings | File Templates.
 */
public class PrimeConfig extends Config {

    public PrimeConfig () {
        super();
        setDefaults();

        setEmployerClass(PrimeEmployer.class);
        setConsumerClass(PrimeConsumer.class);
        setWorkerClass(PrimeWorker.class);

        setEmployerHostname("pc134");
        setConsumerHostname("pc134");

        String homeDir = System.getProperty("user.home");

        set (WORKER_CACHE_PATH, homeDir + File.separator + "tmp/worker_cache");
        set (CONSUMER_CACHE_PATH, homeDir + File.separator + "tmp/consumer_cache");
        set (CONSUMER_CACHE_PORT, 26000);
        set (CONSUMER_CACHE_SERVICE, "consumer_cache");
    }
}
