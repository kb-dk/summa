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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Highlighter that given a query an an Alto input returns a list of bounding boxes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoHighlighter implements Configurable {
    private static Log log = LogFactory.getLog(AltoHighlighter.class);

    /**
     * The abstraction level for segments to highlight.
     * </p><p>
     * Optional. Default is 'textblock'. Possible values are {@link Alto.SEGMENT_LEVEL}.
     */
    public static final String CONF_SEGMENT_LEVEL = "altohighlighter.segment.level";
    public static final String DEFAULT_SEGMENT_LEVEL = Alto.SEGMENT_LEVEL.textblock.toString();

    /**
     * How measurementUnit are represented in the result.
     * </p><p>
     * Optional. Default is percent of width for {@code x} and {@code width} and percent of height for {@code y} and
     * {@code height}. Valid values are {@link Alto.MEASUREMENT_UNIT}.
     * </p>
     */
    public static final String CONF_MEASUREMENT_UNIT = "altohighlighter.measurementunit";
    public static final String DEFAULT_MEASUREMENT_UNIT = Alto.MEASUREMENT_UNIT.percent.toString();

    private final Alto.SEGMENT_LEVEL segmentLevel;
    private final Alto.MEASUREMENT_UNIT measurementUnit;

    @SuppressWarnings("UnusedParameters")
    public AltoHighlighter(Configuration conf) {
        segmentLevel = Alto.SEGMENT_LEVEL.valueOf(conf.getString(CONF_SEGMENT_LEVEL, DEFAULT_SEGMENT_LEVEL));
        measurementUnit =
                Alto.MEASUREMENT_UNIT.valueOf(conf.getString(CONF_MEASUREMENT_UNIT, DEFAULT_MEASUREMENT_UNIT));
        if (measurementUnit != Alto.MEASUREMENT_UNIT.percent) {
            throw new IllegalArgumentException(String.format(
                    "Only '%s' is currently supported for value %s",
                    Alto.MEASUREMENT_UNIT.percent, CONF_MEASUREMENT_UNIT));
        }
    }

    public List<Box> getBoxes(String altoXML, String altoOrigin, String query)
            throws FileNotFoundException, XMLStreamException, ParseException {
        List<String> qTokens = getTokens(query);
        List<Box> boxes = getBoxes(altoXML, altoOrigin);
        return intersectAndAssign(boxes, qTokens);
    }

    private List<Box> intersectAndAssign(List<Box> boxes, List<String> tokens) {
        List<Box> intersection = new ArrayList<Box>(boxes.size());
        boxLoop:
        for (Box box: boxes) {
            for (String token: tokens) {
                if (!token.endsWith("*")) {
                    for (String boxToken: box.tokens) {
                        if (boxToken.equals(token)) {
                            box.clear();
                            box.add(token);
                            intersection.add(box);
                            continue boxLoop;
                        }
                    }
                } else {
                    String prefix = token.substring(0, token.length()-1);
                    if (prefix.isEmpty()) {
                        continue;
                    }
                    for (String boxToken: box.tokens) {
                        if (boxToken.startsWith(prefix)) {
                            box.clear();
                            box.add(token);
                            intersection.add(box);
                            continue boxLoop;
                        }
                    }
                }
            }
        }
        return intersection;
    }

    protected List<Box> getBoxes(String altoXML, String altoOrigin) throws FileNotFoundException, XMLStreamException {
        Alto alto = new Alto(altoXML, altoOrigin);
        List<Box> boxes = new ArrayList<Box>();
        if (alto.getLayout().size() > 1) {
            log.warn("The highlighter got " + alto.getLayout().size() + " pages but will only process the first one");
        }
        for (Alto.Page page: alto.getLayout()) {
            if (segmentLevel == Alto.SEGMENT_LEVEL.page) {
                boxes.add(new Box(page));
                continue;
            }
            for (Alto.TextBlock block: page.getPrintSpace()) {
                if (segmentLevel == Alto.SEGMENT_LEVEL.textblock) {
                    boxes.add(new Box(block));
                    continue;
                }
                for (Alto.TextLine line: block.getLines()) {
                    if (segmentLevel == Alto.SEGMENT_LEVEL.textline) {
                        boxes.add(new Box(line));
                        continue;
                    }
                    for (Alto.TextString tstring: line.getTextStrings()) {
                        if (segmentLevel == Alto.SEGMENT_LEVEL.string) {
                            boxes.add(new Box(tstring));
                            continue;
                        }
                        throw new IllegalArgumentException(
                                "The segment level was " + segmentLevel + ", which is currently unsupported");
                    }
                }
            }
            convertMeasurements(alto, page, boxes);
            break;
        }

        // TODO: Ensure that bounding box is valid
        return boxes;
    }

    private void convertMeasurements(Alto alto, Alto.Page page, List<Box> boxes) {
        if (measurementUnit == Alto.MEASUREMENT_UNIT.percent) {
            double xfactor = 1.0 * page.getWidth();
            double yfactor = 1.0 * page.getWidth();
            for (Box box: boxes) {
                box.scale(xfactor, yfactor);
            }
            return;
        }
        throw new UnsupportedOperationException("Measurement unit " + measurementUnit + " is not supported yet");
    }

    /**
     * @param query a query entered by the user.
     * @return a list of all the tokens found in the query that are not negative. Tokens ending in '*' are prefixes.
     */
    protected List<String> getTokens(String query) throws ParseException {
        CollectTokenEvent collector = new CollectTokenEvent();
        QueryRewriter rw = new QueryRewriter(null, null, collector);
        rw.rewrite(query);
        if (log.isDebugEnabled()) {
            log.debug("getTokens(" + query + ") produced {" + Strings.join(collector.getTokens(), ", ") + "}");
        }
        return collector.getTokens();
    }

    private class CollectTokenEvent extends QueryRewriter.Event {
        private List<String> tokens = new ArrayList<String>();

        @Override
        public Query onQuery(TermQuery query) {
            tokens.add(query.getTerm().text());
            return query;
        }

        @Override
        public Query onQuery(PhraseQuery query) {
            for (Term term: query.getTerms()) {
                tokens.add(term.text());
            }
            return query;
        }

        @Override
        public Query onQuery(PrefixQuery query) {
            tokens.add(query.getPrefix().text() + "*");
            return query;
        }

        @Override
        public Query onQuery(BooleanQuery query) {
            BooleanQuery result = new BooleanQuery();
            boolean foundSome = false;
            for (BooleanClause clause : query.getClauses()) {
                if (clause.getOccur() != BooleanClause.Occur.MUST_NOT) {
                    result.add(clause);
                    foundSome = true;
                }
            }
            return foundSome ? result : null;
        }

        public List<String> getTokens() {
            return tokens;
        }
    }

    public static class Box {
        private List<String> tokens;
        private double x;
        private double y;
        private double width;
        private double height;

        public Box(Alto.PositionedElement pe) {
            this(pe.getHpos(), pe.getVpos(), pe.getWidth(), pe.getHeight(), pe.getAllTexts());
        }
        public Box(double x, double y, double width, double height) {
            this(x, y, width, height, new ArrayList<String>());

        }
        public Box(double x, double y, double width, double height, List<String> tokens) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.tokens = tokens;
        }

        public void add(String token) {
            tokens.add(token);
        }

        public void clear() {
            tokens.clear();
        }

        public void scale(double xFactor, double yFactor) {
            x *= xFactor;
            y *= yFactor;
            width *= xFactor;
            height *= yFactor;
        }
    }
}
