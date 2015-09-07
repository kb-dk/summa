/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package org.apache.lucene.search.exposed.hash;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.exposed.OrdinalTermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;

/**
 * Fully experimental segment based faceting with hash-value negotiation of tags.
 * </p><p>
 * Merging is two-pass with the first pass exchanging hash-values. A bitmap keeps track of the hash-values that are
 * unique to the same term. When the index is changed, the bitmap is cleared,
 */
public class HashProvider {

    /**
     * If the TermsEnum does not support ordinal seeking directly, the {@link OrdinalTermsEnum} will use this sample
     * ratio to provide ordinal seek capabilities.
     */
    public static final int ORD_SAMPLE_RATE = 128;

    /**
     * The field with the terms.
     */
    protected final String field;

    /**
     * Underlying reader to supply the actual terms.
     */
    protected final AtomicReader reader;
    private final TermsEnum terms;

    /**
     * 32 bit hash for the terms
     */
    protected final int[] hashes;
    protected final OpenBitSet single;

    public HashProvider(AtomicReader reader, String field) throws IOException {
        this.reader = reader;
        terms = OrdinalTermsEnum.createEnum(reader, field, ORD_SAMPLE_RATE);
        this.field = field;
        hashes = getHashOrd(reader);
        single = new OpenBitSet(hashes.length);
    }

    // Generates hashes for all the ordinals
    private int[] getHashOrd(AtomicReader reader) throws IOException {
        long hashTime = -System.nanoTime();
        terms.seekExact(0);
        // Count (TODO: Optimize to single pass)
        int ordinal = 0;
        while (terms.next() != null) {
            ordinal++;
        }
        final int[] hashed = new int[ordinal];

        terms.seekExact(0);
        BytesRef term = null;
        ordinal = 0;
        while ((term = terms.next()) != null) {
            hashes[ordinal] = terms.hashCode();
        }
        hashTime += System.nanoTime();
        System.out.println("Segment " + reader + " hashed " + hashes);
        return hashed;
    }
}
