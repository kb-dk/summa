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
import dk.statsbiblioteket.summa.common.marc.MARCObject;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final String PASSWORD_SUBFIELD = "k";
    public static final String PASSWORD_CONTENT = "password required";
    public static final String PROVIDER_SPECIFIC_ID = "w"; // Record Control Number in 856

    protected final int connectionTimeout;
    protected final int readTimeout;
    protected final boolean haltOnError;
    protected final String rest;
    public ETSSStatusFilter(Configuration conf) {
        super(conf);
        feedback = false;
        rest = conf.getString(CONF_REST);
        connectionTimeout = conf.getInt(CONF_ETSS_CONNECTION_TIMEOUT, DEFAULT_ETSS_CONNECTION_TIMEOUT);
        readTimeout = conf.getInt(CONF_ETSS_READ_TIMEOUT, DEFAULT_ETSS_READ_TIMEOUT);
        haltOnError = conf.getBoolean(CONF_HALT_ON_EXTERNAL_ERROR, DEFAULT_HALT_ON_EXTERNAL_ERROR);
        log.debug(String.format("Constructed filter with REST='%s''", rest));
    }

    @Override
    protected MARCObject adjust(Payload payload, MARCObject marc) {
        long checkTime = -System.currentTimeMillis();
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
                "There was " + urls.size() + " fields with tag 856 and " + providers.size()
                + " fields with tag 980. There should be the same number. The status is left unadjusted",
                Logging.LogLevel.WARN, payload);
            return marc;
        }
        List<String> needs = new ArrayList<String>(providers.size());
        List<String> noneeds = new ArrayList<String>(providers.size());
        for (int i = 0 ; i < urls.size() ; i++) {
            try {
                String uri = getETSSURI(recordID, providers.get(i));
                List<MARCObject.SubField> subFields = urls.get(i).getSubFields();
                if (needsPassword(uri, recordID)) {
                    subFields.add(new MARCObject.SubField(PASSWORD_SUBFIELD, PASSWORD_CONTENT));
                    needs.add(uri);
                } else {
                    noneeds.add(uri);
                }
                String providerPlusID = getProviderPlusId(recordID, providers.get(i));
                if (providerPlusID != null) {
                    subFields.add(new MARCObject.SubField(PROVIDER_SPECIFIC_ID, providerPlusID));
                }
            } catch (Exception e) {
                log.warn("Unable to request password requirement for " + payload, e);
                Logging.logProcess("ETSSStatusFilter", "Unable to request password requirement",
                                   Logging.LogLevel.WARN, payload, e);
                if (haltOnError) {
                    String message =
                        "IOException when requesting password requirements for " + payload + ". "
                        + CONF_HALT_ON_EXTERNAL_ERROR + " is true so the JVM will be shut down in 5 seconds";
                    log.fatal(message, e);
                    System.err.println(message);
                    e.printStackTrace(System.err);
                    new DeferredSystemExit(1, 5000);
                    throw new RuntimeException(message, e);
                }
            }
        }
        checkTime += System.currentTimeMillis();
        if (Logging.processLog.isDebugEnabled()) {
            Logging.logProcess("ETSSStatusFilter",
                               "Checked password requirement for " + urls.size() + " providers for record "
                               + recordID + " in " + checkTime + " ms. " + needs.size()
                               + " providers explicitly needs password (" + Strings.join(needs, ", ") + "). "
                               + noneeds.size()
                               + " providers might not need passwords (" + Strings.join(noneeds, ", ") + ")",
                               Logging.LogLevel.DEBUG, payload);
        }
        return marc;
    }

    private HttpClient http = new DefaultHttpClient();
    private boolean needsPassword(String recordID, MARCObject.DataField dataField) throws IOException {
        String uri = getETSSURI(recordID, dataField);
        return needsPassword(uri, recordID);
    }

    private boolean needsPassword(String uri, String recordID) throws IOException {
        if (uri == null) {
            return false;
        }
        HttpGet method = new HttpGet(uri);
        String response;
        try {
            Long readTime = -System.currentTimeMillis();
            HttpResponse httpResponse = http.execute(method);
            if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(
                    "Expected return code " + HttpStatus.SC_OK + ", got "+ httpResponse.getStatusLine().getStatusCode()
                    + " for '" + recordID + "' with call " + uri);
            }
            readTime += System.currentTimeMillis();
            log.trace("Completed ETSS call to '" + uri + "' in " + readTime + "ms");

            HttpEntity entity = httpResponse.getEntity();
            if (entity == null) {
                Logging.logProcess("ETSSStatusFilter.needsPassword",
                                   "No response from request " + uri, Logging.LogLevel.WARN, recordID);
                return false;
            }
            response = Strings.flush(entity.getContent());
            entity.getContent().close();
        } catch (IOException e) {
            throw new IOException("Unable to connect to remote ETSS service with URI '" + uri + "'", e);
        }
        return parseResponse(response);
    }

    boolean needsPassword(String recordAndProviderID) throws IOException {
        return needsPassword(rest.replace("$ID_AND_PROVIDER", recordAndProviderID), recordAndProviderID);
    }

    // TODO: Where to store the generated provider-ID?
    // http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_0040-5671_theologischeliteraturzeitung
    private String getETSSURI(String recordID, MARCObject.DataField dataField) {
        String fullID = getProviderPlusId(recordID, dataField);
        if (fullID == null) {
            Logging.logProcess(
                "ETSSStatusFilter.getETTSURI", "Unable to resolve id from dataField)", Logging.LogLevel.WARN, recordID);
            return null;
        }
        return rest.replace("$ID_AND_PROVIDER", fullID);
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
     <soapenv:Envelope><soapenv:Body>
     <getFromDBResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     <getFromDBReturn xsi:type="soapenc:string">
     <info>
     <username>2801694</username>
     <password>04.13.13.02.12</password>
     <group>
     </group>
     <comment>
     </comment>
     </info>
     </getFromDBReturn>
     </getFromDBResponse>
     </soapenv:Body>
     </soapenv:Envelope>

     */
    // Yes it is a hack, yes we should do a proper XML parse
    private static final Pattern PASSWORD = Pattern.compile(".*&lt;password&gt;.+&lt;/password&gt;.*", Pattern.DOTALL);
    private boolean parseResponse(String response) {
        return PASSWORD.matcher(response).matches();
    }

    @Override
    public void close(boolean success) {
        super.close(success);
    }
}
