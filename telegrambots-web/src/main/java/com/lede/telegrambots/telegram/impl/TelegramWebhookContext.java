package com.lede.telegrambots.telegram.impl;

import com.lede.telegrambots.domain.bot.ManagedBot;
import com.lede.telegrambots.telegram.command.BotCommand;
import com.lede.telegrambots.telegram.dto.Update;

/**
 * Context object holding input parameters and intermediate/shared data throughout
 * the Telegram webhook processing pipeline.
 */
public class TelegramWebhookContext {
    private final String botUsername;
    private final String secret;
    private final Update update;

    // Shared data mutated by steps
    private ManagedBot bot;
    private Update.Message message;
    private String commandKey;
    private String commandArg;
    private long chatId;
    private BotCommand command;
    private String replyHtml;

    public TelegramWebhookContext(String botUsername, String secret, Update update) {
        this.botUsername = botUsername;
        this.secret = secret;
        this.update = update;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getSecret() {
        return secret;
    }

    public Update getUpdate() {
        return update;
    }

    public ManagedBot getBot() {
        return bot;
    }

    public void setBot(ManagedBot bot) {
        this.bot = bot;
    }

    public Update.Message getMessage() {
        return message;
    }

    public void setMessage(Update.Message message) {
        this.message = message;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public void setCommandKey(String commandKey) {
        this.commandKey = commandKey;
    }

    public String getCommandArg() {
        return commandArg;
    }

    public void setCommandArg(String commandArg) {
        this.commandArg = commandArg;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public BotCommand getCommand() {
        return command;
    }

    public void setCommand(BotCommand command) {
        this.command = command;
    }

    public String getReplyHtml() {
        return replyHtml;
    }

    public void setReplyHtml(String replyHtml) {
        this.replyHtml = replyHtml;
    }
}
