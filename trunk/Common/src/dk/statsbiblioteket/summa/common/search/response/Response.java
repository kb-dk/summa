/* $Id: Response.java,v 1.2 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:18 $
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
package dk.statsbiblioteket.summa.common.search.response;

/*
* The State and University Library of Denmark
* CVS:  $Id: Response.java,v 1.2 2007/10/04 13:28:18 te Exp $
*/
public interface Response {
    public static enum PRIMITIVE_COMPARABLE {
        _float  { int getTag() { return 0x00000001; } },
        _string { int getTag() { return 0x00000002; } },
        _int    { int getTag() { return 0x00000003; } };
        abstract int getTag();
        PRIMITIVE_COMPARABLE getEnum(int tag) {
            switch (tag) {
                case 1: return _float;
                case 2: return _string;
                case 3: return _int;
                default: return null;
            }
        }
    }
}
