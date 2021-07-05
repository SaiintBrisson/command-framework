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

package me.saiintbrisson.bungee.command.command;

import lombok.Getter;
import lombok.Setter;
import me.saiintbrisson.bungee.command.BungeeFrame;
import me.saiintbrisson.bungee.command.executor.BungeeCommandExecutor;
import me.saiintbrisson.bungee.command.target.BungeeTargetValidator;
import me.saiintbrisson.minecraft.command.command.CommandHolder;
import me.saiintbrisson.minecraft.command.command.CommandInfo;
import me.saiintbrisson.minecraft.command.executor.CommandExecutor;
import me.saiintbrisson.minecraft.command.executor.CompleterExecutor;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import me.saiintbrisson.minecraft.command.message.MessageType;
import me.saiintbrisson.minecraft.command.util.ArrayUtil;
import me.saiintbrisson.minecraft.command.util.StringUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Henry Fábio
 * Github: https://github.com/HenryFabio
 */
@Getter
public class BungeeCommand extends Command implements CommandHolder<CommandSender, BungeeChildCommand>, TabExecutor {

    private final BungeeFrame frame;
    private final MessageHolder messageHolder;

    private CommandInfo commandInfo;

    private final int position;

    private CommandExecutor<CommandSender> commandExecutor;
    private CompleterExecutor<CommandSender> completerExecutor;

    private String permission;
    private String[] aliases;

    @Setter
    private String usage;

    private final String description;

    private final List<BungeeChildCommand> childCommandList;

    {
      childCommandList = new LinkedList<>();
      description = "Not provided";
    }

    public BungeeCommand(BungeeFrame frame, String name, int position) {
        super(name);

        this.frame = frame;
        this.position = position;
        this.messageHolder = frame.getMessageHolder();
    }

    public final void initCommand(CommandInfo commandInfo, CommandExecutor<CommandSender> commandExecutor) {
        if (this.commandInfo != null) {
            throw new IllegalStateException("Command already initialized");
        }

        this.commandInfo = commandInfo;
        this.commandExecutor = commandExecutor;

        this.aliases = commandInfo.getAliases();

        String permission = commandInfo.getPermission();
        if (!StringUtil.isEmpty(permission)) {
            this.permission = permission;
        }

        final String usage = commandInfo.getUsage();
        if (!StringUtil.isEmpty(usage)) {
            setUsage(usage);
        } else if (commandExecutor instanceof BungeeCommandExecutor) {
            setUsage(((BungeeCommandExecutor) commandExecutor).getEvaluator().buildUsage(getFancyName()));
        }

        if (commandExecutor instanceof BungeeCommandExecutor) {
            ((BungeeCommandExecutor) commandExecutor).setCommand(this);
        }
    }

    public final void initCompleter(CompleterExecutor<CommandSender> completerExecutor) {
        if (this.completerExecutor != null) {
            throw new IllegalStateException("Completer already initialized");
        }

        this.completerExecutor = completerExecutor;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!testPermissionSilent(sender)) {
            sender.sendMessage(new TextComponent(
              messageHolder.getReplacing(MessageType.NO_PERMISSION, getPermission())
            ));
            return;
        }

        if (!BungeeTargetValidator.INSTANCE.validate(commandInfo.getTarget(), sender)) {
            sender.sendMessage(new TextComponent(frame.getMessageHolder().getReplacing(
              MessageType.INCORRECT_TARGET,
              commandInfo.getTarget().name()
            )));
            return;
        }

        if (args.length > 0) {
            BungeeChildCommand command = getChildCommand(args[0]);
            if (command != null) {
                command.execute(sender, ArrayUtil.copyOfRange(args, 1, args.length));
                return;
            }
        }

        if (commandExecutor == null) {
            return;
        }

        commandExecutor.execute(new BungeeContext(sender, BungeeTargetValidator.INSTANCE.fromSender(sender), args, frame, this));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!testPermissionSilent(sender)) {
            return Collections.emptyList();
        }

        if (completerExecutor != null) {
            return completerExecutor.execute(new BungeeContext(sender, BungeeTargetValidator.INSTANCE.fromSender(sender), args, frame, this));
        }

        if (childCommandList.size() != 0 && args.length != 0) {
            final List<String> matchedChildCommands = new ArrayList<>();

            childCommandList.stream()
                .filter(command -> StringUtil.startsWithIgnoreCase(command.getName(), args[args.length - 1]) && command.testPermissionSilent(sender))
                .forEach(command -> matchedChildCommands.add(command.getName()));

            if (matchedChildCommands.size() != 0) {
                matchedChildCommands.sort(String.CASE_INSENSITIVE_ORDER);
                return matchedChildCommands;
            }
        }

        return new ArrayList<>();
    }

    @Override
    public BungeeChildCommand getChildCommand(final String name) {
      return childCommandList.stream()
          .filter(command -> command.equals(name))
          .findFirst()
          .orElse(null);
    }

    public BungeeCommand createRecursive(String name) {
        int position = getPosition() + StringUtil.countMatches(name, ".");
        if (position == getPosition()) {
            return this;
        }

        final String recursive = name.substring(name.indexOf('.') + 1);

        final int index = recursive.indexOf('.');
       final String childCommandName = index != -1 ? recursive.substring(0, index) : recursive;

        final BungeeChildCommand command = getChildCommand(childCommandName);
        final BungeeChildCommand childCommand = command == null ? new BungeeChildCommand(frame, childCommandName, this) : command;

        getChildCommandList().add(childCommand);
        return childCommand.createRecursive(recursive);
    }

    @Override
    public List<String> getAliasesList() {
        return Arrays.asList(getAliases());
    }

    @Override
    public String getFancyName() {
        return getName();
    }

    protected boolean testPermissionSilent(@NotNull CommandSender target) {
      final String permission = getPermission();
        if (permission == null || permission.length() == 0) {
            return true;
        }

        for (String player : permission.split(";")) {
            if (target.hasPermission(player)) {
                return true;
            }
        }

        return false;
    }

}
