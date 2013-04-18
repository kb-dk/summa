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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponent;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponentImpl;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

/**
 * The Hub webservice is a thin wrapper for a {@link HubComponent}, transforming the provided arguments into a
 * SolrParams object and returning
 *
 */
// TODO: Figure out how to set the path in SolrQuery to avoid having the /select suffix
@Path("/hub/select")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubWS {
    private Log log = LogFactory.getLog(HubWS.class);

    /**
     * The name of the configuration for the hub. This can be overwritten with JNDI "java:comp/env/confLocation".
     */
    public static final String DEFAULT_CONFIGURATION = "hub_consfiguration.xml";

    private Configuration conf;
    private HubComponent hub = null;

    // TODO: Add logging
    @GET
    // TODO: Can we dynamically adjust this to return JSON if wanted?
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    @Produces(MediaType.APPLICATION_XML)
    // Taken from http://stackoverflow.com/questions/7406921/get-all-html-form-param-name-value-using-jersey
//    @Consumes("application/x-www-form-urlencoded")
//    @Consumes("text/plain")
    public String search(@Context UriInfo uriInfo) {
        NamedList<Object> namedPairs = new NamedList<Object>();

        log.debug("search: Entered with " + uriInfo.getQueryParameters().entrySet().size() + " params");
        for (Map.Entry<String, List<String>> pair: uriInfo.getQueryParameters().entrySet()) {
            for (String value: pair.getValue()) {
                log.trace("search: param " + pair.getKey() + "=" + value);
                namedPairs.add(pair.getKey(), value);
            }
        }
        SolrParams solrParams = SolrParams.toSolrParams(namedPairs);
        QueryResponse qResponse;
        try {
            qResponse = getHub().search(null, solrParams);
        } catch (final Exception e) {
            Response response = new Response() {
                @Override
                public Object getEntity() {
                    return "Exception while searching: " + e.getMessage();
                }

                @Override
                public int getStatus() {
                    return Status.INTERNAL_SERVER_ERROR.getStatusCode();
                }

                @Override
                public MultivaluedMap<String, Object> getMetadata() {
                    return null; // TODO: Implement this
                }
            };
            throw new WebApplicationException(e, response);
        }
        // TODO: Produce XML instead of JSON

        return toResultFormat(solrParams, qResponse);
    }

    private String toResultFormat(SolrParams request, QueryResponse response) {
        return HubComponentImpl.toXML(request, response);
    }

    private synchronized HubComponent getHub() {
        if (hub == null) {
            try {
                log.info("Creating HubComponent based on provided configuration...");
                //hub = HubFactory.createComponent(getConfiguration().getSubConfiguration("summa.web.hub"));
                hub = HubFactory.createComponent(getConfiguration());
                log.info("HubComponent successfully created: " + hub);
            } catch (Exception e) {
                log.error("Failed to create HubComponent", e);
            }
        }
        return hub;
    }

    /**
     * Get the a Configuration object. First trying to load the configuration from the location specified in the JNDI
     * property java:comp/env/confLocation, and if that fails, then the System Configuration will be returned.
     *
     * @return The Configuration object.
     */
    private Configuration getConfiguration() {
        final String CONFIG_LOCATION_KEY = "java:comp/env/confLocation";
        if (conf == null) {
            InitialContext context;
            try {
                context = new InitialContext();
                String paramValue;
                try {
                    paramValue = (String) context.lookup(CONFIG_LOCATION_KEY);
                } catch (Exception e) {
                    log.debug("The JNDI context property '" + CONFIG_LOCATION_KEY  + " could not be resolved. " +
                              "Defaulting to config name '" + DEFAULT_CONFIGURATION + "'");
                    paramValue = DEFAULT_CONFIGURATION;
                }
                log.debug("Trying to load configuration from '" + paramValue + "'");
                conf = Configuration.load(paramValue);
            } catch (NamingException e) {
                log.warn("Failed to lookup env-entry. Trying to load system configuration.", e);
                conf = Configuration.getSystemConfiguration(true);
            }
        }
        return conf;
    }
}
