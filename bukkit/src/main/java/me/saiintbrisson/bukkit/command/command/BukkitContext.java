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

package me.saiintbrisson.bukkit.command.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.saiintbrisson.bukkit.command.target.BukkitTargetValidator;
import me.saiintbrisson.minecraft.command.CommandFrame;
import me.saiintbrisson.minecraft.command.command.CommandHolder;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.exception.CommandException;
import me.saiintbrisson.minecraft.command.message.MessageType;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

/**
 * @author SaiintBrisson (https://github.com/SaiintBrisson)
 */
@Getter
@AllArgsConstructor
public final class BukkitContext implements Context<CommandSender> {

    private final String label;
    private final CommandSender sender;
    private final CommandTarget target;
    private final String[] args;

    private final CommandFrame<?, ?, ?> commandFrame;
    private final CommandHolder<?, ?> commandHolder;

    /**
     * Sends a message to the CommandSender
     * @param message the message to be sent
     */
    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

    /**
     * Sends a array of messages to the CommandSender
     * @param messages the messages to be sent
     */
    @Override
    public void sendMessage(String[] messages) {
        sender.sendMessage(messages);
    }

    /**
     * Tests a permission into the CommandSender.
     * <p>Returns true if it was successful</p>
     * @param permission the permission to be tested
     * @param silent     whether a exception should be thrown
     *
     * @return boolenn
     * @throws CommandException Throws if the player doesn't have permission and the test isn't silent.
     */
    @Override
    public boolean testPermission(String permission, boolean silent) throws CommandException {
        if (sender.hasPermission(permission)) {
            return true;
        }

        if (!silent) {
            throw new CommandException(MessageType.NO_PERMISSION, permission);
        }

        return false;
    }

    /**
     * Validates if the target is a {@link CommandTarget}
     * <p>Returns true if the test was successful</p>
     * @param target the target to be tested
     * @param silent whether a exception should be thrown
     *
     * @return boolean
     * @throws CommandException Throws if the validation was wrong and the test isn't silent.
     */
    @Override
    public boolean testTarget(CommandTarget target, boolean silent) throws CommandException {
        if (BukkitTargetValidator.INSTANCE.validate(target, sender)) {
            return true;
        }

        if (!silent) {
            throw new CommandException(MessageType.INCORRECT_USAGE, target.name());
        }

        return false;
    }

}
