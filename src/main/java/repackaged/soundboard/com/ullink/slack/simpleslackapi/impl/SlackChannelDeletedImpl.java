package repackaged.soundboard.com.ullink.slack.simpleslackapi.impl;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackChannelDeleted;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackEventType;

class SlackChannelDeletedImpl implements SlackChannelDeleted
{
    private SlackChannel slackChannel;

    SlackChannelDeletedImpl(SlackChannel slackChannel)
    {
        this.slackChannel = slackChannel;
    }

    @Override
    public SlackChannel getSlackChannel()
    {
        return slackChannel;
    }

    @Override
    public SlackEventType getEventType()
    {
        return SlackEventType.SLACK_CHANNEL_DELETED;
    }
}
