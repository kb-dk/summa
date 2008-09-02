/* $Id: Target.java,v 1.9 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.9 $
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

import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A Target is a Ingest entry.
 * The target defines the basic properties for the meta-data to be ingested.<br>
 * For ingesting the target is coupled by a {@link dk.statsbiblioteket.summa.ingest.IngestContentHandler}
 *  where the content specific rules are defined.
 *
 *
 * A Target is partial immutable, the proper use of a Target is to create a new target -
 * populate the values, and then initialize the target before handing the target further in the processing chain.
 *
 * After initialization values cannot be changed, before initialization no values can be read.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public class Target{

    private Map<String, String> keyVal;

    private String name;
    private String encoding;
    private String directory;
    private boolean fullIngest;
    private boolean check;
    private String id_prefix;
    private String base;

    private boolean isImmutable;

    /**
     * Creates a new empty target.
     */
    public Target(){
        keyVal = new HashMap<String, String>();
        name = null;
        encoding = null;
        directory = null;
        fullIngest = false;
        check = true;
        id_prefix = "";
        base = "";
        isImmutable = false;
    }

    public synchronized void init() {
       if (isImmutable) { throw new IllegalStateException("target is initialized"); }
       isImmutable = true;
    }

    /**
     * Gets a map of any abstract key value pairs that might be defined for this target.<br>
     * This will normally be an empty Map, put can be used to contain target special properties.
     * @return  a Map of special properties for this target.
     */
    public Map<String, String> getKeyVal() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return keyVal;
    }

    /**
     * Sets a Map that must contain target specific special properties.
     * @param keyVal the Map with the properties
     */
    public synchronized void setKeyVal(Map<String, String> keyVal) {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        this.keyVal = keyVal;
    }

    /**
     * The name of the target.<br>
     * Target names should be describing for the target in place.
     * @return the name that the target has.
     */
    public String getName() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return name;
    }

    /**
     * Sets the name for the target.<br>
     * Use a describing name for the target.
     * @param name
     */
    public synchronized void setName(String name) {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        this.name = name;

    }

    /**
     * The encoding of the meta data this target will ingest.<br>
     * @return the encoding of the meta-data
     */
    public String getEncoding() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return encoding;
    }

    /**
     * Sets the encoding that will be used when reading data.<br>
     * format for the encoding is defined in the {@link java.nio.charset.Charset} specification.
     *
     * @param encoding use this when reading meta-data.
     * @throws UnsupportedEncodingException if trying to set a unknown encoding.
     * @see java.nio.charset.Charset
     */
    public synchronized void setEncoding(String encoding) throws UnsupportedEncodingException {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        if (!Charset.isSupported(encoding)) {
            throw new UnsupportedEncodingException(encoding + " unknown");
        }                
        this.encoding = encoding;

    }

    /**
     * Gets the local directory that is root for meta-data for this target.
     * By default ingesting will recursively traverse this directory to find meta-data.
     *
     * @return a String representation of the Directory
     */
    public String getDirectory() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return directory;
    }


    /**
     * Sets a directory that will be used as root directory for meta-data for this target.
     *
     * @param directory the meta-data root directory
     * @throws IOException if argument is not a directory or if the directory cannot be read.
     */
    public synchronized void setDirectory(String directory) throws IOException {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        File f = new File(directory);
        if (!f.isDirectory()) throw new IOException("unable to resolve" + directory  + "as a directory");
        if (!f.canRead()) throw new IOException("unable to read directory: " + directory);

        this.directory = directory;

    }

    /**
     * This is true if the fullIngest hint is set.<br>
     *
     * @return true if full ingest.
     */
    public boolean isFullIngest() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return fullIngest;
    }

    /**
     * Set a hint for the ingest work flow.<br>
     * Setting this to true allows the ingest process to optimize the ingest by assuming that no records exists prior to ingest.
     * This means that all records parsed to have {@link dk.statsbiblioteket.summa.common.Status#MODIFIED}
     * can be ingested as if {@link dk.statsbiblioteket.summa.common.Status#NEW} had been set. 
     *
     * @param fullIngest hint for optimizing ingest process
     * @see dk.statsbiblioteket.summa.common.Status
     */
    public synchronized void setFullIngest(boolean fullIngest) {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        this.fullIngest = fullIngest;
    }


    /**
     * The check flag is used to set the ingester i validation mode for content.
     * @return true if content validation is on.
     */
    public boolean isCheck() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return check;
    }

    /**
     * Set the check flag.
     * @param check true if record content is validated before ingest.
     */
    public synchronized void setCheck(boolean check) {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        this.check = check;
    }

    /**
     * Gets the static prefix, added to id's from this target.
     *
     * @return the id prefix.
     */
    public String getId_prefix() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return id_prefix;
    }

    /**
     * Sets a prefix used on all id's for records in this target.
     *
     * @param id_prefix all recordID's will be prefixed if this is not the empty String or null.
     */
    public synchronized void setId_prefix(String id_prefix) {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        this.id_prefix = id_prefix;
    }

    /**
     * adds an key,value pair, that a given implementation needs.
     * @param key  the key.
     * @param val  the value.
     */
    public synchronized void add(String key, String val){
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        keyVal.put(key,val);
    }

    /**
     * Get the value mapped to the key.
     * @param key the key.
     * @return  the value.
     */
    public String get(String key){
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return keyVal.get(key);
    }

    /**
     * Gets the base for this target.
     *
     * @return the base that the record will be registered to.
     */
    public String getBase() {
        if (!isImmutable) { throw new IllegalStateException("target not initialized"); }
        return base;
    }

    /**
     * The base will be used to associate records to additional resources and work flows.
     *
     * @param base the base name.
     */
    public synchronized void setBase(String base) {
        if (isImmutable) { throw new IllegalStateException("target is initialized"); }
        this.base = base;
    }

    /**
     * The createEqualTarget is a 'near' clone() method for a target.<br>
     * Every value in the returned Target will be equal to values in this Target. 
     *
     * The returned Target will always be in non-initialized state.
     *
     * @return
     */
    public synchronized Target createEqualTarget(){

        Target equal = new Target();
        equal.base = base;
        equal.check = check;
        equal.directory = directory;
        equal.encoding = encoding;
        equal.fullIngest = fullIngest;
        equal.id_prefix = id_prefix;
        equal.name = name;
        for (Map.Entry<String, String> e : keyVal.entrySet()){
            equal.add(e.getKey(), e.getValue());
        }
        return equal;
        
    }


    /**
     *
     * Returns true iff:  Object o is a Target, and all literal values are equal, and all boolean values are identical.
     *
     * @param o
     * @return
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Target target = (Target) o;

        if (check != target.check) return false;
        if (fullIngest != target.fullIngest) return false;
        if (base != null ? !base.equals(target.base) : target.base != null) return false;
        if (directory != null ? !directory.equals(target.directory) : target.directory != null) return false;
        if (encoding != null ? !encoding.equals(target.encoding) : target.encoding != null) return false;
        if (id_prefix != null ? !id_prefix.equals(target.id_prefix) : target.id_prefix != null) return false;
        if (keyVal != null ? !keyVal.equals(target.keyVal) : target.keyVal != null) return false;
        if (name != null ? !name.equals(target.name) : target.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (keyVal != null ? keyVal.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
        result = 31 * result + (directory != null ? directory.hashCode() : 0);
        result = 31 * result + (fullIngest ? 1 : 0);
        result = 31 * result + (check ? 1 : 0);
        result = 31 * result + (id_prefix != null ? id_prefix.hashCode() : 0);
        result = 31 * result + (base != null ? base.hashCode() : 0);
        return result;
    }
}
