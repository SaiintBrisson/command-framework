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

package me.saiintbrisson.bukkit.command.executor;

import lombok.Getter;
import lombok.Setter;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import me.saiintbrisson.bukkit.command.command.BukkitCommand;
import me.saiintbrisson.minecraft.command.argument.eval.ArgumentEvaluator;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.exception.CommandException;
import me.saiintbrisson.minecraft.command.executor.CommandExecutor;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import me.saiintbrisson.minecraft.command.message.MessageType;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The BukkitCommandExecutor is the main executor of each
 * method that is listed as a Command, it invokes the method
 * and executes everything inside.
 *
 * @author SaiintBrisson (https://github.com/SaiintBrisson)
 */
public final class BukkitCommandExecutor implements CommandExecutor<CommandSender> {

    private final Method method;
    private final Object holder;

    @Getter
    private final ArgumentEvaluator<CommandSender> evaluator;

    private final MessageHolder messageHolder;

    @Setter
    private BukkitCommand command;

    /**
     * Creates a new BukkitCommandExecutor with the provided
     * Command method to execute and Command holder
     * @param frame BukkitFrame
     * @param method Method
     * @param holder Object
     */
    public BukkitCommandExecutor(BukkitFrame frame, Method method, Object holder) {
        final Class<?> returnType = method.getReturnType();

        if (!returnType.equals(Void.TYPE)
          && !returnType.equals(Boolean.TYPE)) {
            throw new CommandException("Illegal return type, '" + method.getName());
        }

        this.method = method;
        this.holder = holder;

        this.evaluator = new ArgumentEvaluator<>(frame.getMethodEvaluator().evaluateMethod(method));
        this.messageHolder = frame.getMessageHolder();
    }

    /**
     * Executes the command with the provided context
     * <p>Returns false if the execution wasn't successful</p>
     * @param context Context
     *
     * @return boolean
     */
    @Override
    public boolean execute(Context<CommandSender> context) {
        final Object result = invokeCommand(context);

        if (result != null && result.getClass().equals(Boolean.TYPE)) {
            return ((boolean) result);
        }

        return false;
    }

    /**
     * Invokes the command method and returns the
     * result of dispatching that method.
     * @param context Context
     *
     * @return Object
     */
    public Object invokeCommand(Context<CommandSender> context) {
        try {
            if (evaluator.getArgumentList().size() == 0) {
                return method.invoke(holder);
            }

            final Object[] parameters;
            try {
                parameters = evaluator.parseArguments(context);
            } catch (Exception e) {
                throw new InvocationTargetException(new CommandException(MessageType.INCORRECT_USAGE, null));
            }

            return method.invoke(holder, parameters);
        } catch (InvocationTargetException targetException) {
            final Throwable throwable = targetException.getTargetException();

            if (!(throwable instanceof CommandException)) {
                targetException.printStackTrace();
                context.sendMessage("§cAn internal error occurred, please contact the staff team.");

                return false;
            }

            final CommandException exception = (CommandException) throwable;
            final MessageType messageType = exception.getMessageType();

            String message = throwable.getMessage();

            if (messageType != null) {
                if (message == null) {
                    message = messageType.getDefault(command);
                }

                context.sendMessage(messageHolder.getReplacing(messageType, message));
                return true;
            }

            targetException.printStackTrace();

            if (targetException.getMessage() != null) {
                context.sendMessage(messageHolder.getReplacing(MessageType.ERROR, targetException.getMessage()));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            context.sendMessage("§cAn internal error occurred, please contact the staff team.");
        }

        return false;
    }

}
