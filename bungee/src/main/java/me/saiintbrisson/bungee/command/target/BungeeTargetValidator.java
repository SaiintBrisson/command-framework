/*
 * Copyright 2020 Luiz Carlos Mourão Paes de Carvalho
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.saiintbrisson.bungee.command.target;

import me.saiintbrisson.minecraft.command.target.CommandTarget;
import me.saiintbrisson.minecraft.command.target.TargetValidator;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * @author Henry Fábio
 * Github: https://github.com/HenryFabio
 */
public final class BungeeTargetValidator implements TargetValidator {

    public static final BungeeTargetValidator INSTANCE;

    static {
      INSTANCE = new BungeeTargetValidator();
    }

    @Override
    public boolean validate(CommandTarget target, Object object) {
        if (target == CommandTarget.CONSOLE) {
            return !(object instanceof ProxiedPlayer);
        }

        if (target == CommandTarget.PLAYER) {
            return object instanceof ProxiedPlayer;
        }

        return true;
    }

    @Override
    public CommandTarget fromSender(Object object) {
        if (object instanceof ProxiedPlayer) {
            return CommandTarget.PLAYER;
        }

        if (object instanceof CommandSender) {
            return CommandTarget.CONSOLE;
        }

        return CommandTarget.ALL;
    }

}
