package timongcraft.antichatreporting;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerChatHeaderPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {
    private boolean preventtoast;
    private Method holderAddListener;
    private Method holderRemoveListener;
    private Method holderHasListener;
    private Object freedomChannelInitListenerKey;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        final FileConfiguration config = this.getConfig();

        this.preventtoast = config.getBoolean("prevent-toast");


        this.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new JoinMessage(this), this);

        if (!config.getBoolean("mark-as-save_server"))
            return; // no need to check paper/register listener

        try {
            Class<?> holder = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
            Class<?> listener = Class.forName("io.papermc.paper.network.ChannelInitializeListener");
            final Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            freedomChannelInitListenerKey = keyClass.getMethod("key", String.class, String.class).invoke(null, "antichatreporting", "status_injector");

            holderAddListener = holder.getMethod("addListener", keyClass, listener);
            holderRemoveListener = holder.getMethod("removeListener", keyClass);
            holderHasListener = holder.getMethod("hasListener", keyClass);

            final StatusResponseHandler handler = new StatusResponseHandler();

            Object initListener = Proxy.newProxyInstance(this.getClassLoader(), new Class[]{listener}, (proxy, method, args) -> {
                if (method.getName().equals("afterInitChannel") && method.getParameterCount() == 1 && method.getParameterTypes()[0] == Channel.class) {
                    final Channel channel = (Channel) args[0];
                    channel.pipeline().addAfter("packet_handler", "freedom_chat_status_handler", handler);

                    return null;
                }
                return method.invoke(proxy, args);
            });

            holderAddListener.invoke(null, freedomChannelInitListenerKey, initListener);
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                       InvocationTargetException ignored) {
            this.getLogger().warning("The send-enforces-secure-chat-to-client configuration option is only supported on Paper servers. Download at https://papermc.io");
        }
    }

    @Override
    public void onDisable() {
        if (holderHasListener == null || holderRemoveListener == null) return;
        try {
            if ((Boolean) holderHasListener.invoke(null, freedomChannelInitListenerKey)) {
                holderRemoveListener.invoke(null, freedomChannelInitListenerKey);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            getLogger().log(Level.WARNING, "Failed to remove FreedomChat channel init listener", e);
        }
    }

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        final ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
        final ChannelPipeline pipeline = player.connection.connection.channel.pipeline();

        pipeline.addAfter("packet_handler", "freedom_chat", new ChannelDuplexHandler() {
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                // rewrite all signed (or unsigned) player messages as system messages
                if (msg instanceof ClientboundPlayerChatPacket packet) {
                    final Component content = packet.message().unsignedContent().orElse(packet.message().signedContent().decorated());

                    final Optional<ChatType.Bound> ctbo = packet.chatType().resolve(player.level.registryAccess());
                    if (ctbo.isEmpty()) {
                        getLogger().log(Level.WARNING, "Processing packet with unknown ChatType " + packet.chatType().chatType(), new Throwable());
                        return;
                    }
                    final Component decoratedContent = ctbo.orElseThrow().decorate(content);

                    super.write(ctx, new ClientboundSystemChatPacket(decoratedContent, false), promise);
                    return;
                }

                // strip useless header packets
                if (msg instanceof ClientboundPlayerChatHeaderPacket) {
                    return;
                }

                // remove unsigned content warning toast. all messages are now system. (Doesn't display "Chat messages can't be verified")
                if (preventtoast && msg instanceof ClientboundServerDataPacket packet) {
                    super.write(ctx, new ClientboundServerDataPacket(packet.getMotd().orElse(null), packet.getIconBase64().orElse(null), packet.previewsChat(), true), promise);
                    return;
                }

                super.write(ctx, msg, promise);
            }
        });
    }
}
