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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic segment logic.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class AltoAnalyzerBase<S extends AltoAnalyzerBase.Segment> implements Configurable {

    public static final String CONF_DATE_PATTERN = "altoanalyzer.filedate.pattern";
    // B-1977-10-02w-P-0003.xml -> 19771002
    public static final String DEFAULT_DATE_PATTERN = "..([0-9]{4})-([0-9]{2})-([0-9]{2}).*";

    private final String filedatePattern;

    public AltoAnalyzerBase(Configuration conf) {
        filedatePattern = conf.getString(CONF_DATE_PATTERN, DEFAULT_DATE_PATTERN);
    }

    /**
     * @param alto a fully parsed Alto.
     * @return all segments present in the given alto.
     */
    public abstract List<S> getSegments(Alto alto);

    /**
     * @return an empty Segment.
     */
    public abstract S createSegment();

    protected String dumpFull(List<Alto.TextBlock> blocks) {
        String result = "";
        for (Alto.TextBlock block: blocks) {
            if (!result.isEmpty()) {
                result += "\n";
            }
            result += block + ":" + block.getAllText();
        }
        return result;
    }

    private int idCounter = 0;
    protected S blockToSegment(Alto alto, Alto.TextBlock textBlock) {
        S segment = createSegment();
        segment.origin = alto.getOrigin();
        segment.filename = alto.getFilename();
        segment.date = getDateFromFilename(segment.filename);
        segment.id = "alto_" + segment.date + "_" + idCounter++;
        segment.hpos = textBlock.getHpos();
        segment.vpos = textBlock.getVpos();
        segment.width = textBlock.getWidth();
        segment.height = textBlock.getHeight();
        return segment;
    }

    private Pattern dateFromFile;
    protected String getDateFromFilename(String filename) {
        if (filename == null) {
            throw new NullPointerException("No filename defined");
        }
        if (dateFromFile == null) {
            dateFromFile = Pattern.compile(filedatePattern);
        }
        Matcher matcher = dateFromFile.matcher(filename);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "The filename '" + filename + "' could not be resolved to a date with pattern " + filedatePattern);
        }
        return matcher.group(1) + matcher.group(2) + matcher.group(3);
    }

    /**
     * A representation of a logical segment of the Alto.
     */
    public static abstract class Segment {
        protected String filename = null;
        protected String date = null;
        protected String id = null;
        protected int hpos = -1;
        protected int vpos = -1;
        protected int width = -1;
        protected int height = -1;

        protected String title = null;
        protected String origin = null;
        protected List<String> paragraphs = new ArrayList<String>();

        public String getYear() {
            return getDate() == null || getDate().length() < 4 ? null : getDate().substring(0, 4);
        }

        public String getAllText() {
            return paragraphs.isEmpty() ? title : title + " " + Strings.join(paragraphs, " ");
        }

        public String getShortFormatTitle() {
            return title;
        }

        /**
         * @return an URL for displaying this Segment.
         */
        public abstract String getURL();

        /**
         * Add implementation specific terms meant for indexing. The list already contains base terms such
         * id, title and paragraphs.
         * @param terms existing terms to add to.
         */
        public abstract void addIndexTerms(List<Term> terms);

        /**
         * @return the designation for these segments.
         */
        public abstract String getType();

        public static class Term {
            public final String field;
            public final String text;

            public Term(String field, String text) {
                this.field = field;
                this.text = text;
            }
        }

        /* Mutators */

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getHpos() {
            return hpos;
        }

        public void setHpos(int hpos) {
            this.hpos = hpos;
        }

        public int getVpos() {
            return vpos;
        }

        public void setVpos(int vpos) {
            this.vpos = vpos;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }

        public List<String> getParagraphs() {
            return paragraphs;
        }

        public void addParagraph(String paragraph) {
            paragraphs.add(paragraph);
        }

        public void setParagraphs(List<String> paragraphs) {
            this.paragraphs = new ArrayList<String>(paragraphs);
        }

        @Override
        public String toString() {
            return "Segment(filename='" + filename + '\'' +
                    ", date='" + date + '\'' +
                    ", id='" + id + '\'' +
                    ", hpos=" + hpos +
                    ", vpos=" + vpos +
                    ", width=" + width +
                    ", height=" + height +
                    ", title='" + title + '\'' +
                    ", origin='" + origin + '\'' +
                    ", paragraphs=" + Strings.join(paragraphs, 5) +
                    ')';
        }
    }
}
