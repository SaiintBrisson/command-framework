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

package me.saiintbrisson.bungee.command;

import lombok.Getter;
import me.saiintbrisson.bungee.command.command.BungeeCommand;
import me.saiintbrisson.bungee.command.executor.BungeeCommandExecutor;
import me.saiintbrisson.bungee.command.executor.BungeeCompleterExecutor;
import me.saiintbrisson.minecraft.command.CommandFrame;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Completer;
import me.saiintbrisson.minecraft.command.argument.AdapterMap;
import me.saiintbrisson.minecraft.command.argument.eval.MethodEvaluator;
import me.saiintbrisson.minecraft.command.command.CommandInfo;
import me.saiintbrisson.minecraft.command.executor.CommandExecutor;
import me.saiintbrisson.minecraft.command.executor.CompleterExecutor;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author Henry Fábio
 * Github: https://github.com/HenryFabio
 */
@Getter
public final class BungeeFrame implements CommandFrame<Plugin, CommandSender, BungeeCommand> {

    private final Plugin plugin;
    private final AdapterMap adapterMap;
    private final MessageHolder messageHolder;

    private final Map<String, BungeeCommand> commandMap;
    private final MethodEvaluator methodEvaluator;

    public BungeeFrame(Plugin plugin, AdapterMap adapterMap) {
        this.plugin = plugin;

        this.adapterMap = adapterMap;
        this.messageHolder = new MessageHolder();

        this.commandMap = new HashMap<>();
        this.methodEvaluator = new MethodEvaluator(adapterMap);
    }

    public BungeeFrame(Plugin plugin, boolean registerDefault) {
        this(plugin, new AdapterMap(registerDefault));

        if (registerDefault) {
            registerAdapter(ProxiedPlayer.class, ProxyServer.getInstance()::getPlayer);
        }
    }

    public BungeeFrame(Plugin plugin) {
        this(plugin, true);
    }

    @Override
    public @Nullable Executor getExecutor() {
        return null;
    }

    @Override
    public BungeeCommand getCommand(String name) {
        final int index = name.indexOf('.');
        final String recursiveCommand = (index != -1 ? name.substring(0, index) : name).toLowerCase();

        final BungeeCommand bungeeCommand = commandMap.get(recursiveCommand);
        final BungeeCommand command = bungeeCommand == null ? new BungeeCommand(this, recursiveCommand, 0) : bungeeCommand;

        commandMap.put(recursiveCommand, command);
        return index != -1 ? command.createRecursive(name) : command;
    }

    @Override
    public void registerCommands(Object... objects) {
        for (Object object : objects) {
            for (Method method : object.getClass().getDeclaredMethods()) {
                final Command command = method.getAnnotation(Command.class);
                if (command != null) {
                    registerCommand(new CommandInfo(command), new BungeeCommandExecutor(this, method, object));
                    continue;
                }

                final Completer completer = method.getAnnotation(Completer.class);
                if (completer != null) {
                    registerCompleter(completer.name(), new BungeeCompleterExecutor(method, object));
                }
            }
        }
    }

    @Override
    public void registerCommand(CommandInfo commandInfo, CommandExecutor<CommandSender> commandExecutor) {
        final BungeeCommand recursive = getCommand(commandInfo.getName());
        if (recursive == null) {
            return;
        }

        recursive.initCommand(commandInfo, commandExecutor);

        if (recursive.getPosition() == 0) {
            ProxyServer.getInstance().getPluginManager().registerCommand(plugin, recursive);
        }
    }

    @Override
    public void registerCompleter(String name, CompleterExecutor<CommandSender> completerExecutor) {
        final BungeeCommand recursive = getCommand(name);
        if (recursive == null) {
            return;
        }

        recursive.initCompleter(completerExecutor);
    }

    @Override
    public boolean unregisterCommand(String name) {
        final BungeeCommand command = commandMap.remove(name);
        if (command == null) {
          return false;
        }

        ProxyServer.getInstance().getPluginManager().unregisterCommand(command);
        return true;
    }

}
