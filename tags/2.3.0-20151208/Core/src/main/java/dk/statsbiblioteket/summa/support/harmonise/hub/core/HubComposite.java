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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;

/**
 * A HubLeaf represents a single searcher that communicate both requests and responses using Solr's NamedList.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface HubComposite extends HubComponent {

    /**
     * Add the provided node to the composite as a child node.
     * @param component the sub component to add.
     */
    public void addComponent(HubComponent component);

    /**
     * @return the child nodes for the composite.
     */
    public List<HubComponent> getComponents();
}
