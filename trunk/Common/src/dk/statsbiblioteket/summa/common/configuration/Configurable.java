/* $Id: Configurable.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A {@link Configurable} is a class that has a constructor that takes
 * a {@link Configuration} as single argument. You can instantiate
 * {@link Configurable}s with {@link Configuration#create}.
 * </p><p>
 * <i>It is an error to declare a class {@code Configurable} if it
 * does not have a constructor as described above.</i>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Configurable {

    /**
     * A {@link RuntimeException} thrown by {@link Configuration#create}
     * if there is an error instantiating a {@code Configurable} via its
     * one-argument constructor that takes a {@link Configuration}.
     */
    public class ConfigurationException extends RuntimeException {

        public ConfigurationException (Throwable cause) {
            super (cause);
        }

        public ConfigurationException (String message) {
            super (message);
        }

        public ConfigurationException (String message, Throwable cause) {
            super (message, cause);
        }

    }

}
