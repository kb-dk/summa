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
package dk.statsbiblioteket.summa.support.enrich;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.MARCObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.marc.MARCObject;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLStepper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Highly specific filter that takes Records with MARC21Slim XML, performs an external lookup for whether a password
 * is required to access the article described by the MARC and adjusts the XML to reflect this requirement.
 * </p><p>
 * Fields 856*p will be updated with the generated IDs.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ETSSStatusFilter extends MARCObjectFilter {
    private static Log log = LogFactory.getLog(ETSSStatusFilter.class);

    /**
     * If true the filter chain is shut down if the external ETSS service fails to respond.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_HALT_ON_EXTERNAL_ERROR = "etss.failonexternalerror";
    public static final boolean DEFAULT_HALT_ON_EXTERNAL_ERROR = false;

    /**
     * The REST call to perform. In the call, $ID_AND_PROVIDER will be replaced by the ID of the MARC record merged with
     * the provider, both normalised.
     * </p><p>
     * Mandatory. Example:
     * "http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_$ID_AND_PROVIDER".
     * @see {@link #normaliseProvider(String)}.
     *      */
    public static final String CONF_REST = "etss.service.rest";

    /**
     * The timeout in milliseconds for establishing a connection to the remote ETSS.
     * </p><p>
     * Optional. Default is 2000 milliseconds.
     */
    public static final String CONF_ETSS_CONNECTION_TIMEOUT = "etss.connection.timeout";
    public static final int DEFAULT_ETSS_CONNECTION_TIMEOUT = 2000;
    /**
     * The timeout in milliseconds for receiving data after a connection has been established to the ETSS service.
     * </p><p>
     * Optional. Default is 8000 milliseconds.
     */
    public static final String CONF_ETSS_READ_TIMEOUT = "etss.read.timeout";
    public static final int DEFAULT_ETSS_READ_TIMEOUT = 8000;

    /**
     * If true, all fields 856*k, 856*w and 856*l are cleared before processing. This is normally used in conjunction
     * with {@link #CONF_DISCARD_UNCHANGED} to build a batch refresher of password requirements.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_CLEAN_PREVIOUS_STATUS = "etss.cleanpreviousstatus";
    public static final boolean DEFAULT_CLEAN_PREVIOUS_STATUS = false;

    /**
     * If true, Records that are not changed by the password update are marked as discardable.
     * If this is true, it is recommended to set {@link #CONF_CLEAN_PREVIOUS_STATUS} to true.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_DISCARD_UNCHANGED = "etss.discardunchanged";
    public static final boolean DEFAULT_DISCARD_UNCHANGED = false;

    /**
     * If true, Records marked as deleted are skipped. Note that setting this to true means that the deletion of an
     * existing record will not result in an index update.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_DISCARD_DELETED = "etss.discarddeleted";
    public static final boolean DEFAULT_DISCARD_DELETED = false;

    public static final String PASSWORD_SUBFIELD = "k"; // In 856
    public static final String PASSWORD_CONTENT = "password required";
    public static final String PROVIDER_SPECIFIC_ID = "w"; // Record Control Number in 856
    public static final String COMMENT_SUBFIELD = "l"; // In 856

    /**
     * The wrapping format for the answer from the service.
     * </p><p>
     * Optional. Valid values are {@code soap} and {@code json}. Default is {@code soap}.
     */
    public static final String CONF_RETURN_PACKAGING = "etss.returnpackaging";
    public static final String DEFAULT_RETURN_PACKAGING = RETURN_PACKAGING_FORMAT.soap.toString();

    public enum RETURN_PACKAGING_FORMAT {soap, json}

    protected final int connectionTimeout;
    protected final int readTimeout;
    protected final boolean haltOnError;
    protected final boolean cleanPrevious;
    protected final boolean discardUnchanged;
    protected final boolean discardDeleted;
    protected final String rest;
    protected final RETURN_PACKAGING_FORMAT packaging;

    public ETSSStatusFilter(Configuration conf) {
        super(conf);
        feedback = false;
        rest = conf.getString(CONF_REST);
        connectionTimeout = conf.getInt(CONF_ETSS_CONNECTION_TIMEOUT, DEFAULT_ETSS_CONNECTION_TIMEOUT);
        readTimeout = conf.getInt(CONF_ETSS_READ_TIMEOUT, DEFAULT_ETSS_READ_TIMEOUT);
        haltOnError = conf.getBoolean(CONF_HALT_ON_EXTERNAL_ERROR, DEFAULT_HALT_ON_EXTERNAL_ERROR);
        cleanPrevious = conf.getBoolean(CONF_CLEAN_PREVIOUS_STATUS, DEFAULT_CLEAN_PREVIOUS_STATUS);
        discardUnchanged = conf.getBoolean(CONF_DISCARD_UNCHANGED, DEFAULT_DISCARD_UNCHANGED);
        discardDeleted = conf.getBoolean(CONF_DISCARD_DELETED, DEFAULT_DISCARD_DELETED);
        packaging = RETURN_PACKAGING_FORMAT.valueOf(conf.getString(CONF_RETURN_PACKAGING, DEFAULT_RETURN_PACKAGING));
        log.info(String.format("Constructed filter with REST='%s', haltOnError=%b, cleanPrevious=%b, "
                               + "discardUnchanged=%b, discardDeleted=%b, return packaging format=%s",
                               rest, haltOnError, cleanPrevious, discardUnchanged, discardDeleted,
                               packaging));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (discardDeleted && payload.getRecord() != null && payload.getRecord().isDeleted()) {
            return false;
        }
        return super.processPayload(payload);    // TODO: Implement this
    }

    @Override
    protected MARCObject adjust(Payload payload, MARCObject marc) {
        long checkTime = -System.currentTimeMillis();
        MARCObject original;
        try {
            original = discardUnchanged ? (MARCObject) marc.clone() : null;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException("Unable to clone MARC record", e);
        }
        if (cleanPrevious) {
            clean(marc);
        }

        String recordID = getID(marc);
        if (recordID == null) {
            Logging.logProcess(
                "ETSSStatusFilter",
                "Unable to extract ID from fields 022*a, 022*l, 022*x or 245*a. No adjustment performed",
                Logging.LogLevel.WARN, payload);
            return marc;
        }
        List<MARCObject.DataField> urls = marc.getDataFields("856");
        List<MARCObject.DataField> providers = marc.getDataFields("980");
        if (urls.size() != providers.size()) {
            Logging.logProcess(
                "ETSSStatusFilter",
                "There were " + urls.size() + " fields with tag 856 and " + providers.size()
                + " fields with tag 980. There should be the same number. The status is left unadjusted",
                Logging.LogLevel.WARN, payload);
            return marc;
        }
        List<String> needs = new ArrayList<>(providers.size());
        List<String> noneeds = new ArrayList<>(providers.size());
        for (int i = 0 ; i < urls.size() ; i++) {
            try {
                enrich(recordID, urls.get(i), providers.get(i), needs, noneeds);
            } catch (Exception e) {
                log.warn("Unable to request password requirement for " + payload, e);
                Logging.logProcess("ETSSStatusFilter", "Unable to request password requirement",
                                   Logging.LogLevel.WARN, payload, e);
                if (haltOnError) {
                    String message =
                        "IOException when requesting password requirements for " + payload + ". "
                        + CONF_HALT_ON_EXTERNAL_ERROR + " is true so the JVM will be shut down in 5 seconds";
                    Logging.fatal(log, "ETSSStatusFilter.adjust", message, e);
                    System.err.println(message);
                    e.printStackTrace(System.err);
                    new DeferredSystemExit(1, 5000);
                    throw new RuntimeException(message, e);
                }
            }
        }
        checkTime += System.currentTimeMillis();
        boolean discard = discardUnchanged && original.equals(marc);
        if (Logging.processLog.isDebugEnabled()) {
            Logging.logProcess("ETSSStatusFilter",
                               "Checked password requirement for " + urls.size() + " providers for record "
                               + recordID + " in " + checkTime + " ms. " + needs.size()
                               + " providers explicitly needs password (" + Strings.join(needs, ", ") + "). "
                               + noneeds.size()
                               + " providers might not need passwords (" + Strings.join(noneeds, ", ") + ")"
                               + (discard ? ". Requirements has not changed and the record will be discarded"
                                          : ". Keeping record"),
                               Logging.LogLevel.DEBUG, payload);
        }
        return discard ? null : marc;
    }

    /**
     * Enriches the url-field with data derived from the provider-field. Enrichments include lookup-ID, password
     * requirement and comment from the password service response.
     * @param recordID            used for logging messages.
     * @param url                 the field containing the URL of the provider, to be extended.
     * @param provider            the field containing provider information such as ID.
     * @param needsPassword       if a password is needed, the lookup-URI is stored here.
     * @param doesNotNeedPassword if a password is not needed, the lookup-URI is stored here.
     * @throws IOException in case of network errors.
     */
    private void enrich(String recordID, MARCObject.DataField url, MARCObject.DataField provider,
                        List<String> needsPassword, List<String> doesNotNeedPassword) throws IOException {
        String providerPlusID = getProviderPlusId(recordID, provider);
        String lookupURI = getLookupURI(recordID, providerPlusID);
        if (lookupURI == null) {
            doesNotNeedPassword.add("Unresolved(" + recordID + ")");
            return;
        }
        String response = lookup(recordID, lookupURI);

        List<MARCObject.SubField> subFields = url.getSubFields();
        if (providerPlusID != null) {
            subFields.add(new MARCObject.SubField(PROVIDER_SPECIFIC_ID, providerPlusID));
        }

        if (response != null && !"".equals(response)) {
            String comment = getComment(response);
            if (comment != null) {
                subFields.add(new MARCObject.SubField(COMMENT_SUBFIELD, comment));
            }
            if (needsPassword(response)) {
                subFields.add(new MARCObject.SubField(PASSWORD_SUBFIELD, PASSWORD_CONTENT));
                needsPassword.add(lookupURI);
                return;
            }
        }
        Logging.logProcess("ETSSStatusFilter.enrich", String.format(
                "No requirements for lookupURI '%s'. Marking sub field as not needing password", lookupURI),
                           Logging.LogLevel.DEBUG, recordID);
        doesNotNeedPassword.add(lookupURI);
    }

    // Cleans all previous password enrichments
    private void clean(MARCObject marc) {
        for (MARCObject.DataField urls: marc.getDataFields("856")) {
            for (int i = urls.getSubFields().size()-1 ; i >= 0 ; i--) {
                String code = urls.getSubFields().get(i).getCode();
                if (PASSWORD_SUBFIELD.equals(code)
                    || COMMENT_SUBFIELD.equals(code)
                    || PROVIDER_SPECIFIC_ID.equals(code)) {
                    urls.getSubFields().remove(i);
                }
            }
        }
    }

    private HttpClient http = new DefaultHttpClient();

    /*
    SOAP:
    <?xml version="1.0" encoding="UTF-8"?>
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <soapenv:Body>
    <getFromDBResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <getFromDBReturn xsi:type="soapenc:string" xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/">
    &lt;info&gt;&lt;username&gt;lsa@statsbiblioteket.dk&lt;/username&gt;&lt;password&gt;dame7896&lt;/password&gt;&lt;group&gt;&lt;/group&gt;&lt;comment&gt;Dette er en kommentar&lt;/comment&gt;&lt;/info&gt;</getFromDBReturn>
    </getFromDBResponse>
    </soapenv:Body>
    </soapenv:Envelope>

     JSON:
    [{"key":"genericderby.access_etss_0011-5477_sciencntercentersandsonlinepublishermedicaljournals",
      "value":"<info><username>1234</username><password>abcdefg</password><group></group><comment></comment></info>",
      "modified":1434527979547,"expire":9999999999999,"expired":false}]
     */
    // Returns remote service response for the given recordID. Null is a valid response
    // This methods unpacks the content from SOAP
    protected String lookup(String recordID, String lookupURI) throws IOException {
        HttpGet method = new HttpGet(lookupURI);
        String response;
        try {
            Long readTime = -System.currentTimeMillis();
            HttpResponse httpResponse = http.execute(method);
            if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(String.format(
                    "Expected return code %d, got %d for '%s' with call %s",
                    HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode(), recordID, lookupURI));
            }
            readTime += System.currentTimeMillis();
            if (log.isTraceEnabled()) {
                log.trace("Completed ETSS call to '" + lookupURI + "' in " + readTime + "ms");
            }

            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                Logging.logProcess("ETSSStatusFilter.lookup", "No response from request " + lookupURI,
                                   Logging.LogLevel.WARN, recordID);
                return null;
            }
            response = Strings.flush(entity.getContent());
            entity.getContent().close();
        } catch (IOException e) {
            throw new IOException("Unable to connect to remote ETSS service with URI '" + lookupURI + "'", e);
        }
        if (response == null) {
            return null;
        }
        return unwrap(response, recordID);
    }

    //    [{"key":"genericderby.access_etss_0011-5477_sciencntercentersandsonlinepublishermedicaljournals",
    //      "value":"<info><username>1234</username><password>abcdefg</password><group></group><comment></comment></info>",
    //      "modified":1434527979547,"expire":9999999999999,"expired":false}]

    private String unwrap(String response, String recordID) {
        switch (packaging) {
            case soap: return unwrapSOAP(response, recordID);
            case json: return unwrapJSON(response, recordID);
            default: throw new UnsupportedOperationException(
                    "The return packaging format '" + packaging + "' is unknown");
        }
    }

    private String unwrapJSON(String response, String recordID) {
        if (!(response.startsWith("[") && response.endsWith("]"))) {
            Logging.logProcess("ETSSStatusFilter.unwrapJSON",
                               "Expected a response starting with '[' and ending with ']': '"
                               + Logging.getSnippet(response) + "'", Logging.LogLevel.WARN, recordID);
            return null;
        }
        response = "{\"list\":" + response + "}"; // Yes, ugly, but the input format is not valid JSON


        JSONObject json = JSONObject.fromObject(response);
        JSONObject inner;
        try {
            inner = (JSONObject)((JSONArray)json.get("list")).get(0);

            //String key = inner.get("key").toString(); // Should we check the key?
            String value = inner.get("value").toString();
            return value;
        } catch (Exception e) {
            Logging.logProcess("ETSSStatusFilter.unwrapJSON",
                               "Unable to extract key and value from '" + Logging.getSnippet(response)
                               + "' due to exception " + e.getMessage(),
                               Logging.LogLevel.DEBUG, recordID);
            return null;
        }
/*
        if (json.isEmpty()) {
            Logging.logProcess("ETSSStatusFilter.unwrapJSON",
                               "Empty JSON response: '" + Logging.getSnippet(response) + "'",
                               Logging.LogLevel.DEBUG, recordID);
            return null;
        }
        Set outerCol = json.entrySet();
        if (outerCol.isEmpty()) {
            Logging.logProcess("ETSSStatusFilter.unwrapJSON",
                               "Empty top level collection in JSON response: '" + Logging.getSnippet(response) + "'",
                               Logging.LogLevel.DEBUG, recordID);
            return null;
        }
        Object listObject = outerCol.iterator().next();
        if (!(listObject instanceof ListOrderedMap.Entry)) {
            Logging.logProcess("ETSSStatusFilter.unwrapJSON",
                               "Expected a ListOrderedMap.Entry in JSON but got " + listObject.getClass()
                               + " from response: '" + Logging.getSnippet(response) + "'",
                               Logging.LogLevel.DEBUG, recordID);
            return null;
        }
        ListOrderedMap.Entry lomEntry = (ListOrderedMap.Entry)listObject;
        Object jrObject = lomEntry.getValue();
        if (jrObject == null) {
            log.error("unwrapJSON wrapping error. Empty value in ListOrderedMap.Entry in JSON from response: '"
                      + Logging.getSnippet(response) + "' for " + recordID);
            Logging.logProcess("ETSSStatusFilter.unwrapJSON",
                               "Wrapping error. Empty value in ListOrderedMap.Entry from response: '"
                               + Logging.getSnippet(response) + "'", Logging.LogLevel.WARN, recordID);
            return null;
        }
        if (!(jrObject instanceof JSONArray)) {
            Logging.logProcess("ETSSStatusFilter.unwrapJSON",
                               "Expected a JSONArray in JSON but got " + jrObject.getClass() + " from response: '"
                               + Logging.getSnippet(response) + "'", Logging.LogLevel.DEBUG, recordID);
            return null;
        }

        return jrObject.toString();
        */
    }

    private String unwrapSOAP(String response, String recordID) {
        try {
            return XMLStepper.getFirstElementText(response, "getFromDBReturn");
        } catch (XMLStreamException e) {
            Logging.logProcess("ETSSStatusFilter.lookup",
                               "Unable to extract content from SOAP 'getFromDBReturn' from XML '" +
                               Logging.getSnippet(response) + "'",
                               Logging.LogLevel.WARN, recordID, e);
            return null;
        }
    }

    // Old style
    // http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_0040-5671_theologischeliteraturzeitung
    // New style (post 20150812)
    // http://devel06:9561/attributestore/services/json/store/genericderby.access_etss_0001-5547_scienceprintersandpublishersonlinemedicaljournals
    protected String getLookupURI(String recordID, String providerPlusID) {
        if (providerPlusID == null) {
            Logging.logProcess("ETSSStatusFilter.getETTSURI", "Unable to resolve id from dataField)",
                               Logging.LogLevel.WARN, recordID);
            return null;
        }
        return rest.replace("$ID_AND_PROVIDER", providerPlusID);
    }

    private String getProviderPlusId(String id, MARCObject.DataField dataField) {
        MARCObject.SubField subField = dataField.getFirstSubField("g");
        if (subField == null) {
            return null;
        }
        return id + "_" + normaliseProvider(subField.getContent());
    }

    // Priority: 022*a, 022*l, 022*x, 245*a, null
    private String getID(MARCObject marc) {
        final String[] SUBS = new String[]{"a", "l", "x"};
        for (String sub: SUBS) {
            MARCObject.SubField subField = marc.getFirstSubField("022", sub);
            if (subField != null) {
                return subField.getContent();
            }
        }
        MARCObject.SubField subField = marc.getFirstSubField("245", "a");
        return subField == null ? null : flatten(subField.getContent());
    }

    // Retrodigitized Journals -> retrodigitizedjournals
    private String flatten(String content) {
        StringWriter sw = new StringWriter(content.length());
        content = content.toLowerCase(new Locale("en"));
        for (int i = 0 ; i < content.length() ; i++) {
            char c = content.charAt(i);
            sw.append((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') ? Character.toString(c) : "");
        }
        return sw.toString();
    }

    // Retrodigitized Journals -> retrodigitizedjournals
    String normaliseProvider(String content) {
        return flatten(content);
    }

    /*
    <info><username>lsa@statsbiblioteket.dk</username><password>dame7896</password><group></group><comment>Dette er en kommentar</comment></info></getFromDBReturn>
     */
    protected boolean needsPassword(String response) {
        try {
            return XMLStepper.getFirstElementText(response, "password") != null;
        } catch (XMLStreamException e) {
            Logging.logProcess("ETSSStatusFilter.needsPassword",
                               "XMLException while extracting element 'password' from XML '"
                               + Logging.getSnippet(response) + "'",
                               Logging.LogLevel.WARN, "ID N/A", e);
            return false;
        }
//        return PASSWORD.matcher(response).matches();
    }


    protected String getComment(String response) {
        try {
            return XMLStepper.getFirstElementText(response, "comment");
        } catch (XMLStreamException e) {
            Logging.logProcess("ETSSStatusFilter.needsPassword",
                               "XMLException while extracting element 'comment' from XML '"
                               + Logging.getSnippet(response) + "'",
                               Logging.LogLevel.WARN, "ID N/A", e);
            return null;
        }
/*        Matcher matcher = COMMENT.matcher(response);
        if (!matcher.matches()) {
            return null;
        }
        String match = matcher.group(1).replace("\n", "").replace("\t", "").trim();
        return "".equals(match) ? null : match;*/
    }

    @Override
    public void close(boolean success) {
        super.close(success);
    }
}
