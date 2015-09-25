package repackaged.soundboard.com.ullink.slack.simpleslackapi.impl;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackUser;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackChannelUnarchived;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackEventType;

class SlackChannelUnarchivedImpl implements SlackChannelUnarchived
{
    private SlackChannel slackChannel;
    private SlackUser slackuser;
    
    SlackChannelUnarchivedImpl(SlackChannel slackChannel, SlackUser slackuser)
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
        return SlackEventType.SLACK_CHANNEL_UNARCHIVED;
    }

}
