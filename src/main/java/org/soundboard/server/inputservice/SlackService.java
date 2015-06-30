/**
 *
 */
package org.soundboard.server.inputservice;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.soundboard.server.LoggingService;
import org.soundboard.server.SoundboardConfiguration;
import org.soundboard.server.command.CommandHandler;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

/**
 * @author mike
 *
 */
public class SlackService extends InputService {

   public static final String SERVICE_NAME = "slack";
   public static final String API_KEY = "api-key";

   public static final String TEAM = "team";
   public static final String BOT_USER = "bot-user";
   public static final String CHANNEL = "channel";
   public static final String REPLY_DIRECT_ONLY = "reply-direct-only";

   private SlackSession session = null;

   public SlackService() {
   }

   @Override public boolean initialize() {
      final String apiKey = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), API_KEY);
      // String team = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), TEAM);
      final String botUserName = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), BOT_USER);
      final String channel = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), CHANNEL);
      final boolean replyDirectOnly = Boolean.valueOf(SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), REPLY_DIRECT_ONLY));


      final Pattern MENTION = Pattern.compile("@" + botUserName + ":? `(.*?)`");

      session = SlackSessionFactory.createWebSocketSlackSession(apiKey);
      session.addMessagePostedListener(new SlackMessagePostedListener() {
         @Override public void onEvent(SlackMessagePosted event, SlackSession session) {

            // ignore own messages
            if (!event.getSender().getUserName().equals(botUserName)) {
               String content = event.getMessageContent();
               String[] command = null;
               if (event.getChannel().isDirect() || event.getChannel().getName().equals(channel)) {
                  command = content.split(" ");
               } else {
                  // look for "(@<bot>: cmd args)"
                  Matcher mention = MENTION.matcher(content);
                  if (mention.find()) {
                     command = mention.group(1).split(" ");
                  }
               }
               if (command != null) {
                  CommandHandler handler = new CommandHandler();
                  String response = handler.handleCommand(SlackService.this, event.getSender().getUserName(), command);
                  if (response != null && !response.isEmpty()) {
                     if (response.length() > 100) {
                        response = "```"+response+"```";
                     }
                     SlackChannel slackChannel = null;
                     if (replyDirectOnly) {
                        slackChannel = findDirectChannel(event, session);
                     } else {
                        String cmd = command[0];
                        //The responses to these command are always long, and should be published in a direct channel
                        boolean replyDirect =
                           cmd.equals(CommandHandler.HELP) ||
                           cmd.equals("List") ||
                           cmd.equals("LIST") ||
                           cmd.equals("list")
                        ;

                        if (replyDirect) {
                           slackChannel = findDirectChannel(event, session);
                           LoggingService.getInstance().log("SlackService: No private channel for "+event.getSender());
                        } else {
                           slackChannel = session.findChannelByName(channel);
                           //public messages get "mentioned"
                           response = response.replaceFirst("^I said", "@"+event.getSender().getUserName() + ": ");
                        }
                     }
                     if (slackChannel != null) {
                        session.sendMessage(slackChannel, response, null);
                     }
                  }
               }
            }
         }
      });
      try {
         session.connect();
      } catch (IOException e) {
         LoggingService.getInstance().serverLog(e);
         return false;
      }

      return true;
   }

   private SlackChannel findDirectChannel(SlackMessagePosted event, SlackSession session) {
      if (event.getChannel().isDirect()) {
         return event.getChannel();
      } else {
         for (SlackChannel slackChannel : session.getChannels()) {
            if (slackChannel.isDirect()) {
               if (slackChannel.getMembers().contains(event.getSender())) {
                  return slackChannel;
               }
            }
         }
      }
      return null;
   }

   @Override public boolean isRunning() {
      return true;
   }

   @Override public void stopRunning() {
      //no-op
   }

   @Override public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override public void send(String to, String message) {
      String channel = SoundboardConfiguration.config().getProperty(SoundboardConfiguration.INPUT, getServiceName(), CHANNEL);
      SlackChannel slackChannel = session.findChannelByName(channel);
      session.sendMessage(slackChannel, "@"+to + ":" + message, null);
   }

   @Override public boolean isAvailable(String userName) {
      return true;
   }

}
