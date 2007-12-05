/* $Id: ClusterEngineTVHandler.java,v 1.3 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/05 10:20:24 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.facetbrowser.build;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Pattern;

import dk.statsbiblioteket.summa.facetbrowser.build.facet.FacetModel;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.connection.IndexConnection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.document.Field;

/**
 * The State and University Library of Denmark
 * Helper class for ClusterEngineImpl, used for extraction of relevant clusters
 * from TermVectors.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ClusterEngineTVHandler {
    private Set<ClusterCommon.SpecialFacet> specialFacets =
                                               ClusterCommon.getSpecialFacets();

    private String[] manyInOneFields;
    private boolean failOnMissing = true;

    public static String TVNAMESPROPERTY = "ClusterTermVectors";
    public static String FIELDNAMESPROPERTY = "ClusterFields";
    public static String TVPREPROPERTYNAME = "Cluster_vector_exceptions_";
    public static String FIELDPREPROPERTYNAME = "Cluster_field_exceptions_";
    public static String MANYINONEFAILONMISSINGKEY =
            "ClusterManyInOneFailOnMissingKey";

    public static final Pattern numberPattern =
            Pattern.compile("([0-9]{1,3}([.][0-9]+)?)|" +
                            "([0-9]{5,}([.][0-9]+)?)");

    private static final Logger log =
            Logger.getLogger(ClusterEngineTVHandler.class);

    private HashMap<String, HashSet<String>> termVectors;
    private HashMap<String, HashSet<String>> termFields;

    public ClusterEngineTVHandler() {
        log.trace("Creating ClusterEngineTVHandler");
        termVectors = makeLookupMap(TVNAMESPROPERTY, TVPREPROPERTYNAME);
        termFields =  makeLookupMap(FIELDNAMESPROPERTY, FIELDPREPROPERTYNAME);
        Boolean failOnMissing =
                ClusterCommon.getPropertyBoolean(MANYINONEFAILONMISSINGKEY);
        if (failOnMissing != null) {
            this.failOnMissing = failOnMissing;
        } else {
            log.warn(MANYINONEFAILONMISSINGKEY +
                     " not specified in properties");
        }
        String manys = ClusterCommon.getProperty(ClusterCommon.MANYINONEKEYS);
        if (manys == null) {
            if (specialFacets.contains(ClusterCommon.SpecialFacet.MANYINONE)) {
                log.warn("No keys for manyInOne, although manyInOne is set!");
            }
            manyInOneFields = new String[0];
        } else {
            manyInOneFields = manys.split(" ,|,");
            if (manyInOneFields.length == 0) {
                log.warn("No keys for manyInOne property");
            }
        }
        log.trace("Finished creation");
    }

    private HashMap<String, HashSet<String>> makeLookupMap(String lookups,
                                                           String exceptionStart) {
        Properties properties = ClusterCommon.getProperties();
        
        String termNames = properties.getProperty(lookups);
        if (termNames == null) {
            log.error("The property " + lookups +
                      " could not be fetched");
            return new HashMap<String, HashSet<String>>(1);
        }
        String[] lookupTerms = ClusterCommon.getNamesFromProperties(lookups);
        HashMap<String, HashSet<String>> lookupMap =
                new HashMap<String, HashSet<String>>(lookupTerms.length);
        for (String lookupName : lookupTerms) {
            if (!lookupName.trim().equals("")) {
                String exceptionsList =
                        properties.getProperty(exceptionStart + lookupName);
                String exceptions[];
                if (exceptionsList == null) {
                    log.debug("Could not find exceptions list for " +
                             lookupName);
                    exceptions = new String[0];
                } else {
                    exceptions = exceptionsList.split(", |,");
                }
                HashSet<String> exceptionsSet =
                        new HashSet<String>(exceptions.length);
                if (!"".equals(exceptionsList)) {
                    for (String exception: exceptions) {
                        exceptionsSet.add(exception.intern());
                    }
    //                Collections.addAll(exceptionsSet, exceptions);
                }
                lookupMap.put(lookupName.intern(), exceptionsSet);
            }
        }
        return lookupMap;
    }

    /**
     * Returns a reference to the internal termVectors. This is primarily
     * used for debugging.
     * @return the internal termVector
     */
    public HashMap<String, HashSet<String>> getTermVectors() {
        return termVectors;
    }
    /**
     * Returns a reference to the internal termFields. This is primarily
     * used for debugging.
     * @return the internal termFields
     */
    public HashMap<String, HashSet<String>> getTermFields() {
        return termFields;
    }

    public void handle(IndexConnection connection, FacetModel<String> model,
                       int docID, String storeString, float score) {
        Set<Map.Entry<String, HashSet<String>>> vectors =
                termVectors.entrySet();
        if (vectors == null) {
            log.error("No termVectors specified for the handler method");
            return;
        }
        // Normal frequency vectors
        for (Map.Entry<String, HashSet<String>> vector: vectors) {
            TermFreqVector concreteVector =
                connection.getTermFreqVector(docID, vector.getKey());
            if (concreteVector != null) {
                HashSet<String> exceptions = vector.getValue();
                for (String term: concreteVector.getTerms()) {
                    if (!exceptions.contains(term)) {
                        log.trace("Adding " + term);
                        String custom = "<query>" + vector.getKey() + ":" +
                                        ClusterCommon.safeSearchValue(term) +
                                       "</query>\n";
                        model.put(vector.getKey(), term,
                                  storeString, score);
                        model.setCustomTagString(vector.getKey(), term,
                                                 custom);
                    }
                }
            }
        }
        // Stored fields
        Set<Map.Entry<String, HashSet<String>>> fields = termFields.entrySet();
        if (fields == null) {
            log.debug("No termFields specified for the handler method");
            return;
        }
        for (Map.Entry<String, HashSet<String>> entry : fields) {
//            System.out.println("===" + entry.getKey());
            Field[] concreteFields =
                    connection.getDoc(docID).getFields(entry.getKey());
/*            Enumeration<Field> f = connection.getDoc(docID).fields();
            while (f.hasMoreElements()) {
                System.out.println("*** " + f.nextElement().name());
            }
            System.out.println(concreteFields);*/
            if (concreteFields != null) {
                HashSet<String> exceptions = entry.getValue();
                for (Field luceneField: concreteFields) {
                    System.out.println(luceneField.stringValue());
                    String term = luceneField.stringValue();
                    if (!exceptions.contains(term)) {
                        log.debug(term);
                        String custom = "<query>" + entry.getKey() + ":" +
                                        ClusterCommon.safeSearchValue(term) +
                                       "</query>\n";
                        model.put(entry.getKey(), term,
                                  storeString, score);
                        model.setCustomTagString(entry.getKey(), term,
                                                 custom);
                    }
                }
            }
        }
        // manyInOne
        if (specialFacets.contains(ClusterCommon.SpecialFacet.MANYINONE) &&
            manyInOneFields.length > 0) {
            StringBuffer sb = new StringBuffer(100);
            StringBuffer query = new StringBuffer(200);
            query.append("<query>");
            for (String fieldName: manyInOneFields) {
                TermFreqVector vector =
                        connection.getTermFreqVector(docID, fieldName);
                if (vector != null && vector.size() > 0) {
                    if (sb.length() != 0 ) {
                        sb.append(" - ");
                        query.append(" ");
                    }
                    // TODO: Can we assume that the order in term vectors is significant? (probably not)
                    sb.append(vector.getTerms()[0]);
                    query.append(fieldName).append(":");
                    query.append(ClusterCommon.
                            safeSearchValue(vector.getTerms()[0]));
                } else {
                    if (failOnMissing) {
                        sb = new StringBuffer(0); // Bit of a hack
                        break;
                    }
                }
            }
            query.append("</query>");
            if (sb.length() > 0) {
                model.put(ClusterCommon.MANYINONE, sb.toString(),
                          storeString, score);
                model.setCustomTagString(ClusterCommon.MANYINONE, sb.toString(),
                                         query.toString());

            }
        }
        // autoCluster
        if (specialFacets.contains(ClusterCommon.SpecialFacet.AUTOCLUSTER)) {
            TermFreqVector autoVector =
                connection.getTermFreqVector(docID, ClusterCommon.AUTOCLUSTER);
            if (autoVector != null) {
                for (String term: autoVector.getTerms()) {
                    String custom = "<query>" + ClusterCommon.AUTOCLUSTER +
                                    ":" +
                                   ClusterCommon.safeSearchValue(term) +
                                   "</query>\n";
                    //TODO: Loss of facet-information here. How do we avoid that?
                    String[] tokens = term.split(":", 2);
                    String showTerm = tokens.length == 2 ? tokens[1] : term;
                    model.put(ClusterCommon.AUTOCLUSTER, showTerm,
                              storeString, score);
                    model.setCustomTagString(ClusterCommon.AUTOCLUSTER,
                                             showTerm, custom);
                }
            }

        }
    }

    public void setSpecialFacets(Set<ClusterCommon.SpecialFacet>
            specialFacets) {
        this.specialFacets = specialFacets;
    }
    public Set<ClusterCommon.SpecialFacet> getSpecialFacets() {
        return specialFacets;
    }
    public List<String> getFacetNames() {
        LinkedList<String> result = new LinkedList<String>();
        result.addAll(termVectors.keySet());
        result.addAll(termFields.keySet());
        return result;
    }
}
