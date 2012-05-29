package org.apache.lucene.util.packed;

/**
 * Returns the index as the value in {@link #get}. Useful for representing a 1:1
 * mapping of another structure.
 */
public class IdentityReader implements PackedInts.Reader {
  private final int size;

  public IdentityReader(int size) {
    this.size = size;
  }

  @Override
  public long get(int index) {
    return index;
  }

  @Override
  public int getBitsPerValue() {
    return 0;
  }

  @Override
  public int size() {
    return size;
  }

    @Override
    public Object getArray() {
        throw new UnsupportedOperationException("No array");
    }

    @Override
    public boolean hasArray() {
        return false;
    }
}
