package repackaged.soundboard.com.ullink.slack.simpleslackapi.impl;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackEventType;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackGroupJoined;

class SlackGroupJoinedImpl implements SlackGroupJoined
{
    private SlackChannel slackChannel;

    SlackGroupJoinedImpl(SlackChannel slackChannel)
    {
        this.slackChannel = slackChannel;
    }

    @Override
    public SlackChannel getSlackChannel()
    {
        return slackChannel;
    }

    void setSlackChannel(SlackChannel slackChannel)
    {
        this.slackChannel = slackChannel;
    }

    @Override
    public SlackEventType getEventType()
    {
        return SlackEventType.SLACK_GROUP_JOINED;
    }

}
