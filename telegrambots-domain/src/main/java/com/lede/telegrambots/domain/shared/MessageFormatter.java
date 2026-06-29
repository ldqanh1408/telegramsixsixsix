package com.lede.telegrambots.domain.shared;

/**
 * Telegram HTML formatting helpers, shared by command replies and GitHub event rendering.
 * Pure string utility, kept in the domain layer so no feature package has to own it.
 */
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
