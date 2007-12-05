/* $Id: ClusterCommon.java,v 1.4 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:24 $
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
package dk.statsbiblioteket.summa.facetbrowser.util;

import java.util.Properties;
import java.util.InvalidPropertiesFormatException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.ParallelReader;
import org.apache.lucene.search.IndexSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;
//import dk.statsbiblioteket.commons.XmlOperations;

/**
 * Methods and constants that are used by several classes in the Clusters
 * package.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ClusterCommon {
    private static Properties _properties = null;
    private static Properties search_properties = null;
    private static Properties index_properties = null;
    private static final Logger log = Logger.getLogger(ClusterCommon.class);

    public static final Pattern plainTerm = Pattern.compile("([a-z]|[0-9])+");
    /*
     * Fields from the Summa Lucene index.
     */
    public static final String CREATOR =     "creator";
    public static final String TYPE =        "type";
    public static final String FREETEXT =    "freetext";
    public static final String SHORTFORMAT = "shortformat";
    public static final String RECORDID =    "recordID";
    public static final String FRBR =        "FRBR";
    public static final String MANYINONE =   "ManyInOne";
    public static final String AUTOCLUSTER =   "autoCluster";
//    public static final String AUTOCLUSTER =   "autoCluster";
    public static final String INDEXLOCATION = "directory_name";
    public static final String SEARCH_INDEXLOCATION = "indexPath";
    public static final String INDEX_INDEXLOCATION = "indexPath";
    public static final String INDEX_POSTFIX = "mergedIndex";

    public static String SPECIALFACETSPROPERTY = "ClusterSpecialFacets";
    public static final Pattern creatorPattern =
            Pattern.compile("(?s)<dc:creator>(.+?)</dc:creator>");
    public static final Pattern titlePattern =
            Pattern.compile("(?s)<dc:title>(.+?)</dc:title>");
    public static final Pattern typePattern =
            Pattern.compile("(?s)<dc:type xml:lang=\"da\">(.+?)</dc:type>");
    public static final String FACET_NAMES = "ClusterTermVectors";

    public static List<String> getTopXTerms(TermFreqVector vector, int max) {
        LinkedList<ReversePair<Integer, String>> pairs =
                new LinkedList<ReversePair<Integer, String>>();
        for (int i = 0 ; i < vector.size() ; i++) {
            pairs.add(new ReversePair<Integer, String>(vector.getTermFrequencies()[i],
                                                       vector.getTerms()[i]));
        }
        Collections.sort(pairs);

        List<String> reduced = new LinkedList<String>();
        int counter = 0;
        for (ReversePair<Integer, String> pair: pairs) {
            if (counter++ == max) {
                break;
            }
            reduced.add(pair.getValue());
        }
        return reduced;
    }

    public static String oneliner(IndexSearcher is, int docID) {
        Document doc = null;
        try {
            doc = is.doc(docID);
        } catch (IOException e) {
            // Do nothing as this is just for feedback
        }
        TermFreqVector freetext = null;
        try {
            freetext = is.getIndexReader().getTermFreqVector(docID, FREETEXT);
        } catch (IOException e) {
            // Do nothing as this is just for feedback
        }
        return oneliner(doc, docID, freetext);
    }
    public static String oneliner(Document doc, int docID,
                                  TermFreqVector vector) {
        Matcher titleMatcher = ClusterCommon.
                       titlePattern.matcher(doc.get(SHORTFORMAT));
        String title = titleMatcher.find() ?
                       titleMatcher.group(1) :
                       "[No title]";
        Matcher creatorMatcher = ClusterCommon.
                       creatorPattern.matcher(doc.get(SHORTFORMAT));
        String creator = creatorMatcher.find() ?
                         creatorMatcher.group(1) :
                         "[No creator]";
        StringWriter sw = new StringWriter(500);
        if (vector == null) {
            return title + " - " + creator + " [Error fetching freetext]";
        }
        for (String term: getTopXTerms(vector, 10)) {
            sw.append(term);
            sw.append(" ");
        }

        return title + " - " + creator + ": " + sw.toString();
    }

    public static String getMem() {
        return (Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()) / 1048576 + "/" +
               Runtime.getRuntime().totalMemory() / 1048576 + "MB used";

    }

    public static String simpleEntityEscape(String text) {
        return text.replaceAll("&",  "&amp;").
                    replaceAll("<",  "&lt;").
                    replaceAll(">",  "&gt;").
                    replaceAll("#",  "%23"). // Escaping for URL
                    replaceAll("\"", "&quot;");
    }


    public enum SpecialFacet {FRBR, TYPE, CREATOR, MANYINONE, AUTOCLUSTER}
    public static final String MANYINONEKEYS = "ClusterManyInOneKeys";
    /**
     * The name of the properties file, availabele somewhere in the ClassPath.
     */
    public static final String properties_file_name = "cluster.properties.xml";
    public static final String search_properties_file_name = "searchEngine.properties.xml";
    public static final String index_properties_file_name = "index.properties.xml";
    /**
     * @deprecated use the negative terms from properties instead.
     */
    public static Pattern stopWordPattern =
        Pattern.compile("[0-9]+[.]?-?[0-9]+|[0-9]|og|eng|bog|trykt|and|.|of|af|the|for|in|de|von");


    /**
     * Fetch the default properties on the first call, reuse the fetched
     * properties on later calls.
     * @return the Properties object from the properties_file_name
     */
    public static Properties getProperties() {
        if (_properties == null) {
            _properties = getProperties(properties_file_name);
        }
        return _properties;
    }

    /**
     * The properties for the Summa search engine. Note that this uses different
     * properties-files, depending on test and production.
     * @return a properties object for Summa Search
     */
    public static Properties getSearchProperties() {
        if (search_properties == null) {
            search_properties = getProperties(search_properties_file_name);
        }
        return search_properties;
    }
    public static Properties getIndexProperties() {
        if (index_properties == null) {
            index_properties = getProperties(index_properties_file_name);
        }
        return index_properties;
    }

    private static Properties getProperties(String name) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties result = new Properties();
        try {
            result.loadFromXML(loader.getResourceAsStream(name));
        } catch (InvalidPropertiesFormatException e1) {
            log.error("Unable to parse the properties file.", e1);
        } catch (IOException e) {
            log.error("Unable to load properties file", e);
        }
        return result;
    }

    public static Integer getPropertyInt(String propertyKey) {
        return Integer.parseInt(getProperties().getProperty(propertyKey));
    }
    public static Double getPropertyDouble(String propertyKey) {
        return Double.parseDouble(getProperties().getProperty(propertyKey));
    }
    public static Boolean getPropertyBoolean(String propertyKey) {
        return Boolean.parseBoolean(getProperties().getProperty(propertyKey));
    }
    public static String getProperty(String propertyKey) {
        return getProperties().getProperty(propertyKey);
    }

    /**
     * Write the given value to the given property name in the properties file.
     * @param name property name
     * @param value property value
     * @since 1.5
     */
    public static void writeToProp(String name, String value) {
        _properties.setProperty(name, value);
        FileOutputStream os = null;

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL prop_url = loader.getResource(properties_file_name);

        try {
            if (prop_url!=null) {
                os = new FileOutputStream(prop_url.getPath());
                _properties.storeToXML(os, "");//note: "" is a comment
            }
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
        finally {
            if(os!=null) {
                try {
                    os.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Set the Cluster-package properties. Mainly used for debugging.
     * @param properties the new properties to use
     */
    public static void setProperties(Properties properties) {
        _properties = properties;
    }
    public static String safeSearchValue(String value) {
//        String safe = colonEscape(value);
        if (plainTerm.matcher(value).matches()) {
            return value;
        }
//        return "&quot;" + safe + "&quot;";
//        return "&quot;" + XmlOperations.entityEncode(safe) + "&quot;";
        return "&quot;" + simpleEntityEscape(value) + "&quot;";
    }

    public static String colonEscape(String s) {
        return s.replace(":", "\\:");
    }

    public static String[] getFacetsFromProperties() {
        return getNamesFromProperties(FACET_NAMES);
    }

    public static String[] getNamesFromProperties(String property) {
        Properties properties = getProperties();
        String nameToken = properties.getProperty(property);
        if (nameToken == null) {
            log.error("Could not locate " + property + " in properties");
            return new String[0];
        }

        String[] names = nameToken.split(" *, *");

        // Remove stuff in parentheses
        String[] realNames = new String[names.length];
        int counter = 0;
        for (String name: names) {
            String[] nameTokens = name.split(" ");
            realNames[counter++] = nameTokens[0];
        }
        return realNames;
    }

    public static int[] getValuesFromProperties(String property,
                                                int defaultValue) {
        Properties properties = getProperties();
        String nameToken = properties.getProperty(property);
        if (nameToken == null) {
            log.error("Could not locate " + property + " in properties");
            return new int[0];
        }

        String[] tokens = nameToken.split(", |,");

        Pattern parenNumber = Pattern.compile("\\([0-9]+\\)");

        // Get stuff in parentheses
        int[] result = new int[tokens.length];
        int counter = 0;
        for (String name: tokens) {
            result[counter] = defaultValue;
            String[] nameTokens = name.split(" ");
            if (nameTokens.length == 2) {
                if (parenNumber.matcher(nameTokens[1]).matches()) {
                    result[counter] =
                            Integer.parseInt(
                                    nameTokens[1].
                                            substring(1,
                                                     nameTokens[1].length()-1));
                } else {
                    log.warn("Expected " + nameTokens + " to be a number " +
                             " in a parenthesis");
                }
            }
            counter++;
        }
        return result;
    }

    public static Set<SpecialFacet> getSpecialFacets() {
        Set<SpecialFacet> specialFacets = new HashSet<SpecialFacet>(10);
        Properties properties = getProperties();
        String specialFacetsString =
                properties.getProperty(ClusterCommon.SPECIALFACETSPROPERTY);
        if (specialFacetsString != null) {
            String[] specialFacetsNames = specialFacetsString.split(", |,");
            specialFacets.clear();
            for(String facet: specialFacetsNames) {
                if (facet.toLowerCase().
                        equals(ClusterCommon.FRBR.toLowerCase())) {
                    specialFacets.add(ClusterCommon.SpecialFacet.FRBR);
                } else if (facet.toLowerCase().
                            equals(ClusterCommon.TYPE.toLowerCase())) {
                        specialFacets.add(ClusterCommon.SpecialFacet.TYPE);
                } else if (facet.toLowerCase().
                            equals(ClusterCommon.CREATOR.toLowerCase())) {
                        specialFacets.add(ClusterCommon.SpecialFacet.CREATOR);
                } else if (facet.toLowerCase().
                            equals(ClusterCommon.MANYINONE.toLowerCase())) {
                        specialFacets.add(ClusterCommon.SpecialFacet.MANYINONE);
                } else if (facet.toLowerCase().
                            equals(ClusterCommon.AUTOCLUSTER.toLowerCase())) {
                      specialFacets.add(ClusterCommon.SpecialFacet.AUTOCLUSTER);
                } else {
                    log.warn("Unknown special facet: " + facet);
                }
            }
        }
        return specialFacets;
    }

    /**
     * Get index reader for the initial index defined in properties.
     * Remember to close the index reader, when it is no longer used.
     * @return a new index reader
     * @throws IOException if the index could not be openeds
     */
    public static IndexReader getIndexReaderInitialIndex() throws IOException {
        String index_location = getProperty(INDEXLOCATION);
        if (index_location==null) {
            log.fatal("No directory name for an index is known. " +
                      "Add the right location for " + INDEXLOCATION);
            return null;
        }

        return IndexReader.open(index_location);
    }

    public static PrintStream stringPrinter(File location, String filename)
            throws FileNotFoundException {
        File file = new File(location, filename);
        FileOutputStream stream = new FileOutputStream(file);
        BufferedOutputStream buffer = new BufferedOutputStream(stream, 10000);
        return new PrintStream(buffer);
    }
    public static ObjectOutputStream objectPrinter(File location,
                                                    String filename)
            throws IOException {
        File file = new File(location, filename);
        FileOutputStream stream = new FileOutputStream(file);
        BufferedOutputStream buffer = new BufferedOutputStream(stream, 10000);
        return new ObjectOutputStream(buffer);
    }
    public static BufferedReader stringLoader(File location, String filename)
            throws FileNotFoundException {
        FileReader reader = new FileReader(new File(location, filename));
        BufferedReader buffer = new BufferedReader(reader, 10000);
        return new BufferedReader(buffer);
    }
    public static ObjectInputStream objectLoader(File location,
                                                  String filename) throws
                                                               IOException {
        FileInputStream stream = new FileInputStream(new File(location,
                                                              filename));
        BufferedInputStream buffer = new BufferedInputStream(stream, 50000);
        return new ObjectInputStream(buffer);
    }



    /**
     * Get index reader for all parallel indices defined in properties.
     * Remember to close the index reader, when it is no longer used.
     * @return a new index reader
     * @throws IOException if one or more of the indexes could not be opened
     */
    public static IndexReader getIndexReaderParallelIndex() throws IOException {
        IndexReader reader = getIndexReaderInitialIndex();
        if (reader==null) {
            return null;
        }

        List<String> parallel_index_locations = new LinkedList<String>();
        int count = 1;
        String parallelIndexPath =
                ClusterCommon.getProperty(ClusterCommon.INDEXLOCATION+count);
        while (parallelIndexPath!=null) {
            if (new File(parallelIndexPath).exists()) {
                parallel_index_locations.add(parallelIndexPath);
            } else {
                log.warn("The parallel index " + count + " \"" +
                parallelIndexPath + "\" did not exist");
            }
            count++;
            parallelIndexPath =
               ClusterCommon.getProperty(ClusterCommon.INDEXLOCATION+count);
        }

        if (!parallel_index_locations.isEmpty()) {
            ParallelReader parallelReader = new ParallelReader();
            parallelReader.add(reader);
            for (String parallelIndexDirName : parallel_index_locations) {
                IndexReader parallelIndexReader =
                        IndexReader.open(parallelIndexDirName);
                if (reader.maxDoc()==parallelIndexReader.maxDoc()) {
                    parallelReader.add(parallelIndexReader);
                } else {
                    log.info("Parallel index '" + parallelIndexDirName +
                             "' NOT up to date?");
                }
            }
            reader = parallelReader;
        }

        return reader;
    }

    // ISO 639-2 Code	ISO 639-1 Code	English name of Language	French name of Language Danish name of Language
    // http://www.kat-format.dk/danMARC2/Danmarc2.a8.htm
    public static String[][] ISO639_2 = {
    {"aar", "aa", "Afar", "afar", "Afar"},
    {"abk", "ab", "Abkhazian", "abkhaze", "Abkhazian"},
    {"ace", "", "Achinese", "aceh", "Achinesisk"},
    {"ach", "", "Acoli", "acoli", "Acoli"},
    {"ada", "ad", "Adangme", "adangme", "Adangme"},
    {"ady", "", "Adyghe; Adygei", "adyghé", ""},
    {"afa", "", "Afro-Asiatic (Other)", "afro-asiatiques, autres langues", "Afro-asiatiske sprog (øvrige)"},
    {"afh", "", "Afrihili", "afrihili", "Afrihili"},
    {"afr", "af", "Afrikaans", "afrikaans", "Afrikaans"},
    {"ain", "", "Ainu", "aïnou", ""},
    {"aka", "ak", "Akan", "akan", "Akan"},
    {"akk", "", "Akkadian", "akkadien", "Akkadisk"},
    {"alb/sqi", "sq", "Albanian", "albanais", ""},
    {"ale", "", "Aleut", "aléoute", "Aleutiske sprog"},
    {"alg", "", "Algonquian languages", "algonquines, langues", "Algonkiske sprog"},
    {"alt", "", "Southern Altai", "altai du Sud", ""},
    {"amh", "am", "Amharic", "amharique", "Amharisk"},
    {"ang", "", "English, Old (ca.450-1100)", "anglo-saxon (ca.450-1100)", "Angelsaksisk"},
    {"anp", "", "Angika", "angika", ""},
    {"apa", "", "Apache languages", "apache", "Apache sprog"},
    {"ara", "ar", "Arabic", "arabe", "Arabisk"},
    {"arc", "", "Aramaic", "araméen", "Aramæisk"},
    {"arg", "an", "Aragonese", "aragonais", ""},
    {"arm/hye", "hy", "Armenian", "arménien", ""},
    {"arn", "", "Araucanian", "araucan", "Araukansk"},
    {"arp", "", "Arapaho", "arapaho", "Arapaho"},
    {"art", "", "Artificial (Other)", "artificielles, autres langues", "Kunstsprog (øvrige)"},
    {"arw", "", "Arawak", "arawak", "Arawak"},
    {"asm", "as", "Assamese", "assamais", "Assamesisk"},
    {"ast", "", "Asturian; Bable", "asturien; bable", ""},
    {"ath", "", "Athapascan languages", "athapascanes, langues", "Athapascan languages"},
    {"aus", "", "Australian languages", "australiennes, langues", "Australske sprog"},
    {"ava", "av", "Avaric", "avar", "Avarisk"},
    {"ave", "ae", "Avestan", "avestique", "Avestisk"},
    {"awa", "", "Awadhi", "awadhi", "Awadhi"},
    {"aym", "ay", "Aymara", "aymara", "Aymará"},
    {"aze", "az", "Azerbaijani", "azéri", "Aserbajdsjansk"},
    {"bad", "", "Banda", "banda", "Banda"},
    {"bai", "", "Bamileke languages", "bamilékés, langues", "Bamileke languages"},
    {"bak", "ba", "Bashkir", "bachkir", "Bajkirsk"},
    {"bal", "", "Baluchi", "baloutchi", "Baluchi"},
    {"bam", "bm", "Bambara", "bambara", "Bambara"},
    {"ban", "", "Balinese", "balinais", "Balinesisk"},
    {"baq/eus", "eu", "Basque", "basque", ""},
    {"bas", "", "Basa", "basa", "Basa"},
    {"bat", "", "Baltic (Other)", "baltiques, autres langues", "Baltiske sprog (øvrige)"},
    {"bej", "", "Beja", "bedja", "Beja"},
    {"bel", "be", "Belarusian", "biélorusse", "Hviderussisk"},
    {"bem", "", "Bemba", "bemba", "Bemba"},
    {"ben", "bn", "Bengali", "bengali", "Bengali"},
    {"ber", "", "Berber (Other)", "berbères, autres langues", "Berberiske sprog"},
    {"bho", "", "Bhojpuri", "bhojpuri", "Bhojpuri"},
    {"bih", "bh", "Bihari", "bihari", "Bihari"},
    {"bik", "", "Bikol", "bikol", "Bikol"},
    {"bin", "", "Bini", "bini", "Bini"},
    {"bis", "bi", "Bislama", "bichlamar", "Bislama"},
    {"bla", "", "Siksika", "blackfoot", "Siksika"},
    {"bnt", "", "Bantu (Other)", "bantoues, autres langues", "Bantu (øvrige)"},
    {"tib/bod", "bo", "Tibetan", "tibétain", ""},
    {"bos", "bs", "Bosnian", "bosniaque", "Bosnisk"},
    {"bra", "", "Braj", "braj", "Braj"},
    {"bre", "br", "Breton", "breton", "Bretonsk"},
    {"btk", "", "Batak (Indonesia)", "batak (Indonésie)", "Batak"},
    {"bua", "", "Buriat", "bouriate", "Buriat"},
    {"bug", "", "Buginese", "bugi", "Buginesisk"},
    {"bul", "bg", "Bulgarian", "bulgare", "Bulgarsk"},
    {"bur/mya", "my", "Burmese", "birman", ""},
    {"byn", "", "Blin; Bilin", "blin; bilen", ""},
    {"cad", "", "Caddo", "caddo", "Caddo"},
    {"cai", "", "Central American Indian (Other)", "indiennes d'Amérique centrale, autres langues", "Mellemamerikanske indianske sprog (øvrige)"},
    {"car", "", "Carib", "caribe", "Caribisk"},
    {"cat", "ca", "Catalan; Valencian", "catalan; valencien", "Catalansk"},
    {"cau", "", "Caucasian (Other)", "caucasiennes, autres langues", "Kaukasiske sprog (øvrige)"},
    {"ceb", "", "Cebuano", "cebuano", "Cebuano"},
    {"cel", "", "Celtic (Other)", "celtiques, autres langues", "Keltiske sprog (øvrige)"},
    {"cze/ces", "cs", "Czech", "tchèque", ""},
    {"cha", "ch", "Chamorro", "chamorro", "Chamorro"},
    {"chb", "", "Chibcha", "chibcha", "Chibcha"},
    {"che", "ce", "Chechen", "tchétchène", "Tjetjensk"},
    {"chg", "", "Chagatai", "djaghataï", "Chagatai"},
    {"chi/zho", "zh", "Chinese", "chinois", ""},
    {"chk", "", "Chuukese", "chuuk", "Chuukese"},
    {"chm", "", "Mari", "mari", "Mari"},
    {"chn", "", "Chinook jargon", "chinook, jargon", "Chinook"},
    {"cho", "", "Choctaw", "choctaw", "Choctaw"},
    {"chp", "", "Chipewyan", "chipewyan", "Chipewyan"},
    {"chr", "", "Cherokee", "cherokee", "Cherokee"},
    {"chu", "cu", "Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic", "slavon d'église; vieux slave; slavon liturgique; vieux bulgare", "Kirkeslavisk"},
    {"chv", "cv", "Chuvash", "tchouvache", "Tjuvasisk"},
    {"chy", "", "Cheyenne", "cheyenne", "Cheyenne"},
    {"cmc", "", "Chamic languages", "chames, langues", "Chamik sprog"},
    {"cop", "", "Coptic", "copte", "Koptisk"},
    {"cor", "kw", "Cornish", "cornique", "Cornisk"},
    {"cos", "co", "Corsican", "corse", "Korsikansk"},
    {"cpe", "", "Creoles and pidgins, English based (Other)", "créoles et pidgins anglais, autres", "Kreolsk og Pidgin, baseret på engelsk(øvrige)"},
    {"cpf", "", "Creoles and pidgins, French-based (Other)", "créoles et pidgins français, autres", "Kreolsk og Pidgin, baseret på fransk (øvrige)"},
    {"cpp", "", "Creoles and pidgins, Portuguese-based (Other)", "créoles et pidgins portugais, autres", "Kreolsk og Pidgin, baseret på portugisisk (øvrige)"},
    {"cre", "cr", "Cree", "cree", "Cree"},
    {"crh", "", "Crimean Tatar; Crimean Turkish", "tatar de Crimé", ""},
    {"crp", "", "Creoles and pidgins (Other)", "créoles et pidgins divers", "Kreolsk og Pidgin (øvrige)"},
    {"csb", "", "Kashubian", "kachoube", ""},
    {"cus", "", "Cushitic (Other)", "couchitiques, autres langues", "Kusjitiske sprog (øvrige)"},
    {"wel/cym", "cy", "Welsh", "gallois", ""},
    {"cze/ces", "cs", "Czech", "tchèque", ""},
    {"dak", "", "Dakota", "dakota", "Dakota"},
    {"dan", "da", "Danish", "danois", "Dansk"},
    {"dar", "", "Dargwa", "dargwa", ""},
    {"day", "", "Dayak", "dayak", "Dayak"},
    {"del", "", "Delaware", "delaware", "Delaware"},
    {"den", "", "Slave (Athapascan)", "esclave (athapascan)", "Slave (Athapascan)"},
    {"ger/deu", "de", "German", "allemand", ""},
    {"dgr", "", "Dogrib", "dogrib", "Dogrib"},
    {"din", "", "Dinka", "dinka", "Dinka"},
    {"div", "dv", "Divehi; Dhivehi; Maldivian", "maldivien", "Divehi"},
    {"doi", "", "Dogri", "dogri", "Dogri"},
    {"dra", "", "Dravidian (Other)", "dravidiennes, autres langues", "Dravidiske sprog (øvrige)"},
    {"dsb", "", "Lower Sorbian", "bas-sorabe", ""},
    {"dua", "", "Duala", "douala", "Duala"},
    {"dum", "", "Dutch, Middle (ca.1050-1350)", "néerlandais moyen (ca. 1050-1350)", "Hollandsk (ca. 1050-1350)"},
    {"dut/nld", "nl", "Dutch; Flemish", "néerlandais; flamand", ""},
    {"dyu", "", "Dyula", "dioula", "Dyula"},
    {"dzo", "dz", "Dzongkha", "dzongkha", "Dzongkha"},
    {"efi", "", "Efik", "efik", "Efik"},
    {"egy", "", "Egyptian (Ancient)", "égyptien", "Egyptisk"},
    {"eka", "", "Ekajuk", "ekajuk", "Ekajuk"},
    {"gre/ell", "el", "Greek, Modern (1453-)", "grec moderne (après 1453)", ""},
    {"elx", "", "Elamite", "élamite", "Elamitisk"},
    {"eng", "en", "English", "anglais", "Engelsk"},
    {"enm", "", "English, Middle (1100-1500)", "anglais moyen (1100-1500)", "Engelsk, Middel- (ca. 1100-1500)"},
    {"epo", "eo", "Esperanto", "espéranto", "Eperanto"},
    {"est", "et", "Estonian", "estonien", "Estisk"},
    {"baq/eus", "eu", "Basque", "basque", ""},
    {"ewe", "ee", "Ewe", "éwé", "Ewe"},
    {"ewo", "", "Ewondo", "éwondo", "Ewondo"},
    {"fan", "", "Fang", "fang", "Fang"},
    {"fao", "fo", "Faroese", "féroïen", "Færøsk"},
    {"per/fas", "fa", "Persian", "persan", ""},
    {"fat", "", "Fanti", "fanti", "Fanti"},
    {"fij", "fj", "Fijian", "fidjien", "Fijian"},
    {"fil", "", "Filipino; Pilipino", "filipino; pilipino", ""},
    {"fin", "fi", "Finnish", "finnois", "Finsk"},
    {"fiu", "", "Finno-Ugrian (Other)", "finno-ougriennes, autres langues", "Finsk-ugriske sprog (øvrige)"},
    {"fon", "", "Fon", "fon", "Fon"},
    {"fre/fra", "fr", "French", "français", ""},
    {"fre/fra", "fr", "French", "français", ""},
    {"frm", "", "French, Middle (ca.1400-1600)", "français moyen (1400-1600)", "Fransk, Middel- (ca. 1400-1600)"},
    {"fro", "", "French, Old (842-ca.1400)", "français ancien (842-ca.1400)", "Fransk, Old- (ca. 842-1400)"},
    {"frr", "", "Northern Frisian", "frison septentrional", ""},
    {"frs", "", "Eastern Frisian", "frison oriental", ""},
    {"fry", "fy", "Western Frisian", "frison occidental", "Frisisk"},
    {"ful", "ff", "Fulah", "peul", "Fulah"},
    {"fur", "", "Friulian", "frioulan", "Friulian"},
    {"gaa", "", "Ga", "ga", "Ga"},
    {"gay", "", "Gayo", "gayo", "Gayo"},
    {"gba", "", "Gbaya", "gbaya", "Gbaya"},
    {"gem", "", "Germanic (Other)", "germaniques, autres langues", "Germanske sprog (øvrige)"},
    {"geo/kat", "ka", "Georgian", "géorgien", ""},
    {"ger/deu", "de", "German", "allemand", ""},
    {"gez", "", "Geez", "guèze", "Geez"},
    {"gil", "", "Gilbertese", "kiribati", "Gilbertesisk"},
    {"gla", "gd", "Gaelic; Scottish Gaelic", "gaélique; gaélique écossais", "Gælisk (skotsk)"},
    {"gle", "ga", "Irish", "irlandais", "Irsk"},
    {"glg", "gl", "Galician", "galicien", "Gallegan"},
    {"glv", "gv", "Manx", "manx; mannois", "Manx"},
    {"gmh", "", "German, Middle High (ca.1050-1500)", "allemand, moyen haut (ca. 1050-1500)", "Tysk, Middelhøj- (ca. 1050-1500)"},
    {"goh", "", "German, Old High (ca.750-1050)", "allemand, vieux haut (ca. 750-1050)", "Tysk, Oldhøj- (ca. 750-1050)"},
    {"gon", "", "Gondi", "gond", "Gondi"},
    {"gor", "", "Gorontalo", "gorontalo", "Gorontalo"},
    {"got", "", "Gothic", "gothique", "Gotisk"},
    {"grb", "", "Grebo", "grebo", "Grebo"},
    {"grc", "", "Greek, Ancient (to 1453)", "grec ancien (jusqu'à 1453)", "Græsk, Old- (til 1453)"},
    {"gre/ell", "el", "Greek, Modern (1453-)", "grec moderne (après 1453)", ""},
    {"grn", "gn", "Guarani", "guarani", "Guarani"},
    {"gsw", "", "Alemani; Swiss German", "alémanique", ""},
    {"guj", "gu", "Gujarati", "goudjrati", "Gujarati"},
    {"gwi", "", "Gwich´in", "gwich´in", "Gwich'in"},
    {"hai", "", "Haida", "haida", "Haida"},
    {"hat", "ht", "Haitian; Haitian Creole", "haïtien; créole haïtien", ""},
    {"hau", "ha", "Hausa", "haoussa", "Hausa"},
    {"haw", "", "Hawaiian", "hawaïen", "Hawaiiansk"},
    {"heb", "he", "Hebrew", "hébreu", "Hebraisk"},
    {"her", "hz", "Herero", "herero", "Herero"},
    {"hil", "", "Hiligaynon", "hiligaynon", "Hiligaynon"},
    {"him", "", "Himachali", "himachali", "Himachali"},
    {"hin", "hi", "Hindi", "hindi", "Hindi"},
    {"hit", "", "Hittite", "hittite", "Hittitisk"},
    {"hmn", "", "Hmong", "hmong", "Hmong"},
    {"hmo", "ho", "Hiri Motu", "hiri motu", "Hiri Motu"},
    {"scr/hrv", "hr", "Croatian", "croate", ""},
    {"hsb", "", "Upper Sorbian", "haut-sorabe", ""},
    {"hun", "hu", "Hungarian", "hongrois", "Ungarsk"},
    {"hup", "", "Hupa", "hupa", "Hupa"},
    {"arm/hye", "hy", "Armenian", "arménien", ""},
    {"iba", "", "Iban", "iban", "Iban"},
    {"ibo", "ig", "Igbo", "igbo", "Igbo"},
    {"ice/isl", "is", "Icelandic", "islandais", ""},
    {"ido", "io", "Ido", "ido", ""},
    {"iii", "ii", "Sichuan Yi", "yi de Sichuan", ""},
    {"ijo", "", "Ijo", "ijo", "Ijo"},
    {"iku", "iu", "Inuktitut", "inuktitut", "Inuktitut"},
    {"ile", "ie", "Interlingue", "interlingue", "Interlingue"},
    {"ilo", "", "Iloko", "ilocano", "Iloko"},
    {"ina", "ia", "Interlingua (International Auxiliary Language Association)", "interlingua (langue auxiliaire internationale)", "Interlingua"},
    {"inc", "", "Indic (Other)", "indo-aryennes, autres langues", "Indiske sprog (øvrige)"},
    {"ind", "id", "Indonesian", "indonésien", "Indonesisk"},
    {"ine", "", "Indo-European (Other)", "indo-européennes, autres langues", "Indoeuropæiske sprog (øvrige)"},
    {"inh", "", "Ingush", "ingouche", ""},
    {"ipk", "ik", "Inupiaq", "inupiaq", "Inupiaq"},
    {"ira", "", "Iranian (Other)", "iraniennes, autres langues", "Iranske sprog (øvrige)"},
    {"iro", "", "Iroquoian languages", "iroquoises, langues (famille)", "Irokesiske sprog"},
    {"ice/isl", "is", "Icelandic", "islandais", ""},
    {"ita", "it", "Italian", "italien", "Italiensk"},
    {"jav", "jv", "Javanese", "javanais", "Javanesisk"},
    {"jbo", "", "Lojban", "lojban", ""},
    {"jpn", "ja", "Japanese", "japonais", "Japansk"},
    {"jpr", "", "Judeo-Persian", "judéo-persan", "Jødisk-persisk"},
    {"jrb", "", "Judeo-Arabic", "judéo-arabe", "Jødisk-arabisk"},
    {"kaa", "", "Kara-Kalpak", "karakalpak", "Karakalpakisk"},
    {"kab", "", "Kabyle", "kabyle", "Kabyle"},
    {"kac", "", "Kachin", "kachin", "Kachin"},
    {"kal", "kl", "Kalaallisut; Greenlandic", "groenlandais", "Kalaallisut"},
    {"kam", "", "Kamba", "kamba", "Kamba"},
    {"kan", "kn", "Kannada", "kannada", "Kannaresisk"},
    {"kar", "", "Karen", "karen", "Karen"},
    {"kas", "ks", "Kashmiri", "kashmiri", "Kashmiri"},
    {"geo/kat", "ka", "Georgian", "géorgien", ""},
    {"kau", "kr", "Kanuri", "kanouri", "Kanuri"},
    {"kaw", "", "Kawi", "kawi", "Kawi"},
    {"kaz", "kk", "Kazakh", "kazakh", "Kasakhisk"},
    {"kbd", "", "Kabardian", "kabardien", ""},
    {"kha", "", "Khasi", "khasi", "Khasi"},
    {"khi", "", "Khoisan (Other)", "khoisan, autres langues", "Khoisan (øvrige)"},
    {"khm", "km", "Khmer", "khmer", "Khmer"},
    {"kho", "", "Khotanese", "khotanais", "Khotanesisk"},
    {"kik", "ki", "Kikuyu; Gikuyu", "kikuyu", "Kikuyu"},
    {"kin", "rw", "Kinyarwanda", "rwanda", "Kinyarwanda"},
    {"kir", "ky", "Kirghiz", "kirghize", "Kirgisisk"},
    {"kmb", "", "Kimbundu", "kimbundu", "Kimbundu"},
    {"kok", "", "Konkani", "konkani", "Konkani"},
    {"kom", "kv", "Komi", "kom", "Komi"},
    {"kon", "kg", "Kongo", "kongo", "Kongo"},
    {"kor", "ko", "Korean", "coréen", "Koreansk"},
    {"kos", "", "Kosraean", "kosrae", "Kosraean"},
    {"kpe", "", "Kpelle", "kpellé", "Kpelle"},
    {"krc", "", "Karachay-Balkar", "karatchai balkar", ""},
    {"krl", "", "Karelian", "carélien", ""},
    {"kro", "", "Kru", "krou", "Kru"},
    {"kru", "", "Kurukh", "kurukh", "Kurukh"},
    {"kua", "kj", "Kuanyama; Kwanyama", "kuanyama; kwanyama", "Kuanyama"},
    {"kum", "", "Kumyk", "koumyk", "Kumyk"},
    {"kur", "ku", "Kurdish", "kurde", "Kurdisk"},
    {"kut", "", "Kutenai", "kutenai", "Kutenai"},
    {"lad", "", "Ladino", "judéo-espagnol", "Ladino"},
    {"lah", "", "Lahnda", "lahnda", "Lahnda"},
    {"lam", "", "Lamba", "lamba", "Lamba"},
    {"lao", "lo", "Lao", "lao", "Laotisk"},
    {"lat", "la", "Latin", "latin", "Latin"},
    {"lav", "lv", "Latvian", "letton", "Lettisk"},
    {"lez", "", "Lezghian", "lezghien", "Lezghian"},
    {"lim", "li", "Limburgan; Limburger; Limburgish", "limbourgeois", ""},
    {"lin", "ln", "Lingala", "lingala", "Lingala"},
    {"lit", "lt", "Lithuanian", "lituanien", "Litauisk"},
    {"lol", "", "Mongo", "mongo", "Mongo"},
    {"loz", "", "Lozi", "lozi", "Lozi"},
    {"ltz", "lb", "Luxembourgish; Letzeburgesch", "luxembourgeois", "Letzeburgesch"},
    {"lua", "", "Luba-Lulua", "luba-lulua", "Luba-Lulua"},
    {"lub", "lu", "Luba-Katanga", "luba-katanga", "Luba-Katanga"},
    {"lug", "lg", "Ganda", "ganda", "Luganda"},
    {"lui", "", "Luiseno", "luiseno", "Luiseño"},
    {"lun", "", "Lunda", "lunda", "Lunda"},
    {"luo", "", "Luo (Kenya and Tanzania)", "luo (Kenya et Tanzanie)", "Luo (Kenya og Tanzania)"},
    {"lus", "", "Lushai", "lushai", "Lushai"},
    {"mac/mkd", "mk", "Macedonian", "macédonien", ""},
    {"mad", "", "Madurese", "madourais", "Madurese"},
    {"mag", "", "Magahi", "magahi", "Magahi"},
    {"mah", "mh", "Marshallese", "marshall", "Marshall"},
    {"mai", "", "Maithili", "maithili", "Maithili"},
    {"mak", "", "Makasar", "makassar", "Makasar"},
    {"mal", "ml", "Malayalam", "malayalam", "Malayalam"},
    {"man", "", "Mandingo", "mandingue", "Mandingo"},
    {"mao/mri", "mi", "Maori", "maori", ""},
    {"map", "", "Austronesian (Other)", "malayo-polynésiennes, autres langues", "Malajo-polynesiske sprog (øvrige)"},
    {"mar", "mr", "Marathi", "marathe", "Marathi"},
    {"mas", "", "Masai", "massaï", "Masai"},
    {"may/msa", "ms", "Malay", "malais", ""},
    {"mdf", "", "Moksha", "moksa", ""},
    {"mdr", "", "Mandar", "mandar", "Mandar"},
    {"men", "", "Mende", "mendé", "Mende"},
    {"mga", "", "Irish, Middle (900-1200)", "irlandais moyen (900-1200)", "Irsk, Middel- (900-1200)"},
    {"mic", "", "Mi'kmaq; Micmac", "mi'kmaq; micmac", "Micmac"},
    {"min", "", "Minangkabau", "minangkabau", "Minangkabau"},
    {"mis", "", "Miscellaneous languages", "diverses, langues", "Miscellaneous (øvrige)"},
    {"mac/mkd", "mk", "Macedonian", "macédonien", ""},
    {"mkh", "", "Mon-Khmer (Other)", "môn-khmer, autres langues", "Mon-khmer (øvrige)"},
    {"mlg", "mg", "Malagasy", "malgache", "Malagasy"},
    {"mlt", "mt", "Maltese", "maltais", "Maltesisk"},
    {"mnc", "", "Manchu", "mandchou", ""},
    {"mni", "", "Manipuri", "manipuri", "Manipuri"},
    {"mno", "", "Manobo languages", "manobo, langues", "Manobo languages"},
    {"moh", "", "Mohawk", "mohawk", "Mohawk"},
    {"mol", "mo", "Moldavian", "moldave", "Moldovisk"},
    {"mon", "mn", "Mongolian", "mongol", "Mongolsk"},
    {"mos", "", "Mossi", "moré", "Mossi"},
    {"mao/mri", "mi", "Maori", "maori", ""},
    {"may/msa", "ms", "Malay", "malais", ""},
    {"mul", "", "Multiple languages", "multilingue", "flere sprog"},
    {"mun", "", "Munda languages", "mounda, langues", "Munda (øvrige)"},
    {"mus", "", "Creek", "muskogee", "Muskogee"},
    {"mwl", "", "Mirandese", "mirandais", ""},
    {"mwr", "", "Marwari", "marvari", "Marwari"},
    {"bur/mya", "my", "Burmese", "birman", ""},
    {"myn", "", "Mayan languages", "maya, langues", "Maya sprog"},
    {"myv", "", "Erzya", "erza", ""},
    {"nah", "", "Nahuatl", "nahuatl", "Nahuatl"},
    {"nai", "", "North American Indian", "indiennes d'Amérique du Nord, autres langues", "Nordamerikanske indianske sprog (øvrige)"},
    {"nap", "", "Neapolitan", "napolitain", ""},
    {"nau", "na", "Nauru", "nauruan", "Nauru"},
    {"nav", "nv", "Navajo; Navaho", "navaho", "Navaho"},
    {"nbl", "nr", "Ndebele, South; South Ndebele", "ndébélé du Sud", "Ndebele, Syd"},
    {"nde", "nd", "Ndebele, North; North Ndebele", "ndébélé du Nord", "Ndebele, Nord"},
    {"ndo", "ng", "Ndonga", "ndonga", "Ndonga"},
    {"nds", "", "Low German; Low Saxon; German, Low; Saxon, Low", "bas allemand; bas saxon; allemand, bas; saxon, bas", ""},
    {"nep", "ne", "Nepali", "népalais", "Nepalesisk"},
    {"new", "", "Nepal Bhasa; Newari", "nepal bhasa; newari", "Newari"},
    {"nia", "", "Nias", "nias", "Nias"},
    {"nic", "", "Niger-Kordofanian (Other)", "nigéro-congolaises, autres langues", "Niger-Congo sprog (øvrige)"},
    {"niu", "", "Niuean", "niué", "Niuean"},
    {"dut/nld", "nl", "Dutch; Flemish", "néerlandais; flamand", ""},
    {"nno", "nn", "Norwegian Nynorsk; Nynorsk, Norwegian", "norvégien nynorsk; nynorsk, norvégien", ""},
    {"nob", "nb", "Bokmål, Norwegian; Norwegian Bokmål", "norvégien bokmål", ""},
    {"nog", "", "Nogai", "nogaï; nogay", ""},
    {"non", "", "Norse, Old", "norrois, vieux", "Islandsk, Old-"},
    {"nor", "no", "Norwegian", "norvégien", "Norsk"},
    {"nqo", "", "N'Ko", "n'ko", ""},
    {"nso", "", "Pedi; Sepedi; Northern Sotho", "pedi; sepedi; sotho du Nord", "Sotho, Nord"},
    {"nub", "", "Nubian languages", "nubiennes, langues", "Nubiske sprog"},
    {"nwc", "", "Classical Newari; Old Newari; Classical Nepal Bhasa", "newari classique", ""},
    {"nya", "ny", "Chichewa; Chewa; Nyanja", "chichewa; chewa; nyanja", "Nyanja"},
    {"nym", "", "Nyamwezi", "nyamwezi", "Nyamwezi"},
    {"nyn", "", "Nyankole", "nyankolé", "Nyankole"},
    {"nyo", "", "Nyoro", "nyoro", "Nyoro sprog"},
    {"nzi", "", "Nzima", "nzema", "Nzima"},
    {"oci", "oc", "Occitan (post 1500); Provençal", "occitan (après 1500); provençal", "Occitansk (efter 1500)"},
    {"oji", "oj", "Ojibwa", "ojibwa", "Ojibwa"},
    {"ori", "or", "Oriya", "oriya", "Orija"},
    {"orm", "om", "Oromo", "galla", "Oromo"},
    {"osa", "", "Osage", "osage", "Osage"},
    {"oss", "os", "Ossetian; Ossetic", "ossète", "Ossetisk"},
    {"ota", "", "Turkish, Ottoman (1500-1928)", "turc ottoman (1500-1928)", "Osmannisk (1500-1928)"},
    {"oto", "", "Otomian languages", "otomangue, langues", "Otomi sprog"},
    {"paa", "", "Papuan (Other)", "papoues, autres langues", "Papua-australske sprog (øvrige)"},
    {"pag", "", "Pangasinan", "pangasinan", "Pangasinan"},
    {"pal", "", "Pahlavi", "pahlavi", "Pahlavi"},
    {"pam", "", "Pampanga", "pampangan", "Pampanga"},
    {"pan", "pa", "Panjabi; Punjabi", "pendjabi", "Panjabi"},
    {"pap", "", "Papiamento", "papiamento", "Papiamento"},
    {"pau", "", "Palauan", "palau", "Paluansk"},
    {"peo", "", "Persian, Old (ca.600-400 B.C.)", "perse, vieux (ca. 600-400 av. J.-C.)", "Persisk, Old- (ca. 600-400 f. Kr.)"},
    {"per/fas", "fa", "Persian", "persan", ""},
    {"phi", "", "Philippine (Other)", "philippines, autres langues", "Filippinske sprog (øvrige)"},
    {"phn", "", "Phoenician", "phénicien", "Fønikisk"},
    {"pli", "pi", "Pali", "pali", "Pali"},
    {"pol", "pl", "Polish", "polonais", "Polsk"},
    {"pon", "", "Pohnpeian", "pohnpei", "Ponape"},
    {"por", "pt", "Portuguese", "portugais", "Portugisisk"},
    {"pra", "", "Prakrit languages", "prâkrit", "Prakit languages"},
    {"pro", "", "Provençal, Old (to 1500)", "provençal ancien (jusqu'à 1500)", "Provencalsk (før 1500)"},
    {"pus", "ps", "Pushto", "pachto", "Pashto"},
    {"qaa-qtz", "", "Reserved for local use", "réservée à l'usage local", ""},
    {"que", "qu", "Quechua", "quechua", "Kechua"},
    {"raj", "", "Rajasthani", "rajasthani", "Rajasthani"},
    {"rap", "", "Rapanui", "rapanui", "Rapanui"},
    {"rar", "", "Rarotongan", "rarotonga", "Rarotongan"},
    {"roa", "", "Romance (Other)", "romanes, autres langues", "Romanske sprog (øvrige)"},
    {"roh", "rm", "Raeto-Romance", "rhéto-roman", "Rætoromansk"},
    {"rom", "", "Romany", "tsigane", "Romani"},
    {"rum/ron", "ro", "Romanian", "roumain", ""},
    {"rum/ron", "ro", "Romanian", "roumain", ""},
    {"run", "rn", "Rundi", "rundi", "Rundi"},
    {"rup", "", "Aromanian; Arumanian; Macedo-Romanian", "aroumain; macédo-roumain", ""},
    {"rus", "ru", "Russian", "russe", "Russisk"},
    {"sad", "", "Sandawe", "sandawe", "Sandawe"},
    {"sag", "sg", "Sango", "sango", "Sango"},
    {"sah", "", "Yakut", "iakoute", "Yakut"},
    {"sai", "", "South American Indian (Other)", "indiennes d'Amérique du Sud, autres langues", "Sydamerikanske indianske sprog (øvrige)"},
    {"sal", "", "Salishan languages", "salish, langues", "Salishan languages"},
    {"sam", "", "Samaritan Aramaic", "samaritain", "Samaritansk"},
    {"san", "sa", "Sanskrit", "sanskrit", "Sanskrit"},
    {"sas", "", "Sasak", "sasak", "Sasak"},
    {"sat", "", "Santali", "santal", "Santali"},
    {"scc/srp", "sr", "Serbian", "serbe", ""},
    {"scn", "", "Sicilian", "sicilien", ""},
    {"sco", "", "Scots", "écossais", "Skotsk"},
    {"scr/hrv", "hr", "Croatian", "croate", ""},
    {"sel", "", "Selkup", "selkoupe", "Selkupisk"},
    {"sem", "", "Semitic (Other)", "sémitiques, autres langues", "Semitiske sprog (øvrige)"},
    {"sga", "", "Irish, Old (to 900)", "irlandais ancien (jusqu'à 900)", "Irsk, Old- (indtil 900)"},
    {"sgn", "", "Sign Languages", "langues des signes", ""},
    {"shn", "", "Shan", "chan", "Shan"},
    {"sid", "", "Sidamo", "sidamo", "Sidamo"},
    {"sin", "si", "Sinhala; Sinhalese", "singhalais", "Singalesisk"},
    {"sio", "", "Siouan languages", "sioux, langues", "Siouan languages"},
    {"sit", "", "Sino-Tibetan (Other)", "sino-tibétaines, autres langues", "Sino-tibetanske sprog (øvrige)"},
    {"sla", "", "Slavic (Other)", "slaves, autres langues", "Slaviske sprog (øvrige)"},
    {"slo/slk", "sk", "Slovak", "slovaque", ""},
    {"slo/slk", "sk", "Slovak", "slovaque", ""},
    {"slv", "sl", "Slovenian", "slovène", "Slovensk"},
    {"sma", "", "Southern Sami", "sami du Sud", ""},
    {"sme", "se", "Northern Sami", "sami du Nord", ""},
    {"smi", "", "Sami languages (Other)", "sami, autres langues", "Sami languages"},
    {"smj", "", "Lule Sami", "sami de Lule", ""},
    {"smn", "", "Inari Sami", "sami d'Inari", ""},
    {"smo", "sm", "Samoan", "samoan", "Samoansk"},
    {"sms", "", "Skolt Sami", "sami skolt", ""},
    {"sna", "sn", "Shona", "shona", "Shona"},
    {"snd", "sd", "Sindhi", "sindhi", "Sindhi"},
    {"snk", "", "Soninke", "soninké", "Soninke"},
    {"sog", "", "Sogdian", "sogdien", "Sogdiansk"},
    {"som", "so", "Somali", "somali", "Somalisk"},
    {"son", "", "Songhai", "songhai", "Songe"},
    {"sot", "st", "Sotho, Southern", "sotho du Sud", "Sotho, Syd"},
    {"spa", "es", "Spanish; Castilian", "espagnol; castillan", "Spansk"},
    {"alb/sqi", "sq", "Albanian", "albanais", ""},
    {"srd", "sc", "Sardinian", "sarde", "Sardinsk"},
    {"srn", "", "Sranan Togo", "sranan togo", ""},
    {"scc/srp", "sr", "Serbian", "serbe", ""},
    {"srr", "", "Serer", "sérère", "Serer"},
    {"ssa", "", "Nilo-Saharan (Other)", "nilo-sahariennes, autres langues", "Afrikanske sprog syd for Sahara (øvrige)"},
    {"ssw", "ss", "Swati", "swati", "Swati"},
    {"suk", "", "Sukuma", "sukuma", "Sukuma"},
    {"sun", "su", "Sundanese", "soundanais", "Sundanesisk"},
    {"sus", "", "Susu", "soussou", "Susu"},
    {"sux", "", "Sumerian", "sumérien", "Sumerisk"},
    {"swa", "sw", "Swahili", "swahili", "Swahili"},
    {"swe", "sv", "Swedish", "suédois", "Svensk"},
    {"syr", "", "Syriac", "syriaque", "Syrisk"},
    {"tah", "ty", "Tahitian", "tahitien", "Tahitiansk"},
    {"tai", "", "Tai (Other)", "thaïes, autres langues", "Tai (øvrige)"},
    {"tam", "ta", "Tamil", "tamoul", "Tamilsk"},
    {"tat", "tt", "Tatar", "tatar", "Tatarisk"},
    {"tel", "te", "Telugu", "télougou", "Telugu"},
    {"tem", "", "Timne", "temne", "Temne"},
    {"ter", "", "Tereno", "tereno", "Tereno"},
    {"tet", "", "Tetum", "tetum", "Tetum"},
    {"tgk", "tg", "Tajik", "tadjik", "Tajik"},
    {"tgl", "tl", "Tagalog", "tagalog", "Tagalog"},
    {"tha", "th", "Thai", "thaï", "Thai"},
    {"tib/bod", "bo", "Tibetan", "tibétain", ""},
    {"tig", "", "Tigre", "tigré", "Tigré"},
    {"tir", "ti", "Tigrinya", "tigrigna", "Tigrinja"},
    {"tiv", "", "Tiv", "tiv", "Tivi"},
    {"tkl", "", "Tokelau", "tokelau", "Tokelau"},
    {"tlh", "", "Klingon; tlhIngan-Hol", "klingon", ""},
    {"tli", "", "Tlingit", "tlingit", "Tlingit"},
    {"tmh", "", "Tamashek", "tamacheq", "Tamashek"},
    {"tog", "", "Tonga (Nyasa)", "tonga (Nyasa)", "Tonga (Nyasa)"},
    {"ton", "to", "Tonga (Tonga Islands)", "tongan (Îles Tonga)", "Tonga (Tongaøerne)"},
    {"tpi", "", "Tok Pisin", "tok pisin", "Tok Pisin"},
    {"tsi", "", "Tsimshian", "tsimshian", "Tsimshisk"},
    {"tsn", "tn", "Tswana", "tswana", "Tswana"},
    {"tso", "ts", "Tsonga", "tsonga", "Tsonga"},
    {"tuk", "tk", "Turkmen", "turkmène", "Turkmensk"},
    {"tum", "", "Tumbuka", "tumbuka", "Tumbuka"},
    {"tup", "", "Tupi languages", "tupi, langues", ""},
    {"tur", "tr", "Turkish", "turc", "Tyrkisk"},
    {"tut", "", "Altaic (Other)", "altaïques, autres langues", "Tyrkisk-tatariske sprog (øvrige)"},
    {"tvl", "", "Tuvalu", "tuvalu", "Tuvalu"},
    {"twi", "tw", "Twi", "twi", "Twi"},
    {"tyv", "", "Tuvinian", "touva", "Tuvinian"},
    {"udm", "", "Udmurt", "oudmourte", ""},
    {"uga", "", "Ugaritic", "ougaritique", "Ugaritisk"},
    {"uig", "ug", "Uighur; Uyghur", "ouïgour", "Uigurisk"},
    {"ukr", "uk", "Ukrainian", "ukrainien", "Ukrainsk"},
    {"umb", "", "Umbundu", "umbundu", "Umbundu"},
    {"und", "", "Undetermined", "indéterminée", "Sproget kan ikke bestemmes"},
    {"urd", "ur", "Urdu", "ourdou", "Urdu"},
    {"uzb", "uz", "Uzbek", "ouszbek", "Usbekisk"},
    {"vai", "", "Vai", "vaï", "Vai"},
    {"ven", "ve", "Venda", "venda", "Venda"},
    {"vie", "vi", "Vietnamese", "vietnamien", "Vietnamesisk"},
    {"vol", "vo", "Volapük", "volapük", "Volapük"},
    {"vot", "", "Votic", "vote", "Votisk"},
    {"wak", "", "Wakashan languages", "wakashennes, langues", "Wakashan languages"},
    {"wal", "", "Walamo", "walamo", "Walamo"},
    {"war", "", "Waray", "waray", "Waray"},
    {"was", "", "Washo", "washo", "Washo"},
    {"wel/cym", "cy", "Welsh", "gallois", ""},
    {"wen", "", "Sorbian languages", "sorabes, langues", "Vendiske sprog"},
    {"wln", "wa", "Walloon", "wallon", ""},
    {"wol", "wo", "Wolof", "wolof", "Wolof"},
    {"xal", "", "Kalmyk; Oirat", "kalmouk; oïrat", ""},
    {"xho", "xh", "Xhosa", "xhosa", "Xhosa"},
    {"yao", "", "Yao", "yao", "Yao"},
    {"yap", "", "Yapese", "yapois", "Yap"},
    {"yid", "yi", "Yiddish", "yiddish", "Jiddisch"},
    {"yor", "yo", "Yoruba", "yoruba", "Yoruba"},
    {"ypk", "", "Yupik languages", "yupik, langues", "Yupik languages"},
    {"zap", "", "Zapotec", "zapotèque", "Zapotec"},
    {"zen", "", "Zenaga", "zenaga", "Zenega"},
    {"zha", "za", "Zhuang; Chuang", "zhuang; chuang", "Zhuang"},
    {"chi/zho", "zh", "Chinese", "chinois", ""},
    {"znd", "", "Zande", "zandé", "Zande"},
    {"zul", "zu", "Zulu", "zoulou", "Zulu"},
    {"zun", "", "Zuni", "zuni", "Zuni"},
    {"zxx", "", "No linguistic content", "pas de contenu linguistique", ""},
    {"zza", "", "Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki", "zaza; dimili; dimli; kirdki; kirmanjki; zazaki", ""}
    };
}
