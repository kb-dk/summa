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
    public static final String DEFAULT_DATE_PATTERN = ".{2,}(\\d{4})-(\\d{2})-(\\d{2}).*";

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
        segment.setBoundingBox(new Alto.Box(textBlock.getHpos(), textBlock.getVpos(),
                                            textBlock.getWidth(), textBlock.getHeight()));
        segment.pageWidth = alto.getLayout().get(0).getWidth(); // TODO: Remove reliance on page 0
        segment.pageHeight = alto.getLayout().get(0).getHeight(); // TODO: Remove reliance on page 0
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
        protected Alto.Box boundingBox = null;
        protected int pageWidth = -1;
        protected int pageHeight = -1;

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

        public void setPageWidth(int pageWidth) {
            this.pageWidth = pageWidth;
        }

        public void setPageHeight(int pageHeight) {
            this.pageHeight = pageHeight;
        }

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
            return boundingBox == null ? -1 : boundingBox.getHpos();
        }
        public double getHpos(boolean fraction) {
            return fraction ? getHpos() * 1.0 / pageWidth : getHpos();
        }

        public int getVpos() {
            return boundingBox == null ? -1 : boundingBox.getVpos();
        }
        public double getVpos(boolean fraction) {
            return fraction ? getVpos() * 1.0 / pageHeight : getVpos();
        }

        public int getWidth() {
            return boundingBox == null ? -1 : boundingBox.getWidth();
        }
        public double getWidth(boolean fraction) {
            return fraction ? getWidth() * 1.0 / pageWidth : getWidth();
        }

        public int getHeight() {
            return boundingBox == null ? -1 : boundingBox.getHeight();
        }
        public double getHeight(boolean fraction) {
            return fraction ? getHeight() * 1.0 / pageHeight : getHeight();
        }

        public void setBoundingBox(Alto.Box boundingBox) {
            this.boundingBox = boundingBox;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * If the given title is more than maxLength, the substring from 0 to the last space before maxLength is
         * assigned as title. If there is no space before maxLength, the substring from 0 to maxLength is assigned.
         * @param title candidate title.
         * @param maxLength the maximum length of the title.
         */
        public void setTitle(String title, int maxLength) {
            if (title.length() <= maxLength) {
                setTitle(title);
                return;
            }
            String reduced = title.substring(0, maxLength+1);
            int lastSPace = reduced.lastIndexOf(' ');
            lastSPace = lastSPace == -1 ? maxLength+1 : lastSPace;
            setTitle(reduced.substring(0, lastSPace));
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
                    ", bounding=" + boundingBox +
                    ", title='" + title + '\'' +
                    ", origin='" + origin + '\'' +
                    ", URL='" + getURL() + "'" +
                    ", paragraphs=" + Strings.join(paragraphs, 5) +
                    ')';
        }
    }
}
