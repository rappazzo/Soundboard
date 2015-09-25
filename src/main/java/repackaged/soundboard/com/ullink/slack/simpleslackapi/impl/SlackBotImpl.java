package repackaged.soundboard.com.ullink.slack.simpleslackapi.impl;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackBot;

class SlackBotImpl implements SlackBot
{

    private String  id;
    private String  name;
    private boolean deleted;

    SlackBotImpl(String id, String name, boolean deleted)
    {
        this.id = id;
        this.name = name;
        this.deleted = deleted;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getUserName()
    {
        return name;
    }

    @Override
    public boolean isDeleted()
    {
        return deleted;
    }
}
