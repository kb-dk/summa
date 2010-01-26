/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.search.api;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import java.io.StringWriter;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A collection of Responses with auto-merging: If a Response with name A is
 * added and another Response with name A already exists, the newest Response
 * is merged into the old one.
 * </p><p>
 * This implementation is thread-safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment="Better class description needed  ")
public class ResponseCollection implements Collection<Response>, Serializable {
    private static Log log = LogFactory.getLog(Response.class);

    private Map<String, Response> responses = new HashMap<String, Response>(5);
    private transient Map<String, Object> tran = new HashMap<String, Object>(5);

    /**
     * All contained Responses are iterated and {@link Response#toXML} is called
     * for each. The results are merged in the following structure:
     * {@code
    <?xml version="1.0" encoding="UTF-8" ?>
    <responsecollection>
    <response>
    response-xml-1
    </response>
    <response>
    response-xml-2
    </response>
    ...
    <responsecollection>
     }
     * @return an XML-representation of all the underlying Responses.
     */
    public synchronized String toXML() {
        StringWriter sw = new StringWriter(5000);
        sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sw.append("<responsecollection>\n");
        // TODO: We really want to thate the namespace!
//        sw.append("<responsecollection xmlns:\"http://statsbiblioteket.dk/summa/2009/SearchResponse\">\n");
        for (Map.Entry<String, Response> entry: responses.entrySet()) {
            sw.append("<response name=\"").append(entry.getValue().getName());
            sw.append("\">\n");
            sw.append(entry.getValue().toXML());
            sw.append("</response>\n");
        }
        sw.append("</responsecollection>\n");
        return sw.toString();
    }

    /**
     * @return a map used for transient data. Useful for storing intermediate
     *         values between SearchNodes.
     */
    public Map<String, Object> getTransient() {
        return tran;
    }

    /* Collection interface */

    public synchronized int size() {
        return responses.size();
    }

    public synchronized boolean isEmpty() {
        return responses.isEmpty();
    }

    public synchronized boolean contains(Object o) {
        //noinspection SuspiciousMethodCalls
        return !(o == null || !(o instanceof Response))
               && responses.containsValue(o);
    }

    /**
     * The iterator moves over a shallow copy, so remove() has no effect.
     * @return an iterator for the contained Responses.
     */
    public synchronized Iterator<Response> iterator() {
        return toList().iterator();
    }

    /**
     * @return a shallow copy of the underlying Responses.
     */
    private synchronized List<Response> toList() {
        List<Response> responses = new ArrayList<Response>(size());
        for (Map.Entry<String, Response> entry: this.responses.entrySet()) {
            responses.add(entry.getValue());
        }
        return responses;
    }

    public synchronized Object[] toArray() {
        return toList().toArray();
    }

    public synchronized <T> T[] toArray(T[] a) {
        //noinspection SuspiciousToArrayCall
        return toList().toArray(a);
    }

    public synchronized boolean add(Response response) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding '" + response + "' to collection");
        Response oldResponse = responses.get(response.getName());
        if (oldResponse == null) {
            responses.put(response.getName(), response);
            return true;
        }
        //noinspection ObjectEquality
        if (oldResponse == response) {
            return false;
        }
        oldResponse.merge(response);
        return true;
    }

    public synchronized boolean remove(Object o) {
        if (o == null || !(o instanceof Response)) {
            return false;
        }
        String name = ((Response)o).getName();
        return responses.remove(name) != null;
    }

    public synchronized boolean containsAll(Collection<?> c) {
        for (Object o: c) {
            if (contains(o)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean addAll(Collection<? extends Response> c) {
        boolean changed = false;
        for (Response r: c) {
            changed = changed | add(r);
        }
        return changed;
    }

    public synchronized boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o: c) {
            changed = changed | remove(o);
        }
        return changed;
    }

    public synchronized boolean retainAll(Collection<?> c) {
        // A bit too much hacking gere
        int startLength = responses.size();
        List<Response> responses = toList();
        responses.retainAll(c);
        clear();
        addAll(responses);
        return startLength != this.responses.size();
    }

    public synchronized void clear() {
        responses.clear();
    }
}




