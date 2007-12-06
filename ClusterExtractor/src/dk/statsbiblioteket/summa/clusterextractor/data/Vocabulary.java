/* $Id: Vocabulary.java,v 1.5 2007/12/04 10:26:43 bam Exp $
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

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The vocabulary data structure contains all words in the vocabulary.
 * The primary method is lookUp(String text), which looks up a {@link Word}.
 * Vocabulary contains a primary {@link Map}: Map<String, Word>
 * and a (possibly empty) set of 'helper maps'.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class Vocabulary implements Serializable {
    protected static final Log log = LogFactory.getLog(Vocabulary.class);
    /** Primary vocabulary map from String to Word. */
    private Map<String, Word> primaryVocab;

    /**
     * Construct empty vocabulary.
     */
    public Vocabulary() {
        primaryVocab = new HashMap<String, Word>();
    }

    /**
     * Put given Word into vocabulary.
     * The word will be put into the primary map as a mapping from
     * word.getText() to word.
     * @param word Word to put into vocabulary
     * @return previous word associated with String word.getText(), or null
     *         if word.getText() was not in vocabulary
     */
    public Word put(Word word) {
        return primaryVocab.put(word.getText(), word);
    }

    /**
     * Copies all entries from the specified vocabulary to this vocabulary.
     * @param other Vocabulary with entries to be stored in this vocabulary
     */
    public void putAll(Vocabulary other) {
        primaryVocab.putAll(other.getPrimaryMap());
    }

    /**
     * Get primary map (Strings to Words) of this Vocabulary.
     * @return primary map (Strings to Words) of this Vocabulary
     */
    public Map<String, Word> getPrimaryMap() {
        return primaryVocab;
    }

    /**
     * Look up given text in vocabulary and return associated Word.
     * @param text String to look up
     * @return Word associated to given text if text is in vocabulary, or null
     *         if text is not in vocabulary
     */
    public Word lookUp(String text) {
        return primaryVocab.get(text);
    }

    /**
     * Test if this vocabulary contains the specified text.
     * @param text text whose presence in the vocabulary is to be tested
     * @return true if this vocabulary contains the specified text
     */
    public boolean contains(String text) {
        return primaryVocab.containsKey(text);
    }

    /**
     * Get the number of words in this vocabulary.
     * @return the number of words in this vocabulary
     */
    public int size() {
        return primaryVocab.size();
    }

    /**
     * Save this Vocabulary in given file.
     * @param file file to save in
     */
    public void save(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException in Vocabulary.save(). " +
                    "Vocabulary cannot be saved.", e);
        } catch (IOException e) {
            log.error("IOException in Vocabulary.save(). " +
                    "Vocabulary cannot be saved.", e);
        }
    }

    /**
     * Load Vocabulary from given file.
     * @param file file to load from
     * @return loaded Vocabulary
     */
    public static Vocabulary load(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object fileContent = ois.readObject();
            ois.close();
            if (fileContent instanceof Vocabulary) {
                return (Vocabulary) fileContent;
            } else {
                log.error("Vocabulary.load(); content of file = " + file +
                        " not a Vocabulary.");
            }
        } catch (FileNotFoundException e) {
            log.error("Vocabulary.load() " +
                    "FileNotFoundException; file = " + file, e);
        } catch (IOException e) {
            log.error("Vocabulary.load() " +
                    "IOException; file = " + file, e);
        } catch (ClassNotFoundException e) {
            log.error("Vocabulary.load() " +
                    "ClassNotFoundException; file = " + file, e);
        }
        return null;
    }
    /**
     * Returns a textual representation of this Vocabulary.
     * The textual representation will not be the full vocabulary, as this
     * is expected to be large. The textual representation will contain size,
     * the 100 first entries (lexicographical order) and every 100th entry
     * from then on.
     * @return a string representation of this vocabulary
     */
    public String toString() {
        String result = "Vocabulary size = " + size() + "\nVocabulary start:\n";
        Set<String> keys = primaryVocab.keySet();
        String[] keyArray = keys.toArray(new String[keys.size()]);
        Arrays.sort(keyArray);
        int startInt = 100;
        for (int i = 0; i<startInt && i<keyArray.length; i++) {
            result = result + primaryVocab.get(keyArray[i]) + "\n";
        }
        int interval = keyArray.length/100;
        result = result + "\nVocabulary words with " + interval + " interval:";
        for (int j = startInt; j<keyArray.length; j+=interval) {
            result = result + primaryVocab.get(keyArray[j]) + "\n";
        }
        return result;
    }
}
