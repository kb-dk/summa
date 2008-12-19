/**
 * Created: te 09-04-2008 20:40:31
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.control.rmiapi;

/**
 * A class with RMI interface that we want wrapped as a service.
 */
public class SomeClass implements SomeInterface {
    private int counter = 0;
    public int getNext() {
        return counter++;
    }
}


