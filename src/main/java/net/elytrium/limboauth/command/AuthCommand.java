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

package net.elytrium.limboauth.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.Collections;
import java.util.List;
import net.elytrium.limboauth.LimboAuth;
import net.elytrium.limboauth.handler.AuthSessionHandler;

public class AuthCommand implements SimpleCommand {

    private final LimboAuth plugin;
    private final String commandName;

    public AuthCommand(LimboAuth plugin, String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return;
        }
        Player player = (Player) invocation.source();
        AuthSessionHandler handler = this.plugin.getAuthenticatingPlayer(player.getUsername());

        // 只有处于验证状态的玩家才能使用此指令
        if (handler != null) {
            String fullCommand = "/" + this.commandName;
            if (invocation.arguments().length > 0) {
                fullCommand += " " + String.join(" ", invocation.arguments());
            }
            handler.handleChat(fullCommand);
        } else {
            // 如果玩家已经登录，通常不应该再响应这些指令，或者提示“已登录”
            // 这里可以留空，或者发送一条“你已经登录了”的消息
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return Collections.emptyList(); // 为了安全，不提示密码等参数
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}