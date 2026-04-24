package com.winsalty.quickstart.infra.mail;

/**
 * 标准邮件模板模型。
 * 封装统一样式模板需要的标题、正文、强调信息和行动按钮。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public class StandardMailTemplate {

    private String title;
    private String greeting;
    private String summary;
    private String highlightLabel;
    private String highlightValue;
    private String description;
    private String actionText;
    private String actionUrl;
    private String actionFallbackTip;
    private String footerNote;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getHighlightLabel() {
        return highlightLabel;
    }

    public void setHighlightLabel(String highlightLabel) {
        this.highlightLabel = highlightLabel;
    }

    public String getHighlightValue() {
        return highlightValue;
    }

    public void setHighlightValue(String highlightValue) {
        this.highlightValue = highlightValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getActionFallbackTip() {
        return actionFallbackTip;
    }

    public void setActionFallbackTip(String actionFallbackTip) {
        this.actionFallbackTip = actionFallbackTip;
    }

    public String getFooterNote() {
        return footerNote;
    }

    public void setFooterNote(String footerNote) {
        this.footerNote = footerNote;
    }
}
