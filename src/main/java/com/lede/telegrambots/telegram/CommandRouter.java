package com.lede.telegrambots.telegram;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import com.lede.telegrambots.telegram.command.BotCommand;
import com.lede.telegrambots.telegram.command.CommandContext;
import com.lede.telegrambots.telegram.dto.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Parses a Telegram {@link Update} and dispatches to the matching {@link BotCommand}.
 *
 * <p>Commands are discovered via Spring DI. The router itself is bot-agnostic: the
 * caller (the webhook controller) hands it the {@link ManagedBot} that owned the URL.</p>
 */
@Component
public class CommandRouter {

    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

    private final Map<String, BotCommand> commands;

    public CommandRouter(List<BotCommand> commands) {
        this.commands = commands.stream()
                .collect(Collectors.toUnmodifiableMap(BotCommand::name, Function.identity()));
        log.info("Registered {} bot command(s): {}", this.commands.size(), this.commands.keySet());
    }

    public void handle(ManagedBot bot, Update update) {
        Update.Message msg = update.message();
        if (msg == null || msg.text() == null) return;

        String text = msg.text().trim();
        if (!text.startsWith("/")) return;

        String[] parts = text.split("\\s+", 2);
        String key = stripBotSuffix(parts[0]);
        String arg = parts.length > 1 ? parts[1] : "";

        BotCommand cmd = commands.get(key);
        if (cmd == null) {
            log.debug("Unknown command on @{}: {}", bot.username(), key);
            return;
        }
        try {
            cmd.execute(new CommandContext(msg.chat().id(), arg, bot));
        } catch (Exception e) {
            log.error("Command {} on @{} threw", key, bot.username(), e);
        }
    }

    /** Telegram appends {@code @botname} in groups: {@code /start@my_bot} → {@code /start}. */
    private static String stripBotSuffix(String token) {
        int at = token.indexOf('@');
        return at < 0 ? token : token.substring(0, at);
    }
}
