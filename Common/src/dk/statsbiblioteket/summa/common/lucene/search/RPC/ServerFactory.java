/* $Id: ServerFactory.java,v 1.3 2007/10/04 13:28:22 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:22 $
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
package dk.statsbiblioteket.summa.common.lucene.search.RPC;

import dk.statsbiblioteket.summa.common.search.SearchServer;
import dk.statsbiblioteket.summa.common.search.LowLevelSearch;
import dk.statsbiblioteket.summa.common.search.SummaTCPServer;
import dk.statsbiblioteket.summa.common.search.LowLevelSearchTester;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.concurrent.*;
import java.io.IOException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class ServerFactory {

    private Executor pool;


    private static class SearchPool extends ThreadPoolExecutor {

        /**
         * Creates a new <tt>ThreadPoolExecutor</tt> with the given initial
         * parameters.
         *
         * @param corePoolSize    the number of threads to keep in the
         *                        pool, even if they are idle.
         * @param maximumPoolSize the maximum number of threads to allow in the
         *                        pool.
         * @param keepAliveTime   when the number of threads is greater than
         *                        the core, this is the maximum time that excess idle threads
         *                        will wait for new tasks before terminating.
         * @param unit            the time unit for the keepAliveTime
         *                        argument.
         * @param workQueue       the queue to use for holding tasks before they
         *                        are executed. This queue will hold only the <tt>Runnable</tt>
         *                        tasks submitted by the <tt>execute</tt> method.
         * @param threadFactory   the factory to use when the executor
         *                        creates a new thread.
         * @param handler         the handler to use when execution is blocked
         *                        because the thread bounds and queue capacities are reached.
         * @throws IllegalArgumentException if corePoolSize, or
         *                                  keepAliveTime less than zero, or if maximumPoolSize less than or
         *                                  equal to zero, or if corePoolSize greater than maximumPoolSize.
         * @throws NullPointerException     if <tt>workQueue</tt>
         *                                  or <tt>threadFactory</tt> or <tt>handler</tt> are null.
         */
        public SearchPool(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) {
            super(corePoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    unit,
                    workQueue,
                    threadFactory,
                    handler);
        }

        protected void afterExecute(Runnable r,
                                    Throwable t) {
            super.afterExecute(r, t);
            //todo : here could be some cleanup if needed.
        }
    }


    SearchServer getDefaultSearchServer() {
        return null; // todo: make this return somthing better;
    }

    SearchServer getSummaTCPServer(int port, LowLevelSearch s) throws IOException {
        return new SummaTCPServer(port, s, this.pool);
    }


    public ServerFactory(int concurrentCount, int QueueSize) {
        this.pool = new SearchPool((concurrentCount * 2) + 1,
                (concurrentCount * 2) + 1,
                3000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(QueueSize * 2, true),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public static void main(String[] args) throws IOException {
/*        SearchServer s = new ServerFactory(10,10).getSummaTCPServer(34677, new LowLevelSearchTester());
        s.start();
        new Thread(s).start();
        System.out.println("server started");
  */

    }
}
