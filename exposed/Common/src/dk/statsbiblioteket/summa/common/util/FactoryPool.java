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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A pool of objects with a specific type that generates new objects when the
 * pool is empty and an object is requested. The pool is very simple and does
 * not keep track on delivered elements, so forgetting to return the element
 * only results in increased garbage collection activity.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class FactoryPool<T> {
    private static Log log = LogFactory.getLog(FactoryPool.class);

    private List<T> elements = new ArrayList<T>(10);

    /**
     * Implementations of the FactoryPool must override this method to provide
     *                 new elements.
     * @return a new element.
     */
    protected abstract T createNewElement();

    /**
     * Get an element from the pool. It is the responsibility of the caller to
     * ensure that the element is reset from previous usages before use.
     * @return an element.
     */
    public synchronized T get() {
        if (elements.size() == 0) {
            log.trace("Creating new element");
            return createNewElement();
        }
        return elements.remove(elements.size() - 1);
    }

    /**
     * Return an element to the pool.
     * @param element the used object that should be added to the pool.
     */
    public synchronized void put(T element) {
        elements.add(element);
    }
}

