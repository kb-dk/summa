/* $Id: ParserTask.java,v 1.14 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.14 $
 * $Date: 2007/10/05 10:20:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.ingest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The ParserTask is responsible for preparing a file for parsing.
 * The actual parsing is invoked by calling run - typically done in a multi-thread setup.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public abstract class ParserTask implements Runnable{

    protected File f;
    protected Ingest in;
    protected Target target;

    private static final Log log = LogFactory.getLog(ParserTask.class);

    /**
     * Creates a ParserTask for <code>File f</code> belonging to <code>Target target</code> submitting to <code>Ingest in</code>.
     *
     * @param f             The file to be parsed
     * @param target        The target the file belongs to
     * @param in            The ingest to submit to.
     */
    protected ParserTask(File f,  Target target, Ingest in) {
        Progress.addTodo(f.getAbsolutePath());
        this.in = in;
        this.f = f;
        this.target = target;
    }

    /**
     * Creates the handler used to filter the file through.
     * Normal behavior will be to return a {@link dk.statsbiblioteket.summa.ingest.IngestContentHandler}
     *
     * @return              the content handler
     */
    protected abstract DefaultHandler2 createContentHandler();

    /**
     * The ParserTask run method will parse the with a {@link org.xml.sax.XMLFilter}
     * using the {@link org.xml.sax.ext.DefaultHandler2} from #createContentHandler.
     * The <code>File f</code> for the ParserTask will be renamed to <code> f.getAbsolutePath() + ".completed"</code>
     * when parsing the file is completed.
     */
    public void run() {
            DefaultHandler2 handler = createContentHandler();

            log.info("Starting parsing file: " + f.getAbsolutePath() + "using content-handler: " + handler.getClass().getName());

            try{
                XMLFilter fil = new XMLFilterImpl();
                fil.setErrorHandler(new LoggingErrorHandler());
                fil.setParent(XMLReaderFactory.createXMLReader());
                fil.setContentHandler(handler);
                fil.parse(new InputSource(new FileInputStream(f.getAbsolutePath())));
            } catch (FileNotFoundException e) {
                log.error("no file found", e);
                Progress.nogood();
            } catch (IOException e) {
                log.error(e);
                Progress.nogood();
            } catch (SAXException e) {
                log.error(e);
                Progress.nogood();
            }

        String path = f.getAbsolutePath();
        f.renameTo(new File(path+".completed"));
        Progress.done(f.getAbsolutePath());
    }

    /**
     * The store method will submit a record to the {@link Ingest}.
     * Calling store with a record will guarantee that: at some time in the
     * future, attempts will be made to actually store the record.
     * @param r             the record scheduled for storage.
     */
    public void store(Record r){

        if (Record.ValidationState.fromString(
                r.getMeta(Record.META_VALIDATION_STATE)) ==
                                               Record.ValidationState.invalid) {
            log.debug(r + " not stored as the data are invalid");
            Progress.nogood();
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("target.getID_prefix:"
                    + target.getId_prefix() +
                    "".equals(target.getId_prefix()) +" "
                    +  (target.getId_prefix() == null) + " id:" +r.getId());
        }

        r.setBase(target.getBase());

        if (!target.isFullIngest() && !r.isDeleted()) {
            r.touch();
        }

        if (target.getId_prefix() != null && !target.getId_prefix().trim().equals("")){
            r.setId(target.getId_prefix() + r.getId());
        }
        
        in.flush(r);
        Progress.count();
    }

}
