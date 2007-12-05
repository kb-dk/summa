/* $Id: ClientDeployer.java,v 1.7 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.7 $
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
package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Abstract representation of a way of deploying and controlling Score Clients.
 * Example {@code ClientDeployer}s could include <i>ssh</i>,
 * <i>local deployment</i>, <i>rmi</i>, etc.
 * </p><p>
 * The ClientDeployer is responsible maintaining or reestablishing contact
 * with clients, in order to perform start or stop.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface ClientDeployer extends Configurable {
    
    /**
     * An id for a client or a service, that is unique within the given Summa
     * installation. This should be set as a system property.<br />
     * Example: -Dsumma.score.instance_id=mySearchInstallation-3.
     */
    public static final String INSTANCE_ID_PROPERTY =
            "summa.score.instance_id";

    /**
     * Deploy the client, as specified in the base configuration for the
     * ClientDeployer.
     * @param feedback callback for communication with the user.
     * @throws Exception if something goes wrong during deploy.
     */
    public void deploy (Feedback feedback) throws Exception;

    /**
     * Start the client specified in the base configuration for the
     * ClientDeployer.
     * @param feedback callback for communication with the user.
     * @throws Exception if something goes wrong during start.
     */
    public void start (Feedback feedback) throws Exception;
}
