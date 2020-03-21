package me.saiintbrisson.commands;

import lombok.Getter;
import lombok.Setter;
import me.saiintbrisson.commands.annotations.Command;
import me.saiintbrisson.commands.result.ResultType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LocalCommand extends org.bukkit.command.Command {

    private CommandFrame owner;
    private final @Getter List<LocalCommand> subCommands = new ArrayList<>();

    private Object holder;
    private Method method;

    private boolean async;
    private boolean inGameOnly;

    private @Setter Method completer;

    public LocalCommand(CommandFrame owner, Object holder, Command command, Method method) {
        super(command.name().split("\\.")[command.name().split("\\.").length - 1]);
        // TODO: 3/21/2020 fix this shitty line ^^

        this.owner = owner;
        this.holder = holder;
        this.method = method;

        setAliases(Arrays.asList(command.aliases()));
        setDescription(command.description());
        setUsage(command.usage().equals("") ? command.name() : command.usage());
        setPermission(command.permission().equals("") ? null : command.permission());

        async = command.async();
        inGameOnly = command.inGameOnly();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(!owner.getOwner().isEnabled()) {
            return false;
        }

        if(inGameOnly && !(sender instanceof Player)) {
            sender.sendMessage(owner.getInGameOnlyMessage());
            return false;
        }

        if(!testPermissionSilent(sender)) {
            sender.sendMessage(owner.getLackPermMessage());
            return false;
        }

        if(args.length > 0) {
            LocalCommand command = owner.matchCommand(args[0], subCommands);
            if(command != null) {
                return command.execute(
                        sender,
                        commandLabel + " " + args[0],
                        Arrays.copyOfRange(args, 1, args.length)
                );
            }
        }

        Execution execution = new Execution(sender, commandLabel, args);

        if(async) {
            CompletableFuture.runAsync(() -> run(execution));
        } else {
            return run(execution);
        }

        return false;
    }

    private boolean run(Execution execution) {
        Object object = invoke(execution);

        if(object instanceof Void) {
            return false;
        } else if(object instanceof Boolean) {
            return (Boolean) object;
        } else if(object instanceof ResultType) {
            ResultType resultType = (ResultType) object;

            switch (resultType) {
                case NO_PERMISSION:
                    execution.sendMessage(owner.getLackPermMessage());
                    return false;
                case INCORRECT_USAGE:
                    execution.sendMessage(owner.getUsageMessage().replace("{usage}", getUsage()));
                    return false;
                case IN_GAME_ONLY:
                    execution.sendMessage(owner.getInGameOnlyMessage());
                    return false;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private Object invoke(Execution execution) {
        Class<?>[] types = method.getParameterTypes();

        try {
            if(types.length == 0) {
                return method.invoke(holder);
            } else if(types.length == 1 && types[0] == Execution.class) {
                return method.invoke(holder, execution);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {
        if(!testPermissionSilent(sender)) {
            return new ArrayList<>();
        }

        if(args.length > 0) {
            LocalCommand command = owner.matchCommand(args[0], subCommands);
            if(command != null) {
                return command.tabComplete(
                        sender,
                        alias + " " + args[0],
                        Arrays.copyOfRange(args, 1, args.length)
                );
            }
        }

        if(completer == null || completer.getReturnType() != List.class) {
            if(subCommands.size() == 0) {
                return Bukkit.getOnlinePlayers()
                        .stream()
                        .map(Player::getName)
                        .filter(s -> args.length <= 0
                                || s.toLowerCase()
                                .startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                return subCommands.stream()
                        .map(LocalCommand::getName)
                        .filter(s -> args.length <= 0
                                || s.toLowerCase()
                                .startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        Class<?>[] types = completer.getParameterTypes();

        try {
            if(types.length == 0) {
                return (List<String>) completer.invoke(holder);
            } else if(types.length == 1 && types[0] == Execution.class) {
                return (List<String>) completer.invoke(holder,
                        new Execution(sender, alias, args));
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}