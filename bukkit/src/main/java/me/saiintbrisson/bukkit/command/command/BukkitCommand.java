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

import lombok.Getter;
import lombok.NonNull;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import me.saiintbrisson.bukkit.command.executor.BukkitCommandExecutor;
import me.saiintbrisson.bukkit.command.target.BukkitTargetValidator;
import me.saiintbrisson.minecraft.command.command.CommandHolder;
import me.saiintbrisson.minecraft.command.command.CommandInfo;
import me.saiintbrisson.minecraft.command.executor.CommandExecutor;
import me.saiintbrisson.minecraft.command.executor.CompleterExecutor;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import me.saiintbrisson.minecraft.command.message.MessageType;
import me.saiintbrisson.minecraft.command.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The BukkitCommand is the main implementation of the
 * AbstractCommand, it contains the main information about
 * that command {@link CommandInfo} position and Child commands {@link BukkitChildCommand}
 *
 * @author SaiintBrisson (https://github.com/SaiintBrisson)
 */
@Getter
public class BukkitCommand extends Command implements CommandHolder<CommandSender, BukkitChildCommand> {

    private final BukkitFrame frame;
    private final MessageHolder messageHolder;

    private CommandInfo commandInfo;

    private final int position;

    private CommandExecutor<CommandSender> commandExecutor;
    private CompleterExecutor<CommandSender> completerExecutor;

    private final List<BukkitChildCommand> childCommandList;

    /**
     * Creates a new BukkitCommand with the name provided.
     * @param frame BukkitFrame
     * @param name String
     * @param position Integer
     */
    public BukkitCommand(@NonNull @NotNull BukkitFrame frame, @NonNull @NotNull String name, int position) {
        super(name);

        this.frame = frame;
        this.position = position;

        this.messageHolder = frame.getMessageHolder();
        this.childCommandList = new ArrayList<>();
    }

    /**
     * Initializes the command when the server is started.
     * <p>If you try to register the same commands multiple times, it throws
     * a CommandException</p>
     * @param commandInfo CommandInfo
     * @param commandExecutor CommandExecutor
     */
    public final void initCommand(CommandInfo commandInfo, CommandExecutor<CommandSender> commandExecutor) {
        if (this.commandInfo != null) {
            throw new CommandException("Command already initialized");
        }

        this.commandInfo = commandInfo;
        this.commandExecutor = commandExecutor;

        setAliases(Arrays.asList(commandInfo.getAliases()));

        if (StringUtils.isNotEmpty(commandInfo.getPermission())) {
            setPermission(commandInfo.getPermission());
        }

        final String usage = commandInfo.getUsage();
        if (StringUtils.isNotEmpty(usage)) {
            setUsage(usage);
        } else if (commandExecutor instanceof BukkitCommandExecutor) {
            setUsage(((BukkitCommandExecutor) commandExecutor).getEvaluator().buildUsage(getFancyName()));
        }

        if ((StringUtils.isNotEmpty(commandInfo.getDescription()))) {
            setDescription(commandInfo.getDescription());
        }

        if (commandExecutor instanceof BukkitCommandExecutor) {
            ((BukkitCommandExecutor) commandExecutor).setCommand(this);
        }
    }

    public final void initCompleter(CompleterExecutor<CommandSender> completerExecutor) {
        if (this.completerExecutor != null) {
            throw new IllegalStateException("Completer already initialized");
        }

        this.completerExecutor = completerExecutor;
    }

    /**
     * Get the Child command from this by the name, if it's not register it
     * will return null.
     * @param name String
     *
     * @return BukkitChildCommand
     */
    @Override @Nullable
    public BukkitChildCommand getChildCommand(String name) {
      return childCommandList.stream()
          .filter(command -> command.equals(name))
          .findFirst()
          .orElse(null);
    }

    @Override
    public String getFancyName() {
        return getName();
    }

    /**
     * Executes the command with the provided label and arguments.
     * <p>If returns false, it wasn't able to execute</p>
     * @param sender CommandSender
     * @param commandLabel String
     * @param args String[]
     *
     * @return boolean
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!frame.getPlugin().isEnabled()) {
            return false;
        }

        if (!testPermissionSilent(sender)) {
            sender.sendMessage(messageHolder.getReplacing(MessageType.NO_PERMISSION, getPermission()));
            return false;
        }

        if (commandInfo != null && !BukkitTargetValidator.INSTANCE.validate(commandInfo.getTarget(), sender)) {
            sender.sendMessage(frame.getMessageHolder().getReplacing(MessageType.INCORRECT_TARGET, commandInfo.getTarget().name()));
            return false;
        }

        if (args.length > 0) {
            final BukkitChildCommand command = getChildCommand(args[0]);
            if (command != null) {
                final String label = commandLabel + " " + args[0];
                return command.execute(sender, label, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        if (commandExecutor == null) {
            return false;
        }

        final BukkitContext context = new BukkitContext(commandLabel, sender, BukkitTargetValidator.INSTANCE.fromSender(sender), args, frame, this);
        if (commandInfo.isAsync() && frame.getExecutor() != null) {
            frame.getExecutor().execute(() -> commandExecutor.execute(context));
            return false;
        }

        return commandExecutor.execute(context);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                                             @NotNull String[] args) throws IllegalArgumentException {
        if (!testPermissionSilent(sender)) {
            return Collections.emptyList();
        }

        if (completerExecutor != null) {
            return completerExecutor.execute(new BukkitContext(alias, sender, BukkitTargetValidator.INSTANCE.fromSender(sender), args, frame, this));
        }

        if (childCommandList.size() != 0 && args.length != 0) {
            List<String> matchedChildCommands = new ArrayList<>();

            childCommandList.stream()
                .filter(command -> StringUtil.startsWithIgnoreCase(command.getName(), args[args.length - 1]) && command.testPermissionSilent(sender))
                .forEach(command -> matchedChildCommands.add(command.getName()));

            if (matchedChildCommands.size() != 0) {
                matchedChildCommands.sort(String.CASE_INSENSITIVE_ORDER);
                return matchedChildCommands;
            }
        }

        return super.tabComplete(sender, alias, args);
    }

    public BukkitCommand createRecursive(String name) {
        final int position = getPosition() + StringUtils.countMatches(name, ".");
        if (position == getPosition()) {
            return this;
        }

        final String subName = name.substring(Math.max(name.indexOf('.') + 1, 0));

        final int index = subName.indexOf('.');
        String nextSubCommand = subName;
        if (index != -1) {
            nextSubCommand = subName.substring(0, index);
        }

        final BukkitChildCommand command = getChildCommand(nextSubCommand);
        final BukkitChildCommand childCommand = command == null ? new BukkitChildCommand(frame, nextSubCommand, this) : command;

        getChildCommandList().add(childCommand);
        return childCommand.createRecursive(subName);
    }

    @Override
    public List<String> getAliasesList() {
        return getAliases();
    }

}
