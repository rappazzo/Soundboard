package repackaged.soundboard.com.ullink.slack.simpleslackapi.events;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackUser;

public interface SlackChannelCreated extends SlackChannelEvent
{
    SlackUser getCreator();
}
