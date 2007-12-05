/* $Id: PersistentHashMap.java,v 1.4 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.4 $
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
package dk.statsbiblioteket.summa.tools.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.FileInputStream;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * PersistentHashMap.
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class PersistentHashMap extends HashMap {

    private HashMap underlyingMap;
    private File serializeDir;
    private static final char path_sep = '/';
    private static final char path_sep_replacement = '_';
    private static final String underlyingMap_serialize_name = "underlyingMap.instance";


    private static final Log log = LogFactory.getLog(PersistentHashMap.class);

        private void serializeUnderlyingMap() {
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(new FileOutputStream(serializeDir.getPath() + File.separator + underlyingMap_serialize_name));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            try {
                assert out != null;
                out.writeObject(underlyingMap);
                out.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Values must be serializable " + e.getMessage());
            }

        }


        public PersistentHashMap(String directory) throws IOException {
            underlyingMap = new HashMap();
            serializeDir = new File(directory);
            if (!serializeDir.isDirectory() || !serializeDir.canWrite()) {
                throw new IOException("unable to use tmp dir: " + directory);
            }

            File serialized_map = new File(directory + File.separator + underlyingMap_serialize_name);
            if (serialized_map.exists()) {
                ObjectInputStream in;
                try {
                    in = new ObjectInputStream(new FileInputStream(serialized_map));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    underlyingMap = (HashMap) in.readObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    log.info("Unable to resume LowMemMap - serialized file contains wrong class", e);
                }
            }
        }

        public int size() {
            return underlyingMap.size();
        }

        public void clear() {
            underlyingMap.clear();
            serializeUnderlyingMap();
        }

        public boolean isEmpty() {
            return underlyingMap.isEmpty();
        }

        public boolean containsKey(final Object key) {
            return underlyingMap.containsKey(key);
        }

        public boolean containsValue(final Object value) {
            throw new UnsupportedOperationException("not implemented");
        }

        public Collection values() {
            throw new UnsupportedOperationException("not implemented");
        }

        public void putAll(final Map t) {
            throw new UnsupportedOperationException("not implemented");
        }

        public Set entrySet() {
            throw new UnsupportedOperationException();
        }

        public Set keySet() {
            return underlyingMap.keySet();
        }

        public Object get(final Object key) {

            final String fileName = (String) underlyingMap.get(key);
            if (fileName == null) {
                return null;
            }
            ObjectInputStream in;
            try {
                in = new ObjectInputStream(new FileInputStream(fileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                return in.readObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        public Object remove(final Object key) {
            final Object obj = get(key);
            underlyingMap.remove(key);
            serializeUnderlyingMap();
            return obj;
        }

        public Object put(final Object key, final Object value) {
            final Object ret = get(key);
            final String fileName = serializeDir + File.separator + key.toString().replace(path_sep, path_sep_replacement) + ".obj";
            final File f = new File(fileName);
            try {
                f.createNewFile();
            } catch (IOException e) {
                log.error(e.getMessage(),e);
            }
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(new FileOutputStream(f));
            } catch (IOException e) {
                log.error(e.getMessage(),e);
            }
            try {
                assert out != null;
                out.writeObject(value);
                out.close();
                underlyingMap.put(key, fileName);
                serializeUnderlyingMap();
            } catch (IOException e) {
                throw new IllegalArgumentException("Values must be serializable " + e.getMessage());
            }
            return ret;
        }
    }
