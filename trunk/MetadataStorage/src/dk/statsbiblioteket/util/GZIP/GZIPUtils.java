/* $Id: GZIPUtils.java,v 1.3 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.util.GZIP;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import dk.statsbiblioteket.util.qa.QAInfo;


@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class GZIPUtils {


    public static final int READ_BUFFER_SIZE= 2048;

    public static byte[] gunzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
        byte[] buf = new byte[READ_BUFFER_SIZE];
            while (true) {
                int size = in.read(buf);
                if (size <= 0)
                    break;
                out.write(buf, 0, size);
            }
            out.close();
            return out.toByteArray();
        }

        public static byte[] gzip(byte[] data) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            GZIPOutputStream zip = new GZIPOutputStream(buf);
            zip.write(data);
            zip.close();
            return buf.toByteArray();
        }




}
