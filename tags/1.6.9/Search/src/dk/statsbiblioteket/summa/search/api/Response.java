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

import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A response from a SearchNode contains SearchNode specific content. This class
 * should is responsible for merging the content of this response with another
 * search response and converting the response to a XML block.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        reviewers = {"hbk"})
public interface Response extends Serializable {
    /**
     * @return the name of the response. Responses instantiated from the same
     *         class are required to return the same name, so that name
     *         equality means that {@link #merge} can be safely called.
     *         Names are required to be unique across classes.
     */
    public String getName();

    /**
     * Merge the content from other into this Response, sorting and trimming
     * when necessary.
     * @param other The Response to merge into this.
     * @throws ClassCastException if other was not assignable to this.
     */
    public void merge(Response other) throws ClassCastException;

    /**
     * The XML returned should be an XML-snippet: UTF-8 is used and no header
     * should be included. A proper response could be:
     * <pre>
        <myresponse>
            <hits total="87">
            <hit>Foo</hit>
            ...
        </myresponse>
     * </pre>
     *
     * @return the content of Response as XML.
     */
    public String toXML();
}




