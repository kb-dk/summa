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
package dk.statsbiblioteket.summa.common.xml;

import javax.xml.namespace.NamespaceContext;
import javax.xml.XMLConstants;
import java.util.*;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * For reasons beyond the frontiers of human intelligens, the XML api in JAVA only provide
 *  an Interface for the {@link javax.xml.namespace.NamespaceContext}.<br>
 *
 * This is the implementation that should have been provided in the API.<br>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class DefaultNamespaceContext implements NamespaceContext {

    private Map<String, Collection<String>> namespace;
    private Map<String, String> prefix;

    private String defaultNameSpaceURI;


    /**
     * Constructs a NamespaceContext with no default namespace.<br>
     * Be aware: Default namespace can only be set during construction.
     */
    public DefaultNamespaceContext(){
       namespace = new HashMap<String, Collection<String>>();
       prefix = new HashMap<String, String>();
       defaultNameSpaceURI = null;

       namespace.put(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, Arrays.asList(XMLConstants.XMLNS_ATTRIBUTE));
       prefix.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);

       namespace.put(XMLConstants.XML_NS_URI, Arrays.asList(XMLConstants.XML_NS_PREFIX));
       prefix.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);

    }

    /**
     * Constructs a NamespaceContext with a default namespace.<br>
     * @param defaultNamespaceURI , the default namespace fopr this context.
     */
    public DefaultNamespaceContext(String defaultNamespaceURI){
       namespace = new HashMap<String, Collection<String>>();
       prefix = new HashMap<String, String>();
       this.defaultNameSpaceURI = defaultNamespaceURI;

       namespace.put(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, Arrays.asList(XMLConstants.XMLNS_ATTRIBUTE));
       prefix.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);

       namespace.put(XMLConstants.XML_NS_URI, Arrays.asList(XMLConstants.XML_NS_PREFIX));
       prefix.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
    }

    /**
     * Set or add a namespace to the context and associated it with a prefix.<br>
     * A given prefix can only be associated with one namespace in the context.
     * A namespace can have multiple prefixes.<br>
     *
     * the prefixes: xml, and xmlns is reserved and predefined in any context.
     *
     * @param namespaceURL the namespace uri
     * @param prefix  the prifix to registere with the uri
     * @throws IllegalArgumentException thrown when trying to assign a namespace to reserved prefixes,
     * and thrown when trying to register a 'used' prefix.
     *
     */
    public void setNameSpace(String namespaceURL, String prefix) throws IllegalArgumentException {

        if (this.prefix.get(prefix) != null) {
            throw new IllegalArgumentException("prefix in use");
        }

        Collection<String> s = namespace.get(namespaceURL);

        if (s == null){
             s = new HashSet<String>();
        }

        s.add(prefix);
        namespace.put(namespaceURL,s);
        this.prefix.put(prefix,namespaceURL);

    }


    public String getNamespaceURI(String prefix) {
        if (prefix == null) { throw new IllegalArgumentException(); }

        if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX) && this.defaultNameSpaceURI != null)
            {
                return this.defaultNameSpaceURI;
            }


        if (!this.prefix.containsKey(prefix)) {
            return XMLConstants.NULL_NS_URI;
        }

        return this.prefix.get(prefix);

    }

    
    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null){ throw new IllegalArgumentException(); }

        if (namespaceURI.equals(defaultNameSpaceURI)) { return XMLConstants.DEFAULT_NS_PREFIX; }

        Collection <String> s = namespace.get(namespaceURI);
        if (s != null && !s.isEmpty()){
            return s.iterator().next();
        } else {
            return null;
        }
    }

    public Iterator getPrefixes(String namespaceURI) {

        if (namespaceURI == null){ throw new IllegalArgumentException(); }

        if (namespaceURI.equals(XMLConstants.XML_NS_URI)){
            return new NonModifiableIterator( Arrays.asList(XMLConstants.XML_NS_PREFIX).iterator());
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)){
            return new NonModifiableIterator( Arrays.asList(XMLConstants.XMLNS_ATTRIBUTE).iterator());
        }
        if (namespaceURI.equals(defaultNameSpaceURI)){
            return new NonModifiableIterator( Arrays.asList(XMLConstants.DEFAULT_NS_PREFIX).iterator());
        }

        Collection <String> s = namespace.get(namespaceURI);
        if (s!= null && !namespace.isEmpty()){
            return new NonModifiableIterator( s.iterator());
        } else {
            return new NonModifiableIterator(new HashSet().iterator());
        }
    }

    /**
     * This Iterator wrappes any Iterator and makes the remove() method unsupported.<br>
     *
     *
     * @author Hans Lund, State and University Library, Aarhus Denamrk.
     * @version $Id: DefaultNamespaceContext.java,v 1.5 2007/10/04 13:28:21 te Exp $
     * @see javax.xml.namespace.NamespaceContext#getPrefixes(String) 
     */
    class NonModifiableIterator implements Iterator{



        Iterator wrapped;

        NonModifiableIterator(Iterator iter){
            wrapped = iter;
        }

        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        /**
         * Returns the next element in the iteration.  Calling this method
         * repeatedly until the {@link #hasNext()} method returns false will
         * return each element in the underlying collection exactly once.
         *
         * @return the next element in the iteration.
         * @throws java.util.NoSuchElementException
         *          iteration has no more elements.
         */
        public Object next() {
            return wrapped.next();
        }

        /**
         * This method is not supported on this Iterator.
         *
         * Allways throws UnsupportedOperationException {@link javax.xml.namespace.NamespaceContext#getPrefixes(String)}
         *
         * @throws UnsupportedOperationException if the <tt>remove</tt>
         *                                       operation is not supported by this Iterator.
         */
        public void remove() {
            throw new UnsupportedOperationException("Conform to XML API please");
        }
    }
}




