package org.apache.lucene.search.exposed.facet;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple generic pool that creates new elements if empty. The caller is
 * responsible for clearing the acquired elements.
 */
public abstract class Pool<T> {
  private List<T> elements = new ArrayList<T>(10);

  public abstract T createElement();

  public synchronized T acquire() {
    if (elements.isEmpty()) {
      return createElement();
    }
    return elements.remove(elements.size()-1);
  }

  public synchronized void release(T element) {
    elements.add(element);
  }

  public void clear() {
    elements.clear();
  }
}
