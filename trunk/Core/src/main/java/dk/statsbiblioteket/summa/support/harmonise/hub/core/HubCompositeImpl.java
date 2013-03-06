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
package dk.statsbiblioteket.summa.support.harmonise.hub.core;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class HubCompositeImpl extends  HubComponentImpl {
    private static Log log = LogFactory.getLog(HubCompositeImpl.class);

    private List<HubComponent> nodes = new ArrayList<HubComponent>();

    public HubCompositeImpl(Configuration conf) {
        super(conf);
        log.info("Created " + toString());
    }

    @Override
    public boolean limitOK(Limit limit) {
        for (HubComponent node: nodes) {
            if (node.limitOK(limit)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MODE getMode() {
        MODE mode = null;
        outer:
        for (HubComponent node: nodes) {
            switch (node.getMode()) {
                case always: {
                    mode = MODE.always;
                    break outer;
                }
                case fallback: {
                    if (mode == MODE.onlyMatch) {
                        mode = MODE.always;
                        break outer;
                    }
                    mode = MODE.fallback;
                    break;
                }
                case onlyMatch: {
                    if (mode == MODE.fallback) {
                        mode = MODE.always;
                        break outer;
                    }
                    mode = MODE.onlyMatch;
                    break;
                }
                default: throw new UnsupportedOperationException("Unknown mode " + node.getMode());
            }
        }
        return mode;
    }

    @Override
    public List<String> getBases() {
        Set<String> bases = new HashSet<String>();
        for (HubComponent node: nodes) {
            bases.addAll(node.getBases());
        }
        return new ArrayList<String>(bases);
    }

    /**
     * @return the maximum number of components that can be added to this composite.
     */
    protected abstract int maxComponents();

    public void addComponent(HubComponent node) {
        if (nodes.size() == maxComponents()) {
            throw new IllegalStateException(
                    "Adding another node would exceed the maximum number of components (" + maxComponents()
                            + ") for this composite");
        }
        nodes.add(node);
    }

    public List<HubComponent> getComponents() {
        return nodes;
    }

    /**
     * @param limit the limit to filter with.
     * @return all sub components that satisfies limit, with respect to the mode for the components.
     */
    public List<HubComponent> getComponents(Limit limit) {
        List<HubComponent> ns = new ArrayList<HubComponent>(nodes.size());
        // First pass is without fallbacks
        for (HubComponent node: nodes) {
            if (node.getMode() != MODE.fallback && node.limitOK(limit)) {
                ns.add(node);
            }
        }
        // Second pass is fallback only
        if (ns.isEmpty()) {
            for (HubComponent node: nodes) {
                if (node.getMode() == MODE.fallback && node.limitOK(limit)) {
                    ns.add(node);
                }
            }
        }
        return ns;
    }
    @Override
    public String toString() {
        String ids = "";
        for (HubComponent node: nodes) {
            ids += (ids.isEmpty() ? "" : ", ") + node.getID();
        }
        return "HubCompositeImpl(" + super.toString() + ", sub-IDs=[" + ids + "])";
    }
}
