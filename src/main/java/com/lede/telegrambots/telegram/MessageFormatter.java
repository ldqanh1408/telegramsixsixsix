package com.lede.telegrambots.telegram;

public final class MessageFormatter {

    private MessageFormatter() {}

    /** Escape special chars for Telegram HTML parse mode. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String link(String text, String url) {
        return "<a href=\"" + esc(url) + "\">" + esc(text) + "</a>";
    }

    public static String bold(String text) {
        return "<b>" + esc(text) + "</b>";
    }

    public static String code(String text) {
        return "<code>" + esc(text) + "</code>";
    }
}
