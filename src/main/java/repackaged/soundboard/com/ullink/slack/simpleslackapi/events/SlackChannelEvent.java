package repackaged.soundboard.com.ullink.slack.simpleslackapi.events;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;

public interface SlackChannelEvent extends SlackEvent
{
    SlackChannel getSlackChannel();
}
