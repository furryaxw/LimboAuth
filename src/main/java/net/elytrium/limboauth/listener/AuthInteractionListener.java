/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.Settings;
import net.elytrium.limboauth.handler.AuthSessionHandler;

public class AuthInteractionListener {

    private final LimboAuth plugin;

    public AuthInteractionListener(LimboAuth plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        AuthSessionHandler handler = plugin.getAuthenticatingPlayer(player.getUsername());

        // [修改] 移除了 "&& event.getPreviousServer() == null"
        // 只要玩家处于验证状态 (handler != null)，无论是刚进服还是切服过来，都尝试显示 UI
        if (handler != null) {
             RegisteredServer currentServer = player.getCurrentServer()
                 .map(ServerConnection::getServer)
                 .orElse(null);

             if (isAuthServer(currentServer)) {
                 // 这里会发送 Title。
                 // 因为是在 PostConnect 触发，此时世界已加载完成，Title 不会被清除。
                 handler.onJoinAuthServer();
             }
        }
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        AuthSessionHandler handler = plugin.getAuthenticatingPlayer(player.getUsername());
        if (handler != null) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            handler.handleChat(event.getMessage());
        }
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) return;
        Player player = (Player) event.getCommandSource();
        AuthSessionHandler handler = plugin.getAuthenticatingPlayer(player.getUsername());

        if (handler != null) {
            String command = "/" + event.getCommand();

            // [修改] 如果是 Auth 指令，放行（因为 Velocity 会调用我们注册的 AuthCommand）
            if (!isAuthCommand(command)) {
                // [关键] 拦截所有非 Auth 指令 (如 /server, /glist 等)
                event.setResult(CommandExecuteEvent.CommandResult.denied());
            }
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof Player)) return;
        Player player = (Player) event.getSource();
        AuthSessionHandler handler = plugin.getAuthenticatingPlayer(player.getUsername());

        if (handler != null) {
            ChannelIdentifier id = event.getIdentifier();
            if (id.equals(plugin.getChannelIdentifier(player))) {
                byte[] data = event.getData();
                ByteBuf buf = Unpooled.wrappedBuffer(data);
                handler.handlePluginMessage(buf);
                event.setResult(PluginMessageEvent.ForwardResult.handled());
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        AuthSessionHandler handler = plugin.getAuthenticatingPlayer(player.getUsername());
        if (handler != null) {
            handler.onDisconnect();
        }
    }

    private boolean isAuthServer(RegisteredServer server) {
        if (server == null) return false;
        String name = server.getServerInfo().getName();
        return Settings.IMP.MAIN.AUTH_SERVERS.contains(name);
    }

    private boolean isAuthCommand(String command) {
        // Simple check to see if the command corresponds to any login/register aliases
        String[] parts = command.split(" ");
        String root = parts[0];
        return Settings.IMP.MAIN.LOGIN_COMMAND.contains(root) ||
                Settings.IMP.MAIN.REGISTER_COMMAND.contains(root) ||
                Settings.IMP.MAIN.TOTP_COMMAND.contains(root);
    }
}
