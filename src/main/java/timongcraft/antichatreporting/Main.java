package timongcraft.antichatreporting;

import net.kyori.adventure.key.Key;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static io.papermc.paper.network.ChannelInitializeListenerHolder.*;

public class Main extends JavaPlugin implements Listener {

    private final boolean OnJoinMessageOn = getConfig().getBoolean("message_on_join_on");
    private final String OnJoinMessage = getConfig().getString("message_on_join");
    private static final Key listenerKey = Key.key("antichatreporting", "listener");

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        final Handler handler = new Handler(
                getConfig().getBoolean("prevent-toast"),
                getConfig().getBoolean("mark-as-save-server"),
                this
        );

        addListener(listenerKey, channel -> channel.pipeline().addAfter("packet_handler", "antichatreporting_handler", handler));
    }

    @Override
    public void onDisable() {
        if (hasListener(listenerKey)) removeListener(listenerKey);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(OnJoinMessageOn){
            if(OnJoinMessage != null){
                event.getPlayer().sendMessage(OnJoinMessage.replaceAll("&", "§"));
            }
        }
    }
}
