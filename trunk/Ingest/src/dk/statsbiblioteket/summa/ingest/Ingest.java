/* $Id: Ingest.java,v 1.17 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.17 $
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

import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.ingest.io.IODeleteRecord;
import dk.statsbiblioteket.summa.ingest.io.IOUpdateRecord;
import dk.statsbiblioteket.summa.ingest.io.IOCreateRecord;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.Status;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.net.MalformedURLException;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides access to an underlying storage component.<br>
 * The Ingest collects records and acts as an non-blocking buffer between the underlying IO and
 * the Parser process.<br>
 *
 * The buffer has a fixed size of 10K write operations, if the buffer limit is exceeded write operations will block.<br>
 * @deprecated this has been superceeded by the general filter workflow and
 *             specifically by RecordWriter in the Storage module. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal, te")
public class Ingest {

    private static final Log log = LogFactory.getLog(Ingest.class);

    /**
     * The {@link Access}-enabled metadata storage server that this Ingest is
     * connected to.
     */
    private static Access _io;

    /**
     * A map between locations and instances of Ingest. This is used by
     * {@link #getInstance}.
     */
    private static Map<String, Ingest> _myInstances = null;

    /**
     * The maximum size of the {@link #storageQueue} before a
     * {@link QueueFullHandler} is invoked.
     */
    private static final int STORAGE_QUEUE_SIZE = 15000;

    /**
     * The queue holding the Runnables responsible for updating the metadata
     * storage server. The queue is delayed so rapid queueing will result in
     * pauses to let the queue empty. If the queue runs full (which is
     * unlikely), a {@link QueueFullHandler} is invoked.
     */
    private  ArrayBlockingQueue<Runnable> storageQueue;

    /**
     * A thread pool connected to the storageQueue. Runnables added to the
     * storageQueue will be executed in turn, when there are available threads
     * in the pool.
     */
    private  ExecutorService servicePool;

    /**
     * Connects to the given {@link Access}-enabled metadata storage server
     * by RMI and creates a servicePool, ready for processing records.
     * @param RMIServer the name of a metadata storage server.
     * @throws MalformedURLException if the URL was not well-formed.
     * @throws NotBoundException     if the RMIServer was not registered.
     * @throws RemoteException       in case of connection problems.
     */
    private Ingest(String RMIServer) throws MalformedURLException,
                                            NotBoundException,
                                            RemoteException {
        storageQueue = new ArrayBlockingQueue<Runnable>(STORAGE_QUEUE_SIZE);
        servicePool = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE,
                                             TimeUnit.MILLISECONDS,
                                             storageQueue,
                                             new QueueFullHandler());

           //if (System.getSecurityManager() == null) {
                log.info("Getting RMISecurityManager");
                System.setSecurityManager(new RMISecurityManager());
           // }
            log.info("getting remote io: " + RMIServer);
        _io = (Access) Naming.lookup(RMIServer);

    }

    /**
     * Telegram an Ingest instance for a given location. Instances are
     * automatically created, if they don't exist.
     * @param location the address of the RMIServer that should be used.
     * @return an instance of Ingest that corresponds to the given location.
     *         null is returned if it was not possible to establish a
     *         connection.
     */
    public static synchronized Ingest getInstance(String location){
        if (_myInstances == null){
            _myInstances = new HashMap<String, Ingest>(10);
        }
        if (!_myInstances.containsKey(location)){
            try {
                Ingest in = new Ingest(location);
                 _myInstances.put(location, in);
            } catch (MalformedURLException e) {
                log.error(e.getMessage(), e);
            } catch (NotBoundException e) {
               log.error(e.getMessage(), e);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
            }

        }
        return _myInstances.get(location);
    }

    /**
     * Depending on the internal state of the record, either update, delete
     * or ingest it to the storage server.
     * @param record the record to flush to the storage server.
     */
    public void flush(Record record){
        String validationState =
                record.getMeta(Record.META_VALIDATION_STATE) == null ?
                Record.ValidationState.notValidated.toString() :
                Record.ValidationState.fromString(record.getMeta(
                        Record.META_VALIDATION_STATE)).toString();
        log.info("Flushing record: isDeleted: " + record.isDeleted()
                 + " isIndexable: " + record.isIndexable()
                 + " isValid: " + validationState
                 + " base:" + record.getBase()
                 + " id:" + record.getId());
        // TODO: Should we ingest if the record is invalid
//        if (record.getState() != null &&
        if (record.getBase() != null &&
            record.getId() != null) {
            if (record.isModified()){
                update(record.getId(), record.getContent(), record.getBase());
            } else if (record.isDeleted()){
                delete(record.getId());
            } else {
                ingest(record.getId(), record.getContent(), record.getBase());
            }
        } else {
            throw new IllegalStateException("Record not ready for flush");
        }
    }

    /**
     * Update the record with the given ID in the given base. Any existing
     * record with that ID will be overwritten.
     * @param id     the ID for the record.
     * @param record the record to store.
     * @param base   the name of the original record base.
     * @deprecated use @link #update(String, byte[], String} instead.
     */
    public void update(String id, String record, String base){
        waitForQueue();
        servicePool.submit(new IOUpdateRecord(_io,id,record.getBytes(), base));
    }

    /**
     * Update the record with the given ID in the given base. Any existing
     * record with that ID will be overwritten.
     * @param id     the ID for the record.
     * @param record the record to store.
     * @param base   the name of the original record base.
     */
    public void update(String id, byte[] record, String base){
        waitForQueue();
        servicePool.submit(new IOUpdateRecord(_io,id,record, base));
    }

    /**
     * Ingest the record with the given ID in the given base. If a record
     * already exists with the given ID, an exception is thrown.
     * @param id     the ID for the record.
     * @param record the record to store.
     * @param base   the name of the original record base.
     */
    public void ingest(String id, byte[] record, String base){
        waitForQueue();
        servicePool.submit(new IOCreateRecord(_io,id,record, base));
        log.info("ingested \"" + id + "\" from \"" + base + "\"");
    }

    /**
     * Ingest the record with the given ID in the given base. If there is
     * no record with the given ID, nothing happens.
     * @param id the ID for the record.
     * */
    public void delete(String id){
        waitForQueue();
        servicePool.submit(new IODeleteRecord(_io,id));
    }

    /**
     * If the {@link #storageQueue} is too large, send the current Thread to
     * sleep for a while, giving the {@link #servicePool} time to process some
     * of the entries.
     * If the queue is not too large, the method returns immediately.
     * @see QueueFullHandler for the behaviour in case of queue overflow.
     */
    private void waitForQueue() {
        if (storageQueue.remainingCapacity() < 5000) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.debug(e.getMessage());
            }
        }
    }

    /**
     * @return The {@link Access}-enabled metadata storage server that this
     *         Ingest is connected to.
     */
    Access getIO(){
        return _io;
    }

    /**
     * @return the number of record updates that are queued.
     */
    public int getInQueue(){
        return storageQueue.size();
    }


}
