package com.ullink.slack.simpleslackapi.impl;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ullink.slack.simpleslackapi.SlackBot;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPersona;
import com.ullink.slack.simpleslackapi.SlackUser;

class SlackJSONSessionStatusParser
{
    private static final Logger       LOGGER   = LoggerFactory.getLogger(SlackJSONSessionStatusParser.class);

    private Map<String, SlackChannel> channels = new HashMap<String, SlackChannel>();
    private Map<String, SlackUser>    users    = new HashMap<String, SlackUser>();
    private Map<String, SlackBot>     bots     = new HashMap<String, SlackBot>();

    private SlackPersona sessionPersona;

    private String                    webSocketURL;

    private String                    toParse;

    private String                    error;

    SlackJSONSessionStatusParser(String toParse)
    {
        this.toParse = toParse;
    }

    Map<String, SlackBot> getBots()
    {
        return bots;
    }

    Map<String, SlackChannel> getChannels()
    {
        return channels;
    }

    Map<String, SlackUser> getUsers()
    {
        return users;
    }

    public String getWebSocketURL()
    {
        return webSocketURL;
    }

    public String getError()
    {
        return error;
    }

    void parse() throws ParseException
    {
        LOGGER.debug("parsing session status : " + toParse);
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(toParse);
        Boolean ok = (Boolean)jsonResponse.get("ok");
        if (Boolean.FALSE.equals(ok)) {
            error = (String)jsonResponse.get("error");
            return;
        }
        JSONArray usersJson = (JSONArray) jsonResponse.get("users");

        for (Object jsonObject : usersJson)
        {
            JSONObject jsonUser = (JSONObject) jsonObject;
            SlackUser slackUser = SlackJSONParsingUtils.buildSlackUser(jsonUser);
            LOGGER.debug("slack user found : " + slackUser.getId());
            users.put(slackUser.getId(), slackUser);
        }

        JSONArray botsJson = (JSONArray) jsonResponse.get("bots");

        for (Object jsonObject : botsJson)
        {
            JSONObject jsonBot = (JSONObject) jsonObject;
            SlackBot slackBot = SlackJSONParsingUtils.buildSlackBot(jsonBot);
            LOGGER.debug("slack bot found : " + slackBot.getId());
            bots.put(slackBot.getId(), slackBot);
        }

        JSONArray channelsJson = (JSONArray) jsonResponse.get("channels");

        for (Object jsonObject : channelsJson)
        {
            JSONObject jsonChannel = (JSONObject) jsonObject;
            SlackChannelImpl channel = SlackJSONParsingUtils.buildSlackChannel(jsonChannel, users);
            LOGGER.debug("slack public channel found : " + channel.getId());
            channels.put(channel.getId(), channel);
        }

        JSONArray groupsJson = (JSONArray) jsonResponse.get("groups");

        for (Object jsonObject : groupsJson)
        {
            JSONObject jsonChannel = (JSONObject) jsonObject;
            SlackChannelImpl channel = SlackJSONParsingUtils.buildSlackChannel(jsonChannel, users);
            LOGGER.debug("slack private group found : " + channel.getId());
            channels.put(channel.getId(), channel);
        }

        JSONObject selfJson = (JSONObject) jsonResponse.get("self");
        sessionPersona = SlackJSONParsingUtils.buildSlackUser(selfJson);


        webSocketURL = (String) jsonResponse.get("url");

    }

    public SlackPersona getSessionPersona() {
        return sessionPersona;
    }
}
