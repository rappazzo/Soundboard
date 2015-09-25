package repackaged.soundboard.com.ullink.slack.simpleslackapi.events;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackUser;

public interface SlackChannelArchived extends SlackChannelEvent
{
    SlackUser getUser();
}
