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
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Special purpose ingester of TEI XML (see http://tei-c.org/).
 * External requirement is a SummaSearcher for resolving ISBN to Summa ID.
 * </p><p>
 * Received Records are parsed, their ISBN-number extracted and a Summa ID of
 * an existing Document associated with that ISBN is resolved from the searcher.
 * The resolved Summa ID will be set as the parent to the Record.
 * </p><p>
 * If the filter is unable to resolve the ISBN, the Record will be closed with
 * success == false and discarded.
 * </p><p>
 * Connection to the searcher is handled by a {@link SearchClient} where the
 * connection setup is defined by the configuration parameter
 * {@link ConnectionConsumer#CONF_RPC_TARGET} and optionally those from.
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TEIIngester extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(TEIIngester.class);

    /**
     * The field to use for searches for ISBN.
     * The query will be "isbnfield:isbn", unless the ISBN-field is empty,
     * in which case the query will be "isbn".
     * </p><p>
     * Optional. Default is "isbn".
     */
    public static final String CONF_ISBN_FIELD = "teiingester.search.isbnfield";
    public static final String DEFAULT_ISBN_FIELD = "isbn";

    /**
     * The maximum number of hits to receive. The Record will be assigned as
     * to each of the documents from the returned hits.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_MAXHITS = "teiingester.search.maxhits";
    public static final int DEFAULT_MAXHITS = 1;

    /**
     * Non-matched records will be stored in this folder.
     * If this property is blank, non-matched Records will be discarded.
     * </p><p>
     * This location is resolved relative to the persistent folder.
     * </p><p>
     * Optional. Default is "enrich/tei/nonmatched".
     */
    public static final String CONF_NONMATCHED_FOLDER =
        "teiingester.nonmatched.folder";
    public static final String DEFAULT_NONMATCHED_FOLDER =
        "enrich/tei/nonmatched";

    /**
     * If true, ISBN extracted from TEI-XML is normalised to contain only
     * digits. If false, extracted ISBNs are used directly when searching.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_NORMALISE_ISBN =
        "teiingester.isbn.normalise";
    public static final boolean DEFAULT_NORMALISE_ISBN = true;

    private final SearchClient searcher;
    private final String isbnField;
    private final int maxHits;
    private final File nonmatchedFolder;
    private final boolean normalise;

    public TEIIngester(Configuration conf) {
        super(conf);
        searcher = new SearchClient(conf);
        isbnField = conf.getString(CONF_ISBN_FIELD, DEFAULT_ISBN_FIELD);
        maxHits = conf.getInt(CONF_MAXHITS, DEFAULT_MAXHITS);
        normalise =
            conf.getBoolean(CONF_NORMALISE_ISBN, DEFAULT_NORMALISE_ISBN);
        String non = conf.getString(
            CONF_NONMATCHED_FOLDER, DEFAULT_NONMATCHED_FOLDER);
        if (non == null || "".equals(non)) {
            log.debug("No folder for non-matched Records");
            nonmatchedFolder = null;
        } else {
            File nonF = Resolver.getPersistentFile(new File(non));
            if (nonF == null) {
                throw new ConfigurationException(
                    "Unable to resolve non-matching folder '" + non
                    + "' from property " + CONF_NONMATCHED_FOLDER);
            }
            if (!nonF.exists()) {
                if (!nonF.mkdirs()) {
                    throw new ConfigurationException(
                        "Unable to create non-matching folder '" + nonF + "'");
                }
            }
            nonmatchedFolder = nonF;
        }
        if (maxHits == 0) {
            log.warn("maxHits is 0, effectively making TEIIngester a "
                     + "discard-all filter");
        }
        log.info(String.format(
            "Created TEIIngester with searcher '%s', isbnField '%s', "
            + "maxHits %d, normalise-ISBN %b and non-matching folder '%s'",
            conf.getString(ConnectionConsumer.CONF_RPC_TARGET), isbnField,
            maxHits, normalise, nonmatchedFolder));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        log.trace("processPayload(...) called");
        if (payload.getRecord() == null) {
            throw new PayloadException("Payload with Record required", payload);
        }

        String isbn = getISBN(payload);
        if (isbn == null) {
            Logging.logProcess(
                "TEIIngester",
                "Unable to resolve ISBN, classifying Payload as non-matching",
                Logging.LogLevel.DEBUG, payload);
            nonmatching(payload);
            return false;
        }
        // We got ISBN, let's pair it up
        DocumentResponse docResponse = getHits(payload, isbn);
        if (docResponse.getHitCount() == 0) {
            nonmatching(payload);
            Logging.logProcess(
                "TEIIngester",
                "Unable to resolve find any documents matching '" + isbn + "'",
                Logging.LogLevel.DEBUG, payload);
            return false;
        }

        assignParents(payload, docResponse);
        return true;
    }

    private DocumentResponse getHits(Payload payload, String isbn)
        throws PayloadException {
        Request request = new Request();
        String query =
            "".equals(isbnField) ? isbn : isbnField + ":\"" + isbn + "\"";
        request.put(DocumentKeys.SEARCH_QUERY, query);
        request.put(DocumentKeys.SEARCH_MAX_RECORDS, maxHits);
        ResponseCollection responses;
        try {
            log.debug("Searching with query '" + query + "'");
            responses = searcher.search(request);
        } catch (IOException e) {
            nonmatching(payload);
            throw new PayloadException(
                "Exception while searching for ISBN '" + isbn + "'",
                e, payload);
        }
        DocumentResponse docResponse = null;
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                docResponse = (DocumentResponse)response;
            }
        }
        if (docResponse == null) {
            nonmatching(payload);
            throw new PayloadException(
                "Did not receive a DocumentResponse when searching for ISBN '"
                + isbn + "'", payload);
        }
        return docResponse;
    }

    /*
<TEI xmlns="http://www.tei-c.org/ns/1.0">
  <teiHeader>
    <fileDesc>
      <titleStmt>
          <title>MyTitle</title>
          <editor>Redigeret af Foo Bar</editor>
      </titleStmt>
      <publicationStmt>
          <idno type="ISBN">12-3456-789-0</idno>
      ...
     */
    private String getISBN(Payload payload) {

        return null;  // TODO: Implement this
    }

    private void nonmatching(Payload payload) {
        File filename =
            new File(nonmatchedFolder, RecordUtil.getFileName(payload));
        log.trace("Storing non-matched " + payload.getRecord()
                  + " to '" + filename + "'");
        try {
            Files.saveString(payload.getRecord().getContentAsUTF8(), filename);
        } catch (IOException e) {
            log.warn("Unable to dump the content of non-matched "
                     + payload.getRecord() + " to file '" + filename + "'", e);
        }
    }

    private void assignParents(Payload payload, DocumentResponse docResponse) {
        Record record = payload.getRecord();
        List<String> parents =
            record.getParentIds() == null ?
            new ArrayList<String>(docResponse.getRecords().size()) :
            record.getParentIds();
        for (DocumentResponse.Record hitRecord: docResponse.getRecords()) {
            if (log.isTraceEnabled()) {
                log.trace("Assigning parent ID '" + hitRecord.getId()
                          + "' to '" + record);
            }
            parents.add(hitRecord.getId());
        }
        Logging.logProcess(
            "TEIIngester",
            "Assigned '" + docResponse.getRecords().size() + "' parents. "
            + "Parents are now " + Strings.join(parents, ", "),
            Logging.LogLevel.DEBUG, payload);
    }

}
