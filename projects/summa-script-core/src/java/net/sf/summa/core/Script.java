package net.sf.summa.core;

import java.io.InputStream;

/**
 * FIXME: Missing class docs for net.sf.summa.core.Script
 *
 * @author mke
 * @since Sep 25, 2009
 */
public class Script {

    private String id;
    private String extension;
    private InputStream sourceCode;

    public Script(String id, String extension, InputStream sourceCode) {
        this.id = id;
        this.extension = extension;
        this.sourceCode = sourceCode;
    }

    public String getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }

    public InputStream getSourceCode() {
        return sourceCode;
    }

    @Override
    public String toString() {
        return id;
    }
}
