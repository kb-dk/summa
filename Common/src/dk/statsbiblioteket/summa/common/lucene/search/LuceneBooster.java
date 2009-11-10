/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.lucene.search;

import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.index.IndexGroup;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.*;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles query-time field-level boosts for Lucene. This means that it is
 * possible to specify boosts for e.g. title and this affect ranking.
 * This is not the same af query-time term-level boosts that only allows
 * boosts on specific terms e.g. title:mytitle.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneBooster {
    private static Log log = LogFactory.getLog(LuceneBooster.class);

    private static final Pattern boostPattern =
            Pattern.compile("^(.+)boost\\((.+)\\)$");
    private static final Pattern singleBoost =
            Pattern.compile("^(.+)\\^([0-9]+(\\.[0-9]+)?)$");

    private LuceneIndexDescriptor descriptor;
    private Map<String, Float> descriptorBoosts;

    /**
     * Constructs a lucene booster, responsible for query-time field-level
     * boosting.
     * @param descriptor the descriptor for the Lucene index.
     */
    public LuceneBooster(LuceneIndexDescriptor descriptor) {
        log.debug("Constructing LuceneBooster");
        this.descriptor = descriptor;
        extractDescriptorBoosts(descriptor);
    }

    private void extractDescriptorBoosts(LuceneIndexDescriptor descriptor) {
        descriptorBoosts = new HashMap<String, Float>(
                descriptor.getFields().size());
        for (Map.Entry<String, LuceneIndexField> entry:
                descriptor.getFields().entrySet()) {
            LuceneIndexField field = entry.getValue();
            //noinspection FloatingPointEquality
            if (field.getQueryBoost() != IndexField.DEFAULT_BOOST) {
                log.debug(String.format(
                        "Extracted query time boost %s for field '%s' from "
                        + "index descriptor",
                        field.getQueryBoost(), field.getName()));
                descriptorBoosts.put(field.getName(), field.getQueryBoost()); 
            }
        }
    }

    /**
     * Splits the given query into the standard query and any field-boost
     * parameters.
     * </p><p>
     * The format for field-boost parameters is<br />
     * normalquery "boost("(fieldname"^"boostfactor)*")"
     * </p><p>
     * Example: "heste boost(title^3.5)" =>
     *          "heste", "boost(title^3.5)"<br />
     * Example: "heste boost(title^0.5 emne^4)" =>
     *          "heste", "boost(title^0.5 emne^4)"<br />
     * Example: "heste boost" =>
     *          "heste boost", null<br />
     * Example: "galimafry foglio" =>
     *            "galimafry foglio", null.
     * @param query a query as provided by the end-user
     * @return the query and the field-boosts. The query is always something,
     *         the field-boosts are null if they are not defined in the input.
     *         The length of the returned array will always be 2.
     */
    public String[] splitQuery(String query) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("splitQuery(" + query + ") called");
        }
        Matcher matcher = boostPattern.matcher(query);
        if (matcher.matches()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return new String[]{query, null};
    }

    /**
     * Extract field-specific boosts from boost and apply them recursively to
     * an expanded Query, thereby turning field-boosts into term-boosts.
     * </p><p>
     * Note: Boosting on groups has lower precedence that boosting on specific
     *       fields: A field-level boost will always override a group-level
     *       boost on the given field.
     * </p><p>
     * The format for boost is<br />
     * {@code (field"^"boost )*(field"^"boost)?}
     * </p><p>
     * Example: "title^2.9"<br />
     * Example: "title^0.5 emne^4"
     * @param query       a standard query.
     * @param boostString boost-specific parameters.
     * @return true if at least one boost was applied.
     */
    public boolean applyBoost(Query query, String boostString) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("applyBoost(" + query + ", " + boostString + ") entered");
        if (boostString == null) {
            return false;
        }
        String[] boostTokens = boostString.split("\\s+");
        if (boostTokens.length == 0) {
            log.debug("No boosts defined in '" + boostString + "'. Returning");
            return false;
        }
        Map<String, Float> boosts =
                new HashMap<String, Float>(boostTokens.length);
        for (String boost: boostTokens) {
            Matcher matcher = singleBoost.matcher(boost);
            if (!matcher.matches()) {
                log.warn("Illegal boost '" + boost + "' in '" + query
                         + boostString + "'. Aborting boosting");
                return false;
            }
            try {
                boosts.put(matcher.group(1), Float.valueOf(matcher.group(2)));
            } catch (NumberFormatException e) {
                log.warn("Illegal float-value in '" + boost + "'. Aborting");
                return false;
            }
        }
        if (boosts.size() == 0) {
            log.debug("No boosts detected in " + boostString);
            return false;
        }
        return applyBoost(query, boosts);
    }

    /**
     * Applies the query-time boosts specified in the index descriptor to the
     * query.
     * @param query  a standard query.
     */
    public void applyDescriptorBoosts(Query query) {
        log.trace("Applying descriptor boosts");
        applyBoost(query, descriptorBoosts);
    }

    /**
     * Applies the given boosts to the query. Existing boosts are multiplied
     * with the new boost.
     * @param query  a standard query.
     * @param boosts map from field/group name/alias to boost.
     * @return true if at least one boost was applied.
     */
    private boolean applyBoost(Query query, Map<String, Float> boosts) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("applyBoost(Query, " + listBoosts(boosts) + ") entered");
        }
        expandBoosts(boosts);
        boolean applied = false;
        if (query instanceof BooleanQuery) {
            log.trace("applyBoost: BooleanQuery found");
            for (BooleanClause clause: ((BooleanQuery)query).getClauses()) {
                applied = applied | applyBoost(clause.getQuery(), boosts);
            }
        } else if (query instanceof TermQuery) {
            TermQuery termQuery = (TermQuery)query;
            if (log.isTraceEnabled()) {
                log.trace("applyBoost: termQuery '"
                          + termQuery.getTerm().field() + ":"
                          + termQuery.getTerm().text() + "' found");
            }
            if (boosts.containsKey(termQuery.getTerm().field())) {
                Float boost = boosts.get(termQuery.getTerm().field());
                if (log.isTraceEnabled()) {
                    log.trace("applyBoost: Multiplying boost " + boost
                              + " to TermQuery " + termQuery.getTerm());
                }
                termQuery.setBoost(termQuery.getBoost() * boost);
                applied = true;
            }
        } else if (query instanceof DisjunctionMaxQuery) {
            log.trace("applyBoost: DisjunctionMaxQuery found");
            Iterator iterator = ((DisjunctionMaxQuery)query).iterator();
            while (iterator.hasNext()) {
                applied = applied | applyBoost((Query)iterator.next(), boosts);
            }
        } else if (query instanceof ConstantScoreQuery) {
            log.trace("applyBoost: ConstantScoreQuery ignored");
        } else if (query instanceof ConstantScoreRangeQuery) {
            log.trace("applyBoost: ConstantScoreRangeQuery ignored");
        } else if (query instanceof RangeQuery) {
            log.trace("applyBoost: RangeQuery ignored");
        } else {
            log.warn("applyBoost: Unexpected Query '" + query.getClass()
                     + "' ignored");
        }
        log.trace("applyBoost(Query, Map) exited");
        return applied;
    }

    private String listBoosts(Map<String, Float> boosts) {
        StringWriter sw = new StringWriter(100);
        sw.append("Boosts(");
        boolean first = true;
        for (Map.Entry<String, Float> boost: boosts.entrySet()) {
            if (first) {
                first = false;
            } else {
                sw.append(", ");
            }
            sw.append(boost.getKey()).append("=");
            sw.append(Float.toString(boost.getValue()));
        }
        sw.append(")");
        return sw.toString();
    }

    /**
     * Expand groups with boosts so that all fields in the group gets the boost.
     * Also expands aliases to field names.
     * </p><p>
     * Note: Boosts on groups have lower priority that field-specific boosts.
     * @param boosts     a map with boosts for fields.
     */
    private void expandBoosts(Map<String, Float> boosts) {
        Map<String, Float> extras = new HashMap<String, Float>(boosts.size()*2);
        for (Map.Entry<String, Float> entry: boosts.entrySet()) {
            expandBoosts(entry.getKey(), entry.getValue(), extras);
        }
        for (Map.Entry<String, Float> entry: extras.entrySet()) {
            if (!boosts.containsKey(entry.getKey())) {
                boosts.put(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * Checks whether fieldOrGroup is a group or an alias. If so, extras is
     * updated with the members of the group or the real field name resolved
     * from the alias.
     * @param fieldOrGroup the name or alias of a field or a group.
     * @param boost        the boost to apply to the expanded fields.
     * @param extras       where to store the new boosts.
     */
    private void expandBoosts(String fieldOrGroup, Float boost,
                              Map<String, Float> extras) {
        log.trace("expandBoost(String, Float, Map<String, Float>) called");
        IndexField field = descriptor.getField(fieldOrGroup);
        if (field != null) {
            if (field.getName().equals(fieldOrGroup)) {
                return; // Nothing new here
            }
            // Add the real field name and return
            extras.put(field.getName(), boost);
            return;
        }
        IndexGroup group = descriptor.getGroup(fieldOrGroup);
        if (group == null) {
            return;
        }
        for (Object o: group.getFields()) {
            // TODO: Generify this
            IndexField f = (IndexField)o;
            if (log.isTraceEnabled()) {
                log.trace("expandBoost: added boost " + boost + " to "
                          + f.getName() + " in group " + fieldOrGroup);
            }
            extras.put(f.getName(), boost);
        }
    }
}
