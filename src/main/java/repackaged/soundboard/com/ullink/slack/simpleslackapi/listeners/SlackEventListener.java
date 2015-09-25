package repackaged.soundboard.com.ullink.slack.simpleslackapi.listeners;

import repackaged.soundboard.com.ullink.slack.simpleslackapi.SlackSession;
import repackaged.soundboard.com.ullink.slack.simpleslackapi.events.SlackEvent;

public interface SlackEventListener<T extends SlackEvent>
{
    void onEvent(T event, SlackSession session);
}
