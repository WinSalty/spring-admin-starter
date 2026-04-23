package com.winsalty.quickstart.infra.mail;

/**
 * 邮件模板服务。
 * 统一生成可复用的 HTML 邮件样式和纯文本兜底内容。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public interface MailTemplateService {

    MailTemplateContent renderStandard(StandardMailTemplate template);
}
