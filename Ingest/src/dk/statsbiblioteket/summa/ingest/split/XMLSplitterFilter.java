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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Uses a streaming XML-parser to split the given streams into {@link Record}s.
 * </p><p>
 * The input should be a valid XML-document where the individual records are
 * listed sequentially.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLSplitterFilter extends StreamController {
    private static Log log = LogFactory.getLog(XMLSplitterFilter.class);

    /**
     * The prefix to prepend to the extracted Record-ids.
     * </p><p>
     * Optional. Default is "".
     * @see #EXPAND_FOLDER
     */
    public static final String CONF_ID_PREFIX =
            "summa.ingest.xmlsplitter.idprefix";

    /**
     * The postfix to append to the extracted Record-ids.
     * </p><p>
     * Optional. Default is "".
     * @see #EXPAND_FOLDER
     */
    public static final String CONF_ID_POSTFIX =
            "summa.ingest.xmlsplitter.idpostfix";

    /**
     * If this String is used inside {@link #CONF_ID_PREFIX} or
     * {@link #CONF_ID_POSTFIX}, the last path element of the origin will
     * replace the String. If ORIGIN is not present, the String will be replaced
     * with "".
     * </p></p>
     * Example: An XMLSplitterFilter is positioned after a
     * {@link dk.statsbiblioteket.summa.ingest.stream.FileReader}.
     * FileReader adds the folder for the fetched file as the meta-data element
     * {@link dk.statsbiblioteket.summa.common.filter.Payload#ORIGIN}.
     * If the ORIGIN is "/home/summa/data/mysource/specialfolder/somefile.xml",
     * the last path element is "specialfolder".
     */
    public static final String EXPAND_FOLDER = "{ORIGIN_LAST_PATH_ELEMENT}";

    /**
     * If true, extracted ids which beginning matches {@link #CONF_ID_PREFIX}
     * will not have the prefix prepended.
     * </p><p>
     * Example: Prefix is "foo:" and collapse is true:<br />
     *          "barbarella"   => "foo:barbarella"<br />
     *          "foo:fighters" => "foo:fighters"<br />
     *          Prefix is "foo:" and collapse is false:<br />
     *          "barbarella"   => "foo:barbarella"<br />
     *          "foo:fighters" => "foo:foo:fighters"<br />
     * </p><p>
     * Default: "true".
     */
    public static final String CONF_COLLAPSE_PREFIX =
            "summa.ingest.xmlsplitter.collapseprefix";

    /**
     * If true, extracted ids which beginning matches {@link #CONF_ID_POSTFIX}
     * will not have the postfix appended.
     * </p><p>
     * Default: "true".
     * @see #CONF_COLLAPSE_PREFIX
     */
    public static final String CONF_COLLAPSE_POSTFIX =
            "summa.ingest.xmlsplitter.collapsepostfix";


    /**
     * The xml-element containing the individual records. The input stream will
     * be processed sequentially and all elements matching this value will be
     * regarded as a record.
     * </p><p>
     * Default: "record".
     */
    public static final String CONF_RECORD_ELEMENT =
            "summa.ingest.xmlsplitter.recordelement";

    /**
     * The XML namespace of the RECORD element matched by
     * {@link #CONF_RECORD_ELEMENT}.
     * If this property is unset RECORDs will be extracted as descibed in
     * {@link #CONF_RECORD_ELEMENT}.
     * </p><p>
     * Default: {@code null}.
     */
    public static final String CONF_RECORD_NAMESPACE =
            "summa.ingest.xmlsplitter.recordelementnamespace";

    /**
     * The id-element containing the id for a given record. The element must be
     * present inside the record-element. If a tag inside an element is used for
     * id, the syntax is element#tag.
     * </p><p>
     * Example 1: "foo" matches <foo>myid</foo> and returns myid.<br />
     * Example 2: "foo#bar" matches <foo bar="myid">whatever</foo> and returns
     *             myid.<br />
     * </p><p>
     * The element can be qualified or non-qualified. Non-qualified ids will
     * match qualified documents, but not the other way around.
     * </p><p>
     * Example 3: "foo" matches <baz:foo>myid</baz:foo> and returns myid. 
     * Example 4: "baz:foo" does not match <foo>myid</foo>.
     * </p><p>
     * If the id-element is set to the empty string, a dummy-ID will be assigned
     * to generated Records.
     * </p><p>
     * Optional. Default: "id".
     */
    public static final String CONF_ID_ELEMENT =
            "summa.ingest.xmlsplitter.idelement";

    /**
     * The XML namespace of the ID element matched by {@link #CONF_ID_ELEMENT}.
     * If this property is unset IDs will be extracted as descibed in
     * {@link #CONF_ID_ELEMENT}.
     * </p><p>
     * Default: {@code null}.
     */
    public static final String CONF_ID_NAMESPACE =
            "summa.ingest.xmlsplitter.idelementnamespace";

    /**
     * The base for the constructed Records.
     * </p><p>
     * This option is mandatory.
     */
    public static final String CONF_BASE = "summa.ingest.xmlsplitter.base";

    /**
     * If true, name space definitions are copied to the Record XML blocks.
     * If false, name spaces are stripped.
     * </p><p>
     * Default: "true".
     */
    public static final String CONF_PRESERVE_NAMESPACES =
            "summa.ingest.xmlsplitter.preservenamespaces";

    /**
     * If true, the XML-blocks must be valid. That means, other than being
     * well-formed, that at least one DTD must be specified and that the XML
     * must conform to those.
     * </p><p>
     * If CONF_REQUIRE_VALID is true, a fitting value for VALID_XML will be
     * added to the meta-info for the generated Records. If CONF_REQUIRE_VALID
     * is false, no value will be added to meta-info.
     * </p><p>
     * Default: "false".
     */
    public static final String CONF_REQUIRE_VALID =
            "summa.ingest.xmlsplitter.requirevalid";

    // TODO: Properties with default namespaces?

    /**
     * The key for meta-info in Record. Valid values are "true" and "false".
     */
    public static final String VALID_XML = "valid_xml";

    public XMLSplitterFilter(Configuration conf) {
        super(conf);
        log.info("Created XMLSplitterFilter");
    }

    @Override
    protected Class<? extends StreamParser> getDefaultStreamParserClass() {
        return XMLSplitterParser.class;
    }
}

