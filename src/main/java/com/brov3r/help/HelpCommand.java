package com.brov3r.help;

import com.avrix.commands.*;
import com.avrix.enums.CommandScope;
import com.avrix.utils.ChatUtils;
import zombie.commands.CommandBase;
import zombie.core.raknet.UdpConnection;

import java.lang.reflect.Field;
import java.util.*;

@CommandAccessLevel
@CommandChatReturn
@CommandName("help")
@CommandExecutionScope(CommandScope.CHAT)
@CommandDescription("Additional information about the server and commands")
public class HelpCommand extends Command{
    private static Set<String> cachedCommandsNames = null;

    /**
     * Calling a command
     *
     * @param udpConnection player/server connection
     * @param args arguments
     */
    @Override
    public void onInvoke(UdpConnection udpConnection, String[] args) {
        if (udpConnection == null) return;

        for (String line : Main.getInstance().getDefaultConfig().getStringList("helpText")) {
            ChatUtils.sendMessageToPlayer(udpConnection, formatLine(line, udpConnection.username));
        }

        ChatUtils.sendMessageToPlayer(udpConnection, formatLine(Main.getInstance().getDefaultConfig().getString("commandsListText"), udpConnection.username));

        Set<String> commandsName = getCachedCommandsNames();

        StringBuilder stringBuilder = new StringBuilder();
        for (String commandName : commandsName) {
            stringBuilder.append(formatLine("<RGB:0.4,0.5,0.8> /" + commandName + " <RGB:1,1,1> , <SPACE> <SPACE>", ""));
        }

        if (stringBuilder.length() > 2) {
            stringBuilder.setLength(stringBuilder.length() - formatLine(" <RGB:1,1,1> , <SPACE> <SPACE>", "").length());
        }

        ChatUtils.sendMessageToPlayer(udpConnection, stringBuilder.toString());
    }

    /**
     * Getting a list of available commands
     *
     * @return set of commands
     */
    private static Set<String> getCachedCommandsNames() {
        if (cachedCommandsNames == null) {
            cachedCommandsNames = new HashSet<>();
            try {
                Field childrenClassesField = CommandBase.class.getDeclaredField("childrenClasses");
                childrenClassesField.setAccessible(true);
                Class<?>[] childrenClasses = (Class<?>[]) childrenClassesField.get(null);

                for (Class<?> commandClass : childrenClasses) {
                    if (commandClass.isAnnotationPresent(zombie.commands.CommandName.class)) {
                        zombie.commands.CommandName annotation = commandClass.getAnnotation(zombie.commands.CommandName.class);
                        cachedCommandsNames.add(annotation.name());
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException ignore) {}


            cachedCommandsNames.addAll(CommandsManager.getRegisteredCommands().keySet());
        }

        return cachedCommandsNames;
    }

    /**
     * Formats a line of the MOTD (Message of the Day) to include the player's name.
     *
     * @param line The line of the MOTD to format.
     * @param playerName The name of the player.
     * @return The formatted line.
     */
    public static String formatLine(String line, String playerName) {
        return line.replaceAll("<PLAYER>", playerName).replaceAll("<SPACE>", ChatUtils.SPACE_SYMBOL);
    }
}
