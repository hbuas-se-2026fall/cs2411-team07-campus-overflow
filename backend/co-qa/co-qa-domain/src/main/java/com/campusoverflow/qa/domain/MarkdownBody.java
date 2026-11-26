package com.campusoverflow.qa.domain;

public class MarkdownBody {
    private final String raw;

    private MarkdownBody(String raw) {
        this.raw = raw;
    }

    public static MarkdownBody ofQuestion(String text) {
        return new MarkdownBody(text);
    }

    public static MarkdownBody ofAnswer(String text) {
        return new MarkdownBody(text);
    }

    public String raw() {
        return raw;
    }
}