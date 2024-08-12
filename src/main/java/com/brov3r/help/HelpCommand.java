package com.brov3r.help;

import com.avrix.commands.*;
import com.avrix.enums.CommandScope;
import com.avrix.utils.ChatUtils;
import com.avrix.utils.PlayerUtils;
import zombie.commands.CommandBase;
import zombie.commands.PlayerType;
import zombie.commands.RequiredRight;
import zombie.core.raknet.UdpConnection;

import java.lang.reflect.Field;
import java.util.*;

/**
 * The {@code HelpCommand} class provides additional information about the server and commands available to the players.
 * It allows players to retrieve a list of commands and detailed help for specific commands.
 */
@CommandAccessLevel
@CommandName("help")
@CommandExecutionScope(CommandScope.CHAT)
@CommandDescription("Additional information about the server and commands")
public class HelpCommand extends Command{
    /**
     * A map containing the default commands available on the server.
     * The key is the command name, and the value is the class representing the command.
     */
    private final Map<String, Class<CommandBase>> defaultCommands = new TreeMap<>();

    /**
     * A map containing the custom Avrix commands available on the server.
     * The key is the command name, and the value is the command object.
     */
    private final Map<String, Command> avrixCommands = new TreeMap<>();

    /**
     * Returns the description of the command.
     * If a custom description is provided in the server configuration, it will be used; otherwise, the default description is returned.
     *
     * @return the description of the command
     */
    @Override
    public String getDescription() {
        String description = Main.getInstance().getDefaultConfig().getString("commandDescription");

        return description.isEmpty() ? super.getDescription() : description;
    }

    /**
     * Executes the help command when invoked by a player.
     * It displays either a list of available commands or detailed help for a specific command based on the provided arguments.
     *
     * @param connection the UDP connection of the player (or server) invoking the command
     * @param args       the arguments provided by the player
     * @return null, as the command does not return any specific result
     */
    @Override
    public String onInvoke(UdpConnection connection, String[] args) {
        if (connection == null) return null; // Only player

        loadCacheCommands();

        for (String line : Main.getInstance().getDefaultConfig().getStringList("helpText")) {
            ChatUtils.sendMessageToPlayer(connection, formatLine(line, connection.username));
        }

        if (args.length == 0) sendCommandsList(connection);

        if (args.length > 0) sendCommandHelp(connection, args[0].toLowerCase());

        return null;
    }

    /**
     * Sends detailed help for a specific command to the player.
     * If the player does not have sufficient rights to execute the command, a "no rights" message is sent.
     *
     * @param connection  the UDP connection of the player
     * @param commandName the name of the command for which help is requested
     */
    private void sendCommandHelp(UdpConnection connection, String commandName) {
        String helpText = null;

        if (defaultCommands.containsKey(commandName)) {
            Class<CommandBase> command = defaultCommands.get(commandName);
            if (!canCommandExecute(command, connection)) {
                ChatUtils.sendMessageToPlayer(connection, Main.getInstance().getDefaultConfig().getString("noRights"));
                return;
            }
            helpText = CommandBase.getHelp(command);
        }

        if (avrixCommands.containsKey(commandName)) {
            Command command = avrixCommands.get(commandName);
            if (!canCommandExecute(command, connection)) {
                ChatUtils.sendMessageToPlayer(connection, Main.getInstance().getDefaultConfig().getString("noRights"));
                return;
            }
            helpText = command.getDescription();
        }

        if (helpText == null) {
            ChatUtils.sendMessageToPlayer(connection, Main.getInstance().getDefaultConfig().getString("noCommands"));
            return;
        }

        String returnText = formatLine("<RGB:0.4,0.5,0.8> /" + commandName + " <RGB:1,1,1> <SPACE> - ", "") +
                (helpText.isEmpty() ? "No description" : helpText);

        ChatUtils.sendMessageToPlayer(connection, returnText);

    }

    /**
     * Sends a list of available commands to the player.
     * The list includes both default and Avrix commands, but only those the player has the rights to execute.
     *
     * @param connection the UDP connection of the player
     */
    private void sendCommandsList(UdpConnection connection) {
        ChatUtils.sendMessageToPlayer(connection, formatLine(Main.getInstance().getDefaultConfig().getString("commandsListText"), connection.username));

        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, Class<CommandBase>> entry : defaultCommands.entrySet()) {
           if (!canCommandExecute(entry.getValue(), connection)) continue;

           stringBuilder.append(formatLine("<RGB:0.4,0.5,0.8> /" + entry.getKey() + " <RGB:1,1,1> , <SPACE> <SPACE>", ""));
        }

        for (Map.Entry<String, Command> entry : avrixCommands.entrySet()) {
            if (!canCommandExecute(entry.getValue(), connection)) continue;

            stringBuilder.append(formatLine("<RGB:0.4,0.5,0.8> /" + entry.getKey() + " <RGB:1,1,1> , <SPACE> <SPACE>", ""));
        }

        if (stringBuilder.length() > 2) {
            stringBuilder.setLength(stringBuilder.length() - formatLine(" <RGB:1,1,1> , <SPACE> <SPACE>", "").length());
        }

        ChatUtils.sendMessageToPlayer(connection, stringBuilder.toString());
    }

    /**
     * Checks if the player has the necessary rights to execute a default command.
     *
     * @param command    the class of the command to check
     * @param connection the UDP connection of the player
     * @return true if the player has the rights to execute the command, false otherwise
     */
    private boolean canCommandExecute(Class<CommandBase> command, UdpConnection connection) {
       try {
            RequiredRight requiredRight = command.getAnnotation(RequiredRight.class);

            if (requiredRight == null) return true;
            return (CommandBase.accessLevelToInt(PlayerType.toString(connection.accessLevel)) & requiredRight.requiredRights()) != 0;
        } catch (Exception ignore) {}

        return true;
    }

    /**
     * Checks if the player has the necessary rights to execute a custom Avrix command.
     *
     * @param command    the command to check
     * @param connection the UDP connection of the player
     * @return true if the player has the rights to execute the command, false otherwise
     */
    private boolean canCommandExecute(Command command, UdpConnection connection) {
        return PlayerUtils.getAccessLevel(connection).getPriority() >= command.getAccessLevel().getPriority();
    }

    /**
     * Loads and caches the default and Avrix commands into their respective maps.
     * This method is called only if the maps are empty to avoid reloading commands unnecessarily.
     */
    private void loadCacheCommands() {
        if (!defaultCommands.isEmpty() || !avrixCommands.isEmpty()) return;

        // Default commands
        try {
            Field childrenClassesField = CommandBase.class.getDeclaredField("childrenClasses");
            childrenClassesField.setAccessible(true);
            Class<CommandBase>[] childrenClasses = (Class<CommandBase>[]) childrenClassesField.get(null);

            for (Class<CommandBase> commandClass : childrenClasses) {
                if (commandClass.isAnnotationPresent(zombie.commands.CommandName.class)) {
                    zombie.commands.CommandName annotation = commandClass.getAnnotation(zombie.commands.CommandName.class);

                    if (annotation == null) continue;

                    String name = annotation.name().toLowerCase();

                    if (CommandsManager.getRegisteredCommands().containsKey(name)) continue;

                    defaultCommands.put(name, commandClass);
                }
            }
        } catch (Exception ignore) {}

        // Avrix commands
        avrixCommands.putAll(CommandsManager.getRegisteredCommands());
    }

    /**
     * Formats a line of text to include the player's name and replace spaces with a specific symbol.
     *
     * @param line       the line of text to format
     * @param playerName the name of the player
     * @return the formatted line of text
     */
    public static String formatLine(String line, String playerName) {
        return line.replaceAll("<PLAYER>", playerName).replaceAll("<SPACE>", ChatUtils.SPACE_SYMBOL);
    }
}
