/* $Id: ConfigTest.java,v 1.3 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:17 $
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
package dk.statsbiblioteket.summa.test;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.dice.TestWorker;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 27/09/2006
 * Time: 14:22:27
 * To change this template use File | Settings | File Templates.
 */
public class ConfigTest extends TestCase {

    public void testLoadFromXML () throws Exception {
        Config conf = new Config();
        conf.loadFromXML("test.properties.xml");
        System.out.println ("Int: hello.number=" + conf.getInt("hello.number"));
        System.out.println ("Int: hello.world=" + conf.getString("hello.world"));
        System.out.println ("Int: hello.class=" + conf.getClass("hello.class"));

        assertTrue(conf.getInt("hello.number") == 31415);
        assertTrue(conf.getString("hello.world").equals ("String with spaces"));
        assertTrue(conf.getClass("hello.class").equals(TestWorker.class));
    }
}
