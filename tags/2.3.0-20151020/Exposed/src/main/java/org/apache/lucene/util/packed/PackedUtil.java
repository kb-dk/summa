/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package org.apache.lucene.util.packed;

/**
 * Solr's default PackedInts.Mutable auto-chooser favors SingleBlock for speed. While this is the best selection for
 * older CPUs, Intel i7 is faster or just af fast with Packed64 for all bits except for 12(?)
 */
public class PackedUtil {

  /*
   * Attempts to select the fastest implementation.
   */
  public static PackedInts.Mutable getFastMutable(int length, int bpv) {
    if (bpv > 48) {
      return new Direct64(length);
    }
    if (bpv > 44) { // Semi-randomly chosen
      return new Packed16ThreeBlocks(length);
    }
    if (bpv > 32) {
      return new Packed64(length, bpv);
    }
    if (bpv == 32) {
      return new Direct32(length);
    }
    if (bpv > 24) {
      return new Packed64(length, bpv);
    }
    if (bpv > 21) {
      return new Packed8ThreeBlocks(length);
    }
    if (bpv > 16) {
      return new Packed64(length, bpv);
    }
    if (bpv > 14) {
      return new Direct16(length);
    }
    if (bpv == 13) {
      return new Packed64(length, bpv);
    }
    if (bpv == 12) {
      return new Packed64SingleBlock.Packed64SingleBlock12(length);
    }
    if (bpv > 8) {
      return new Packed64(length, bpv);
    }
    if (bpv > 6) {
      return new Direct8(length);
    }
    return new Packed64(length, bpv);
  }
}
