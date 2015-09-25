package repackaged.soundboard.com.ullink.slack.simpleslackapi.impl;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackBot;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackChannel;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackMessage;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackUser;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackEventType;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

class SlackMessagePostedImpl implements SlackMessagePosted
{
    private String       messageContent;
    private SlackUser    user;
    private SlackBot     bot;
    private SlackChannel channel;
    private String       timestamp;

    SlackMessagePostedImpl(String messageContent, SlackBot bot, SlackUser user, SlackChannel channel, String timestamp)
    {
        this.channel = channel;
        this.messageContent = messageContent;
        this.user = user;
        this.bot = bot;
        this.timestamp = timestamp;
    }

    @Override
    public String getMessageContent()
    {
        return messageContent;
    }

    @Override
    public SlackUser getSender()
    {
        return user;
    }

    @Override
    public SlackBot getBot()
    {
        return bot;
    }

    @Override
    public SlackChannel getChannel()
    {
        return channel;
    }

    @Override
    public String getTimeStamp()
    {
        return timestamp;
    }

    @Override
    public String toString()
    {
        return "SlackMessageImpl{" + "messageContent='" + messageContent + '\'' + ", user=" + user + ", bot=" + bot + ", channel=" + channel + '}';
    }

    @Override
    public SlackEventType getEventType()
    {
        return SlackEventType.SLACK_MESSAGE_POSTED;
    }
}
