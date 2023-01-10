package timongcraft.antichatreporting;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinMessage implements Listener {

    private final String ChatReportingonJoinMessageOn;
    private final String ChatReportingonJoinMessage;

    public JoinMessage(Main main) {
        this.ChatReportingonJoinMessage = main.getConfig().getString("message_on_join");
        this.ChatReportingonJoinMessageOn = main.getConfig().getString("message_on_join_on");
    }

    public JoinMessage(String disableChatReporting, String chatReportingonJoinMessage) {
        ChatReportingonJoinMessageOn = disableChatReporting;
        ChatReportingonJoinMessage = chatReportingonJoinMessage;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(ChatReportingonJoinMessageOn == "true" ){
            if(ChatReportingonJoinMessage.length() >= 1){
                player.sendMessage(ChatReportingonJoinMessage.replaceAll("&", "§"));
            }
        }
    }
}

