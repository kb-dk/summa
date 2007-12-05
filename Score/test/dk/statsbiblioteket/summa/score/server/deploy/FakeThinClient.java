/* $Id: FakeThinClient.java,v 1.5 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/11 12:56:25 $
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
 * CVS:  $Id: FakeThinClient.java,v 1.5 2007/10/11 12:56:25 te Exp $
 */
package dk.statsbiblioteket.summa.score.server.deploy;

import java.io.FileWriter;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class FakeThinClient {
    public static void main(String[] args) throws IOException {
        FileWriter fw = new FileWriter("output.txt");
        fw.write("Hello World!\n");
        fw.write("I should get my config from "
                 + System.getenv("summa.score.configuration"));
        fw.write("My instanceID is "
                 + System.getenv("summa.score.instance_id"));
        fw.close();
    }
}
