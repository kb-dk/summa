package dk.statsbiblioteket.summa.support.api;

/**
 * Search keys for the
 * {@link dk.statsbiblioteket.summa.support.didyoumean.DidYouMeanSearchNode}
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Feb 9, 2010
 */
public interface DidYouMeanKeys {
    /**
     * Did-You-Mean search query key.
     */
    public static final String SEARCH_QUERY =
                                           "summa.support.didyoumean.query";
    /**
     * Maximum number of results in a Did-You-Mean query key. 
     */
    public static final String SEARCH_MAX_RESULTS =
                                      "summa.support.didyoumean.maxresults";

}
