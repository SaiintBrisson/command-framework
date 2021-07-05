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

package me.saiintbrisson.bukkit.command.target;

import me.saiintbrisson.minecraft.command.target.CommandTarget;
import me.saiintbrisson.minecraft.command.target.TargetValidator;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * The BukkitTargetValidator validates if the Target
 * is a correct and usable {@link CommandTarget}
 *
 * @author SaiintBrisson (https://github.com/SaiintBrisson)
 */
public final class BukkitTargetValidator implements TargetValidator {

    public static final BukkitTargetValidator INSTANCE;

    static {
      INSTANCE = new BukkitTargetValidator();
    }

    /**
     * Tries to validate the Command target and Sender object.
     * <p> Returns false if it wasn't validated</p>
     * @param target CommandTarget
     * @param object Object
     *
     * @return Boolean
     */
    @Override
    public boolean validate(CommandTarget target, Object object) {
        if (target == CommandTarget.ALL) {
            return true;
        }

        if (target == CommandTarget.PLAYER && object instanceof Player) {
            return true;
        }

        return target == CommandTarget.CONSOLE && object instanceof ConsoleCommandSender;
    }

    /**
     * Returns the CommandTarget by the Sender object
     * <p>Example: The Player object returns a {@link CommandTarget} of PLAYER</p>
     * @param object Object
     *
     * @return CommandTarget
     */
    @Override
    public CommandTarget fromSender(Object object) {
        if (object instanceof Player) {
            return CommandTarget.PLAYER;
        }

        if (object instanceof ConsoleCommandSender) {
            return CommandTarget.CONSOLE;
        }

        return CommandTarget.ALL;
    }

}
