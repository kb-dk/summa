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
    public int get(int index, long[] arr, int off, int len) {
        for (int i = 0 ; i < len ; i++) {
            arr[off+i] = index+i;
        }
        return len;
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
    public long ramBytesUsed() {
        return 0;
    }

    @Override
    public Object getArray() {
        throw new UnsupportedOperationException("No array");
    }

    @Override
    public boolean hasArray() {
        return false;
    }

  @Override
  public String toString() {
    return "IdentityReader(size=" + size + ')';
  }
}
