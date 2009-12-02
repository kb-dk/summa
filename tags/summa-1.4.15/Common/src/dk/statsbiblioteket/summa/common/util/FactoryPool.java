/* $Id:$
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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.List;
import java.util.ArrayList;

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
