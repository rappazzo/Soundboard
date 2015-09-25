package repackaged.soundboard.com.ullink.slack.simpleslackapi.impl;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackUser;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackChannelArchived;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackEventType;

class SlackChannelArchivedImpl implements SlackChannelArchived
{
    private SlackChannel slackChannel;
    private SlackUser slackuser;
    
    SlackChannelArchivedImpl(SlackChannel slackChannel, SlackUser slackuser)
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
    public SlackUser getUser()
    {
        return slackuser;
    }

    @Override
    public SlackEventType getEventType()
    {
        return SlackEventType.SLACK_CHANNEL_ARCHIVED;
    }

}
