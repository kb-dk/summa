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
package dk.statsbiblioteket.summa.support.summon.search.api;

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Representation of RecommendationResponse.xsd. Merging adds recommendations
 * without any duplicate elimination. Note that merging is shallow do
 * modification og the other RecommendationResponse should be avoided after
 * merge.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecommendationResponse extends ResponseImpl {
    private static Log log = LogFactory.getLog(RecommendationResponse.class);

    private ArrayList<RecommendationList> recommendationLists = new ArrayList<>();

    public RecommendationResponse() {
    }

    /**
     * Construct a new list of recommendations, add it to the internal list and
     * return it.
     *
     * @param type the type of the list.
     * @return an empty list, ready for population.
     */
    public RecommendationList newList(String type) {
        RecommendationList list = new RecommendationList(type);
        recommendationLists.add(list);
        return list;
    }

    public boolean isEmpty() {
        return recommendationLists.isEmpty();
    }

    @Override
    public String getName() {
        return "RecommendationResponse";
    }

    @Override
    public void merge(Response other) throws ClassCastException {

        if (!(other instanceof RecommendationResponse)) {
            log.trace("merge(...) expected a RecommendationResponse but got a " + other.getClass());
            return;
        }
        log.trace("Merging RecommendationResponses");
        super.merge(other);
        recommendationLists.addAll(((RecommendationResponse) other).recommendationLists);
    }

    @Override
    public String toXML() {
        StringWriter sw = new StringWriter(200);
        sw.append("<recommendationLists>\n");
        for (RecommendationList recommendationList : recommendationLists) {
            recommendationList.toXML(sw);
        }
        sw.append("</recommendationLists>\n");
        return sw.toString();
    }

    public class RecommendationList extends ArrayList<Recommendation> {
        private final String type;

        private RecommendationList(String type) {
            this.type = type;
        }

        public void addResponse(String title, String description, String link) {
            if (title == null || "".equals(title)) {
                throw new IllegalArgumentException("The title must be present");
            }
            add(new Recommendation(title, description, link));
        }

        public void toXML(StringWriter sw) {
            sw.append("  ").append(String.format("<recommendationList type=\"%s\">\n", type));
            for (Recommendation recommendation : this) {
                recommendation.toXML(sw);
            }
            sw.append("  </recommendationList>\n");
        }
    }

    public class Recommendation implements Serializable {
        private final String title;
        private final String description;
        private final String link;

        private Recommendation(String title, String description, String link) {
            this.title = title;
            this.description = description;
            this.link = link;
        }

        public void toXML(StringWriter sw) {
            sw.append("    ");
            sw.append(String.format("<recommendation title=\"%s\" description=\"%s\" link=\"%s\"/>\n",
                                    XMLUtil.encode(title), XMLUtil.encode(description), XMLUtil.encode(link)));
        }
    }
}
