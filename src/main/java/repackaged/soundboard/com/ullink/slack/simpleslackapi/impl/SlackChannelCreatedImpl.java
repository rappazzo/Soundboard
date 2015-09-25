package repackaged.soundboard.com.ullink.slack.simpleslackapi.impl;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackUser;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackChannelCreated;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackEventType;

class SlackChannelCreatedImpl implements SlackChannelCreated
{
    private SlackChannel slackChannel;
    private SlackUser slackuser;
    
    SlackChannelCreatedImpl(SlackChannel slackChannel, SlackUser slackuser)
    {
        this.slackChannel = slackChannel;
        this.slackuser = slackuser;
    }

    @Override
    public SlackChannel getSlackChannel()
    {
        return slackChannel;
    }

    @Override
    public SlackUser getCreator()
    {
        return slackuser;
    }

    @Override
    public SlackEventType getEventType()
    {
        return SlackEventType.SLACK_CHANNEL_CREATED;
    }

}
