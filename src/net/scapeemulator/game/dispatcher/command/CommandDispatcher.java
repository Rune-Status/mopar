package net.scapeemulator.game.dispatcher.command;

import java.util.HashMap;
import java.util.Map;

import net.scapeemulator.game.GameServer;
import net.scapeemulator.game.model.player.Player;

public final class CommandDispatcher {

    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public void bind(CommandHandler handler) {
        handlers.put(handler.getName(), handler);
    }

    public void unbindAll() {
        handlers.clear();
    }

    public void handle(Player player, String command) {
        if (player.actionsBlocked()) {
            return;
        }
        String[] parts = command.split(" ");

        String name = parts[0].toLowerCase();
        String[] arguments = new String[parts.length - 1];
        System.arraycopy(parts, 1, arguments, 0, arguments.length);

        CommandHandler handler = handlers.get(name);
        if (handler != null) {
            handler.handle(player, arguments);
        }
    }

    public static CommandDispatcher getInstance() {
        return GameServer.getInstance().getMessageDispatcher().getCommandDispatcher();
    }
}
