/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package org.apache.lucene.search.exposed;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

/**
 * We need to use fixed Gap to get support for ordinals so we provide a factory
 * for IndexReaders and IndexWriters for convenience.
 */
// TODO: Re-implement the fixed codec code
public class ExposedIOFactory {
  public static boolean forceFixedCodec = false;

    public static DirectoryReader getReader(File location) throws IOException {
  /*    if (forceFixedCodec) {
        return DirectoryReader.open(
            FSDirectory.open(location), null, true, 1, getCodecProvider());
      }*/
      return DirectoryReader.open(FSDirectory.open(location));
    }

    // Used mainly for testing. Returns the first AtomicReader based on the
    // index at the given location
    public static AtomicReader getAtomicReader(File location) throws IOException {
      return DirectoryReader.open(FSDirectory.open(location)).getSequentialSubReaders().get(0);
    }

  public static IndexWriter getWriter(File location) throws IOException {
//    CodecProvider codecProvider = null;
//    if (forceFixedCodec) {
//      getCodecProvider();
//    }

    Directory dir = FSDirectory.open(location);
    IndexWriter writer  = new IndexWriter(
        dir, new IndexWriterConfig(Version.LUCENE_40,
            new WhitespaceAnalyzer(Version.LUCENE_40)));

    writer.getConfig().setRAMBufferSizeMB(16.0);

//    if (forceFixedCodec) {
//      writer.getConfig().setCodecProvider(codecProvider);
//    }
    return writer;
  }

/*  private static CodecProvider provider = null;
  private static CodecProvider getCodecProvider() {
    if (provider == null) {
    provider = new CodecProvider();
//    codecProvider.register(new StandardCodec());
    FixedGapCodec fixedGapCodec = new FixedGapCodec();
    provider.register(fixedGapCodec);
    provider.setDefaultFieldCodec(fixedGapCodec.name);
    CodecProvider.setDefault(provider);
    provider = provider;
    }
    return provider;
  }*/
}
