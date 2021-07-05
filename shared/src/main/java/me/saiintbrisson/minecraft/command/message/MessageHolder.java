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

package me.saiintbrisson.minecraft.command.message;

import lombok.Getter;

import java.util.EnumMap;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * The MessageHolder stores the messages for each Error
 * thrown by the command.
 *
 * @author SaiintBrisson (https://github.com/SaiintBrisson)
 * @see MessageType
 */
@Getter
public final class MessageHolder {

    private final EnumMap<MessageType, String> registry;

    {
      registry = new EnumMap<>(MessageType.class);
    }

    public MessageHolder() {
        for (MessageType value : MessageType.values()) {
            registry.put(value, value.getDefMessage());
        }
    }

    public String getMessage(MessageType type) {
        return registry.get(type);
    }

    public String getReplacing(MessageType type, String message) {
        return getMessage(type).replace(type.getPlaceHolder(), message);
    }

    public void setMessage(MessageType type, String message) {
        registry.put(type, message);
    }

    public void loadFromResources(String baseName, Locale locale) {
        final ResourceBundle bundle = PropertyResourceBundle.getBundle(baseName, locale);
        loadFromResources(bundle);
    }

    public void loadFromResources(String baseName) {
        final ResourceBundle bundle = PropertyResourceBundle.getBundle(baseName);
        loadFromResources(bundle);
    }

    public void loadFromResources(ResourceBundle bundle) {
        for (String str : bundle.keySet()) {
            try {
                final MessageType type = MessageType.valueOf(str);
                setMessage(type, bundle.getString(str).replace("&", "§"));
            } catch (IllegalArgumentException ignored) {}
        }
    }

}
