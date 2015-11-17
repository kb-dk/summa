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

import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Convenience class for keeping track of timing information and making it easier to provide XML output.
 * </p><p>
 * Implementing classes should either override {@link #toXML()} or {@link #toXML(XMLStreamWriter)}.
 * </p><p>
 * Remember to call super.merge(Response) when implementing merge in order to merge timing information.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
abstract public class ResponseImpl extends TimerImpl implements Response, Serializable {
    private static transient XMLOutputFactory xmlOutFactory;
    private transient ByteArrayOutputStream os;

    /**
     * Constructor without explicit prefix. {@link #getName()} + "." will be
     * used for prefix, but will be lazily requested.
     */
    protected ResponseImpl() {
        this(null);
    }

    /**
     * @param prefix will be appended to all timing information. Recommended
     * prefix is "uniquename.".
     */
    protected ResponseImpl(String prefix) {
        super(prefix);
    }

    @Override
    public synchronized void merge(Response other) throws ClassCastException {
        if (other.getTiming() == null) {
            return;
        }
        super.addTiming(other.getTiming());
    }

    @Override
    public String toXML() {
        checkTransient();
        os.reset();
        XMLStreamWriter xml;
        try {
            xml = xmlOutFactory.createXMLStreamWriter(os);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Unable to create XMLStreamWriter", e);
        }
        try {
            toXML(xml);
            xml.flush();
        } catch (XMLStreamException e) {
            throw new RuntimeException("XMLStreamException", e);
        }
        try {
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unable to flush output stream", e);
        }
        return os.toString();
    }

    protected synchronized void checkTransient() {
        if (xmlOutFactory == null) {
            xmlOutFactory = XMLOutputFactory.newInstance();
        }
        os = new ByteArrayOutputStream();
    }


    public void toXML(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeComment("Summa error: toXML not implemented for " + getName());
    }

    // Convenience methods for generating XML
    protected static void start(XMLStreamWriter xml, String tag, String... attributePairs) throws XMLStreamException {
        if (attributePairs.length % 2 == 1) {
            throw new IllegalArgumentException(
                    "attributePairs should contain an even number but had " + attributePairs.length + " elements");
        }
        xml.writeStartElement(tag);
        for (int i = 0 ; i < attributePairs.length ; i+=2) {
            xml.writeAttribute(attributePairs[i], attributePairs[i+1]);
        }
    }
    protected static void startln(XMLStreamWriter xml, String tag, String... attributePairs) throws XMLStreamException {
        start(xml, tag, attributePairs);
        xml.writeCharacters("\n");
    }
    public static void endln(XMLStreamWriter xml) throws XMLStreamException {
        endln(xml, "");
    }
    public static void endln(XMLStreamWriter xml, String indent) throws XMLStreamException {
        xml.writeCharacters(indent);
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    @Override
    public String getPrefix() {
        if (super.getPrefix() == null) {
            setPrefix(getName() + ".");
        }
        return super.getPrefix();
    }

}
