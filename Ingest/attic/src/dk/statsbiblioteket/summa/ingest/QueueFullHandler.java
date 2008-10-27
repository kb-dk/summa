/* $Id: QueueFullHandler.java,v 1.7 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.7 $
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
package dk.statsbiblioteket.summa.ingest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This Handler is being used on {@link java.util.concurrent.ExecutorService}  that is running upon a {@link java.util.Queue}.<br>
 * This implementation runs the {@link java.lang.Runnable} in the current thread. and provide additional logging of the event.
 *
 * The main difference between this Handler and the {@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy} handler is that this
 * implementation will run the task even though the executor has been shutdown.
 * @deprecated as part of the old stable summa workflow.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
       state = QAInfo.State.UNDEFINED,
       author = "hal")
public class QueueFullHandler implements RejectedExecutionHandler {

    private static Log log = LogFactory.getLog(QueueFullHandler.class);

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.info("Queue is full, running in rejectedExecutionHandler");
        r.run();
    }
}



