package dk.statsbiblioteket.summa.search.dummy;

import dk.statsbiblioteket.summa.search.Response;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: Aug 15, 2008
 * Time: 1:05:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class DummyResponse implements Response {

        protected String warmUps;
        protected String opens;
        protected String closes;
        protected String searches;

        public DummyResponse (int warmUps, int opens, int closes, int searches) {
            this.warmUps = "" + warmUps;
            this.opens = "" + opens;
            this.closes = "" + closes;
            this.searches = "" + searches;
        }

        public String getName () {
            return "DummyResponse";
        }

        public void merge (Response other) throws ClassCastException {
            DummyResponse resp = (DummyResponse)other;
            warmUps += ", " + resp.warmUps;
            opens += ", " + resp.opens;
            closes += ", " + resp.closes;
            searches += ", " + resp.searches;
        }

        public String toXML () {
            return String.format ("<DummyResponse>\n" +
                                  "  <warmUps>%s</warmUps>\n"+
                                  "  <opens>%s</opens>\n"+
                                  "  <closes>%s</closes>\n"+
                                  "  <searches>%s</searches>\n"+
                                  "</DummyResponse>",
                                  warmUps, opens, closes, searches);
        }
    }
