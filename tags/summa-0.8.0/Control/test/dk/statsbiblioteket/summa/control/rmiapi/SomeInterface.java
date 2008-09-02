/**
 * Created: te 09-04-2008 20:25:23
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.control.rmiapi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Dummy interface.
 */
public interface SomeInterface extends Remote {
    public int getNext() throws RemoteException;
}
