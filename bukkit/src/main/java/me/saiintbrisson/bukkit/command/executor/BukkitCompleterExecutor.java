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

import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.exception.CommandException;
import me.saiintbrisson.minecraft.command.executor.CompleterExecutor;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author SaiintBrisson (https://github.com/SaiintBrisson)
 */
public final class BukkitCompleterExecutor implements CompleterExecutor<CommandSender> {

    private final Method method;
    private final Object holder;

    public BukkitCompleterExecutor(Method method, Object holder) {
        final Class<?> returnType = method.getReturnType();
        final Class<?>[] parameters = method.getParameterTypes();

        if (!List.class.isAssignableFrom(returnType)) {
            throw new CommandException("Illegal return type, '" + method.getName());
        }

        if (parameters.length > 1 || (parameters.length == 1 && !Context.class.isAssignableFrom(parameters[0]))) {
            throw new CommandException("Illegal parameter type, '" + method.getName());
        }

        this.method = method;
        this.holder = holder;
    }

    @Override @SuppressWarnings("unchecked")
    public List<String> execute(Context<CommandSender> context) {
        final Class<?>[] types = method.getParameterTypes();
        try {
            if (types.length == 0) {
                return (List<String>) method.invoke(holder);
            }

            if (types.length == 1 && types[0] == Context.class) {
                return (List<String>) method.invoke(holder, context);
            }

            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

}
