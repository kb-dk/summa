/* $Id: Word.java,v 1.5 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.5 $
 * $Date: 2007/12/04 10:26:43 $
 * $Author: bam $
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
package dk.statsbiblioteket.summa.clusterextractor.data;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;

/**
 * A representation of a word in a cluster extractor {@link Vocabulary}.
 * The word contains a String representation of the actual word as well as
 * document frequency (number of documents that this word occurs in) and
 * a boost factor (NOT USED).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class Word implements Serializable {
    /** String representation of the actual word. */
    private String text;
    /** Document frequency of this word in the index. */
    private int freq;
    /** Boost factor of this word - NOT USED. */
    private float boost;

    /**
     * Construct word with given text, frequency and boost factor.
     * @param text String representation of actual word
     * @param freq document frequency in index
     * @param boost boost factor
     */
    public Word(String text, int freq, float boost) {
        this.text = text;
        this.freq = freq;
        this.boost = boost;
    }

    /**
     * Get String representation of word.
     * @return String representation of word
     */
    public String getText() {
        return text;
    }

    /**
     * Get document frequency of this word in the index.
     * @return document frequency of this word in the index
     */
    public int getFreq() {
        return freq;
    }

    /**
     * Get boost factor of this word.
     * @return boost factor of this word
     */
    public float getBoost() {
        return boost;
    }

    /**
     * Returns a textual representation of this Word.
     * The representation includes text, document frequency and boost factor.
     * @return a string representation of this word
     */
    public String toString() {
        return text + ": freq = " + freq + ", boost = " + boost + ".";
    }
}
