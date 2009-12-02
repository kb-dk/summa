/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Converts MARC21Slim controlfields to plain datafields, optionally correcting
 * parse errors. The standard use-case is to post-process the output from
 * {@link ISO2709ToMARCXMLFilter} when the Stream contains {@code
  <record>
    <leader>00614nam0 32001931  45  </leader>
    <controlfield tag="001">000&#31;a3893228845&#31;fa&#31;od</controlfield>
    <controlfield tag="004">000&#31;ae&#31;rn</controlfield>
    <datafield tag="010" ind1="0" ind2="0">
      <subfield code="a">D33875127X</subfield>
    </datafield>
 ...}. By configuring with {@link #CONF_SUBFIELD_SEPARATOR} {@link 31}, the
 * result will be {@code
<record>
  <leader>00614nam0 32001931  45  </leader>
<datafield tag="001" ind1="0" ind2="0">
  <subfield code="a">3893228845</subfield>
  <subfield code="f">a</subfield>
  <subfield code="o">d</subfield>
</datafield>
<datafield tag="004" ind1="0" ind2="0">
  <subfield code="a">e</subfield>
  <subfield code="r">n</subfield>
</datafield>
<datafield tag="010" ind1="0" ind2="0">
  <subfield code="a">D33875127X</subfield>
</datafield>}.
 * </p><p>
 * This converter expects a Stream as input.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARC21SlimTweaker extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(MARC21SlimTweaker.class);

    /**
     * Regular expression which matches the controlfields that should be
     * converted to datafields.
     * </p><p>
     * Optional. Default is "00[0-9]" (all standard controlfields).
     */
    public static final String CONF_CONTROLFIELDS =
            "summa.marctweaker.controlfields";
    public static final String DEFAULT_CONTROLFIELDS = "00[0-9]";

    /**
     * If true, problems with the separator for subfields in controlfields will
     * be fixed.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_SUBFIELD_FIX =
            "summa.marctweaker.subfield.fix";
    public static final boolean DEFAULT_SUBFIELD_FIX = false;

    /**
     * The separator for subfields in controlfields.
     * </p><p>
     * Optional. Default is unicode 31.
     * Only relevant if {@link #CONF_SUBFIELD_FIX} is true.
     */
    public static final String CONF_SUBFIELD_SEPARATOR =
            "summa.marctweaker.subfield.separator";
    public static final String DEFAULT_SUNFIELD_SEPARATOR = "\u0031";

    private Pattern controlfields;
    private boolean subfieldFix = DEFAULT_SUBFIELD_FIX;
    private String subfieldSeparator = DEFAULT_SUNFIELD_SEPARATOR;

    public MARC21SlimTweaker(Configuration conf) {
        super(conf);
        controlfields = Pattern.compile(conf.getString(
                CONF_CONTROLFIELDS, DEFAULT_CONTROLFIELDS));
        subfieldFix = conf.getBoolean(
                CONF_SUBFIELD_FIX, subfieldFix);
        subfieldSeparator = conf.getString(
                CONF_SUBFIELD_SEPARATOR, subfieldSeparator);
        log.debug(String.format(
                "Created MARC21SlimTweaker(controlFields='%s', subfieldfix=%b, "
                + "subfieldSeparator='%s')",
                controlfields.pattern(), subfieldFix, subfieldSeparator));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            Logging.logProcess("MARC21SlimTweaker", "No Stream",
                               Logging.LogLevel.DEBUG, payload);
            return true;
        }
        payload.setStream(new MARC21SlimTweakerStream(
                payload.getStream(), controlfields, subfieldFix,
                subfieldSeparator));
        return true;
    }

    public static class MARC21SlimTweakerStream extends InputStream {
        private InputStream source;

        private Pattern controlfields;
        private boolean subfieldFix;
        private String subfieldSeparator;

        public MARC21SlimTweakerStream(
                InputStream source, Pattern controlfields,
                boolean subfieldFix, String subfieldSeparator) {
            this.source = source;
            this.controlfields = controlfields;
            this.subfieldFix = subfieldFix;
            this.subfieldSeparator = subfieldSeparator;
        }

        public int read() throws IOException {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }
}
