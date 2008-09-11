/* $Id: PhonofileDuration.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.plugins;

import dk.statsbiblioteket.util.qa.QAInfo;

//todo: move this to netmusikken

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class PhonofileDuration {

    private static int sec;

    public static String duration(String in){
        in.indexOf('T');
        in = in.substring(in.indexOf('T')+1);
        in = in.replaceFirst("(0*)", "");
        in = in.replaceAll("^H" , "");
        in = in.replaceAll("[HM]",":");
        in = in.replaceAll("S", "");
        return in;
    }

    public static String addDuration(String in){
        String[] time = duration(in).split(":");
        int j = 1;
        for (int i = time.length - 1; i>=0; i--){
            sec += ((int)Math.pow(60,j++)/60) * Integer.parseInt(time[i]);
        }
        System.out.println("total secs:" + sec);
        return "";
    }

    public static String getAddedDuration(){
        String dur ="";
        int hours = sec / (60*60);
        int min = (sec - hours*(60*60)) / 60;
        int seco = sec - (hours*(60*60)) - min*60;
        if (hours > 0){
            dur += hours + ":";
        }
        if (min<10){
            dur += '0'+min+":";
        } else {
            dur += min + ":";
        }
        if (seco<10){
                 dur += '0'+seco;
        } else {
            dur += seco;
        }
        sec = 0;
        return dur;
    }

}



