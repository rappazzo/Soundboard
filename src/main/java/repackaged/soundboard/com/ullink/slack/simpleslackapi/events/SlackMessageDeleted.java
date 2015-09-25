package repackaged.soundboard.com.ullink.slack.simpleslackapi.events;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;

public interface SlackMessageDeleted extends SlackMessageEvent
{
    SlackChannel getChannel();
    String getMessageTimestamp();
}
