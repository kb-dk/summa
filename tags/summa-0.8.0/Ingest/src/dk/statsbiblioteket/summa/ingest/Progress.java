/* $Id: Progress.java,v 1.8 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.8 $
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


import java.util.ArrayList;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Implementing progress bookkeeping for monitoring purposes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public class Progress implements ProgressMBean {

   private static int recordCount;
   private static int badRecords;

   private static int currentIngestRate;
   private static int meanIngestRate;

   private Ingest in;
   private long lastTime;
   private int lastRecordCount;

   private long firstTime;

   private static ArrayList<String> todo;
   private static ArrayList<String>  done;


    private Progress(){
         todo = new ArrayList<String>();
         done = new ArrayList<String>();
         recordCount = 0;
         badRecords = 0;
         lastRecordCount = 0;
         lastTime = System.currentTimeMillis();
         currentIngestRate =0;
         meanIngestRate = 0;
         firstTime = lastTime;
         in = null;
    }

    public int getCurrentIngestRate(){
        long time = System.currentTimeMillis();
        int rec = recordCount;
        int rate = 0;
        if (recordCount > 0 ){
            double timediff = (time - lastTime)/ (1000.0 * 60.0);
            int recorddiff = rec - lastRecordCount;
            try {
                rate = new Double(Math.ceil(recorddiff / timediff)).intValue();
            } catch (ArithmeticException e) { // Division by zero
                rate = 0;
            }
            lastTime = time;
            lastRecordCount = rec;
            currentIngestRate = rate;
        }
        return currentIngestRate;
    }

    public int getMeanIngestRate(){
        long time = System.currentTimeMillis();
        try {
            if (recordCount > 0 && recordCount > lastRecordCount){
                double timediff = (time - firstTime)/ (1000.0 * 60.0);
                meanIngestRate = new Double(Math.ceil(recordCount / timediff)).intValue();
            }
            return meanIngestRate;
        } catch (ArithmeticException e) { // Division by zero
            return 0;
        }
    }

    public static void count(){
        recordCount++;
    }

    public static void nogood(){
        badRecords++;
    }

    public ArrayList getTodo(){
        return todo;
    }

    public ArrayList getDone(){
        return done;
    }

    public int getCurrentQueueSize() {
        if (in != null){return in.getInQueue();}
        return 0;
    }

    public static void addTodo(String todoTask){
         todo.add(todoTask);
    }

    public static void done(String doneTask){
        todo.remove(doneTask);
        done.add(doneTask);
    }

    public int getRecordCount() {
        return recordCount;
    }

    public int getBadRecords() {
        return badRecords;
    }

    public  void setIngest(Ingest in){
        this.in = in;
    }

    public static Progress reset(){
          return new Progress();
    }
}
