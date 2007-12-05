/* $Id: IndexConfig.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.index.dice;

import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class IndexConfig extends Config {

    public static final String XSLT_URL = "xslt.url";
    public static final String FIRST_RECORD_ID = "record.first";
    public static final String LAST_RECORD_ID = "record.last";
    public static final String BASE = "base";
    public static final String RECORD_SERVICE = "record.service";
    public static final String DATA_DIR = "data.dir";

    public IndexConfig () throws IOException {
        super();

        loadDefaults();

        /* Config options that needs runtime info */
        String diceDataDir = System.getProperty("user.home") + File.separator + "tmp" + File.separator + "dice_data_" + getStartDate();
        set (DATA_DIR, diceDataDir);
        set (EMPLOYER_CACHE_PATH, diceDataDir + File.separator + "indexer_cache");
        set (WORKER_CACHE_PATH, diceDataDir + File.separator + "worker_cache");
        set (CONSUMER_CACHE_PATH, diceDataDir + File.separator + "merger_cache");

    }
}
