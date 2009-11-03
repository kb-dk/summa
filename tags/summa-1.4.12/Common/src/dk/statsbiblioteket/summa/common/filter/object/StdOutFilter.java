package dk.statsbiblioteket.summa.common.filter.object;

import javax.xml.stream.XMLStreamException;

import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.util.RecordUtil;

import dk.statsbiblioteket.util.xml.XMLUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Filter} piping record content or XML format to {@code stdout}
 */
public class StdOutFilter extends ObjectFilterImpl {

    private static final Log log = LogFactory.getLog(StdOutFilter.class);

    /**
     * Optional property defining if only the raw record content is to be
     * dumped. By default (when this property is {@code false}) records will
     * be wrapped in an XML envelope with full record metadata.
     */
    public static final String CONF_CONTENT_ONLY =
                                             "summa.filter.stdout.contentonly";

    /**
     * Default value for the {@link #CONF_CONTENT_ONLY} property
     */
    public static final boolean DEFAULT_CONTENT_ONLY = false;
    
    /**
     * Optional property defining if the written record content should be
     * XML-escaped. This affects the output regardless how
     * {@link #CONF_CONTENT_ONLY} is set
     */
    public static final String CONF_ESCAPE_CONTENT =
                                           "summa.filter.stdout.escapecontent";

    /**
     * Default value for the {@link #CONF_ESCAPE_CONTENT} property
     */
    public static final boolean DEFAULT_ESCAPE_CONTENT = false;
    
    private boolean contentOnly;
    private boolean escapeContent;
    
    public StdOutFilter(Configuration conf) {
        super(conf);
        
        contentOnly = conf.getBoolean(CONF_CONTENT_ONLY, DEFAULT_CONTENT_ONLY);
        escapeContent = conf.getBoolean(CONF_ESCAPE_CONTENT,
                                        DEFAULT_ESCAPE_CONTENT);
    }

    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() == null) {
            throw new PayloadException("Payload has no record", payload);
        }                
        
        if (contentOnly) {
            String content = payload.getRecord().getContentAsUTF8();
            System.out.println(escapeContent ?
                                       XMLUtil.encode(content) : content);
        } else {
            try {
                System.out.println(RecordUtil.toXML(payload.getRecord(),
                                                    escapeContent));
            } catch (javax.xml.stream.XMLStreamException e) {
                throw new PayloadException("Error parsing record content for "
                                           + payload.getId() + ": "
                                           + e.getMessage(), e);
            }
        }
        
        return true;
    }
}
