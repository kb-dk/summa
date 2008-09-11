/* $Id: Workflow.java,v 1.20 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.20 $
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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.StringTokenizer;

import dk.statsbiblioteket.summa.preingest.IngestFilter;
import dk.statsbiblioteket.summa.preingest.Extension;
import dk.statsbiblioteket.summa.ingest.postingest.MultiVolume.MARCMultivolumeMerger;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The workflow is configured using a Property file that needs to be in XML format.<br>
 * The location to the file must be given as argument when running.<br>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public class  Workflow {

    private static final Log log = LogFactory.getLog(Workflow.class);


    private static void filterFile(File f, IngestFilter filter, Extension in, Extension out) {

        if (f.isDirectory()) {
            File[] files = f.listFiles();

            for (File fil : files) {
                filterFile(fil, filter, in, out);
            }
        } else if (f.getAbsolutePath().endsWith("." + in)) {
            filter.applyFilter(f, out, "utf-8");
        }

    }


    public static void main(String args[]) throws Exception{
        try {
            log.info("running ingest");
            Properties p = new Properties();
            p.loadFromXML(new FileInputStream(args[0]));
            Progress progress = Progress.reset();

            String targetss = p.getProperty("targets");
            String[] targets;
            if (targetss != null && targetss.contains(",")){

            targets = p.getProperty("targets").split(",");
            } else {
                targets = new String[]{targetss};
            }

            Boolean mergeFlerbind = "true".equals(p.getProperty("mergeFlerbind"));
            Ingest in = Ingest.getInstance(p.getProperty("io_storage"));

            for (String target : targets) {
                String _file = p.getProperty(target);
                String _DigesterClass = p.getProperty(target + "_DigesterClass");
                String _filter_chain = p.getProperty(target + "_filter_chain");
                String _base_name = p.getProperty(target + "_base_name");
                String _encoding = p.getProperty(target + "_encoding");
                String _full_ingest = p.getProperty(target + "_full_ingest");
                String _idKey = p.getProperty(target + "_idkey");
                String _id_subfields_priority = p.getProperty(target + "_id_subfields_priority");
                String _prefix = p.getProperty(target + "_prefix");
                String _check_output = p.getProperty(target + "_check_output");
              //  Boolean mergeFlerbind = "true".equals(p.getProperty(target + "_mergeFlerbind"));



                log.info("Ingesting target:" + target + "\n" +
                        "\tHomeDir: " + _file + "\n" +
                        "\tBaseName: " + _base_name + "\n" +
                        "\tEncoding: " + _encoding + "\n" +
                        "\tidPrefix: " + _prefix + "\n" +
                        "\tDigester: " + _DigesterClass + "\n" +
                        "\tIDKey: " + _idKey + "\n" +
                        "\tSubFieldPrioity (MARC ONLY): " + _id_subfields_priority + "\n" +
                        "\tFilterChain: " + _filter_chain + "\n" +
                        "\tFull ingest: " + _full_ingest + "\n" +
                        "\tCheckDigestOutput: " + _check_output);

                if ("true".equals(p.getProperty(target+ "_run"))) {


                    //Ingest in = Ingest.getInstance(p.getProperty(target + "_io_storage"));
                    progress.setIngest(in);


                    Digester dig = (Digester) Class.forName(p.getProperty(target + "_DigesterClass")).newInstance();
                    log.info("Digester loaded:" + dig.getClass().getName());
                    String filters = p.getProperty(target + "_filter_chain");
                    log.info("Filters on target:" + filters);
                    File file = new File(p.getProperty(target));

                    if (filters != null) {
                        String[] filt = filters.split(";");
                        for (String filter : filt) {
                            String[] filterDef = filter.split(",");
                            if (filterDef.length == 2) {
                                IngestFilter fi = (IngestFilter) Class.forName(filterDef[0]).newInstance();
                                String ext = filterDef[1];
                                String[] formats = ext.split(">");
                                Extension inExt = Extension.valueOf(formats[0]);
                                Extension outExt = Extension.valueOf(formats[1]);
                                for (File f : file.listFiles()) {
                                    filterFile(f, fi, inExt, outExt);
                                }
                            }

                        }
                    }

                    Target tar = new Target();
                    tar.setBase(p.getProperty(target + "_base_name"));
                    tar.setDirectory(file.getAbsolutePath());
                    tar.setEncoding(p.getProperty(target + "_encoding"));
                    tar.setFullIngest("true".equals(p.getProperty(target + "_full_ingest")));
                    String target_id_key = p.getProperty(target + "_idkey");
                    if (target_id_key != null) {
                        log.info("has id key: " + target_id_key);
                        tar.add("target_id_key", target_id_key);
                        String subfield = p.getProperty(target + "_id_subfields_priority");

                        if (subfield != null) {
                            StringTokenizer st = new StringTokenizer(subfield, ";");
                       
                            while (st.hasMoreTokens()) {
                                String sbuf = st.nextToken();
                                String[] sp = sbuf.split(",");
                                log.info("adding subfields for id:" + sp[0].trim() + ":" + sp[1]);
                                Integer.parseInt(sp[1]);
                                tar.add("subfield_" + sp[0].trim(), sp[1]);  
                            }
                        }
                    }

                    tar.add("id_element",p.getProperty(target + "_id_element"));
                    tar.add("record_element", p.getProperty(target + "_record_element"));



                    tar.setName(target);
                    tar.setId_prefix(_prefix);
                    tar.setCheck("true".equals(_check_output));
                    tar.init();

                    if (!file.isDirectory()) {
                        log.warn("not dir:" + file.getAbsolutePath() + "skipping " + target);
                        throw new Exception("not dir:" + file.getAbsolutePath() + "skipping " + target);
                    }

                    dig.digest(tar, in);
                    log.info("done ingesting: " + target);
                    String mergeXslt = p.getProperty(target + "_flerbind_xslt");
                    if (mergeXslt != null && !"".equals(mergeXslt.trim()) && mergeFlerbind) {
                        log.info("now merging multi volume records for:" + _base_name);
                        ClassLoader l = Thread.currentThread().getContextClassLoader();
                        MARCMultivolumeMerger merger = new MARCMultivolumeMerger(in.getIO(), l.getResourceAsStream(p.getProperty(target + "_flerbind_xslt")), _base_name);
                        merger.MergeRecords(in.getIO());
                    }

                }
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);


        }
        log.info("finished ingest - exiting");
        System.exit(0);

    }




}





