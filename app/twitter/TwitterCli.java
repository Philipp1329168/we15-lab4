package twitter;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by root on 27/05/15.
 */
public class TwitterCli implements ITwitterClient {
    String consumerKey = "GZ6tiy1XyB9W0P4xEJudQ";
    String consumerSecret = "gaJDlW0vf7en46JwHAOkZsTHvtAiZ3QUd2mD1x26J9w";
    String accessToken = "1366513208-MutXEbBMAVOwrbFmZtj1r4Ih2vcoHGHE2207002";
    String accessTokenSecret = "RMPWOePlus3xtURWRVnv1TgrjTyK7Zk33evp4KKyA";

    @Override
    public void publishUuid(TwitterStatusMessage message) throws Exception {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey(this.consumerKey);
        cb.setOAuthConsumerSecret(this.consumerSecret);
        cb.setOAuthAccessToken(this.accessToken);
        cb.setOAuthAccessTokenSecret(this.accessTokenSecret);
        Twitter twitter = new TwitterFactory(cb.build()).getInstance();
        twitter.updateStatus(message.getTwitterPublicationString());
    }
}
