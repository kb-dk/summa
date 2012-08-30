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
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Highly specific filter that takes Records with MARC21Slim XML, performs an external lookup for whether a password
 * is required to access the article described by the MARC and adjusts the XML to reflect this requirement.
 * </p><p>
 * The values added are true|false|unresolved as fields 856p. unresolved is only possible if the external password
 * service could not be queried and {@link #CONF_HALT_ON_EXTERNAL_ERROR} is false.
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
     * The REST call to perform. In the call, $ID will be replaced by the ID of the MARC record, $PROVIDER will be
     * replaced by the normalised provider from the MARC record.
     * </p><p>
     * Mandatory. Example:
     * "http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_$ID_$PROVIDER".
     */
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

    protected final int connectionTimeout;
    protected final int readTimeout;
    protected final boolean haltOnError;
    protected final String rest;
    public ETSSStatusFilter(Configuration conf) {
        super(conf);
        rest = conf.getString(CONF_REST);
        connectionTimeout = conf.getInt(CONF_ETSS_CONNECTION_TIMEOUT, DEFAULT_ETSS_CONNECTION_TIMEOUT);
        readTimeout = conf.getInt(CONF_ETSS_READ_TIMEOUT, DEFAULT_ETSS_READ_TIMEOUT);
        haltOnError = conf.getBoolean(CONF_HALT_ON_EXTERNAL_ERROR, DEFAULT_HALT_ON_EXTERNAL_ERROR);
        log.debug(String.format("Constructed filter with REST='%s''", rest));
    }

    @Override
    protected MARCObject adjust(Payload payload, MARCObject marc) {
        MARCObject.SubField idSub = marc.getFirstSubField("001", "a");
        if (idSub == null) {
            Logging.logProcess(
                "ETSSStatusFilter", "No ID-field (001a) defined for MARC record. No adjustment performed",
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
        for (int i = 0 ; i < urls.size() ; i++) {
            try {
                if (needsPassword(idSub.getContent(), providers.get(i))) {
                    urls.get(i).getSubFields().add(new MARCObject.SubField(PASSWORD_SUBFIELD, PASSWORD_CONTENT));
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
        return marc;
    }

    private HttpClient http = new DefaultHttpClient();
    private boolean needsPassword(String recordID, MARCObject.DataField dataField) throws IOException {
        String uri = getETSSURI(recordID, dataField);
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

    private String getETSSURI(String recordID, MARCObject.DataField dataField) {
        MARCObject.SubField subField = dataField.getFirstSubField("g");
        if (subField == null) {
            Logging.logProcess(
                "ETSSStatusFilter.getETTSURI", "No content provider (subfield g)", Logging.LogLevel.WARN, recordID);
            return null;
        }
        String provider = normalise(subField.getContent());
        return rest.replace("$ID", recordID).replace("$PROVIDER", provider);
    }

    private String normalise(String content) {
        return content;  // TODO: Normalise the provider (ask Hans & Mads about the standard for this)
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
