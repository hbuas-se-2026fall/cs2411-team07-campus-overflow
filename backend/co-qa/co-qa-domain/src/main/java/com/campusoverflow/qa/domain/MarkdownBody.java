package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.Guard;

/** Markdown 正文。只保存原文；渲染与 XSS 净化在基础设施层完成。 */
public record MarkdownBody(String value) {

    public static MarkdownBody ofQuestion(String raw) {
        // 问题正文：非空，20 ~ 30000码点（实验文档）
        Guard.requireText(raw, 20, 30000, "QA-1002", "问题正文长度必须在2‑30000字符之间");
        return new MarkdownBody(raw);
    }

    public static MarkdownBody ofAnswer(String raw) {
        // 回答正文：非空，10 ~ 30000码点
        Guard.requireText(raw, 10, 30000, "QA-1002", "回答正文长度必须在10‑30000字符之间");
        return new MarkdownBody(raw);
    }
}
