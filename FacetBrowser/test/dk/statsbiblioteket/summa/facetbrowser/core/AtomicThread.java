/* $Id: AtomicThread.java,v 1.4 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:17 $
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
 * CVS:  $Id: AtomicThread.java,v 1.4 2007/10/04 13:28:17 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

/**
 * Helper class to test the speed of operations on AtomicInteger vs. int.
 */
public class AtomicThread extends Thread {
    public int[] plain;
    AtomicInteger[] atomic;
    Random random = new Random();
    int runs;
    TYPE type;
    enum TYPE {SIMULATE, PLAIN, ATOMIC}

    public AtomicThread(int[] plain, AtomicInteger[] atomic, TYPE type,
                        int runs) {
        this.plain = plain;
        this.atomic = atomic;
        this.type = type;
        this.runs = runs;
    }

    public void run() {
        int length;
        switch (type) {
            case PLAIN:
                length = plain.length;
                for (int i = 0 ; i < runs ; i++) {
                    plain[random.nextInt(length)]++;
                }
                break;
            case ATOMIC:
                length = atomic.length;
                for (int i = 0 ; i < runs ; i++) {
                    atomic[random.nextInt(length)].incrementAndGet();
                }
                break;
            case SIMULATE:
                for (int i = 0 ; i < runs ; i++) {
                    random.nextInt();
                }
                break;
        }
    }
}
