package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.feedback.ConsoleFeedback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class used to instantiate {@link Feedback} objects of the right type.
 */
public class FeedbackFactory {

    private static final Log log = LogFactory.getLog(FeedbackFactory.class);

    /**
     * Instantiate a new {@link Feedback} of the class defined in
     * {@link ClientDeployer#DEPLOYER_FEEDBACK_PROPERTY}. If this property
     * is not defined the factory will default to a {@link ConsoleFeedback}.
     *
     * @param conf the configuration used to look up the value of the
     *             {@link ClientDeployer#DEPLOYER_FEEDBACK_PROPERTY} property
     * @return a newly created {@code Feedvback} instance
     */
    public static Feedback createFeedback (Configuration conf) {
         log.debug("Creating deployer feedback from class: "
                  + conf.getString(ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY));
        Class<? extends Feedback> feedbackClass =
                           conf.getClass(ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY,
                                         Feedback.class,
                                         ConsoleFeedback.class);
        Feedback feedback = Configuration.create(feedbackClass, conf);

        return feedback;
    }
}
