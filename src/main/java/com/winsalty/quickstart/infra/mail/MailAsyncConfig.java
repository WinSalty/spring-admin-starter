package com.winsalty.quickstart.infra.mail;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 邮件异步发送线程池配置。
 * 使用有界队列隔离 SMTP 慢调用，避免匿名邮件入口长期占用 Web 请求线程。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Configuration
public class MailAsyncConfig {

    private static final String THREAD_NAME_PREFIX = "mail-send-";

    /**
     * 创建邮件发送线程池。
     *
     * @param mailProperties 邮件配置
     * @return 邮件发送任务执行器
     * @author sunshengxian
     * @date 2026-04-24
     */
    @Bean
    @Qualifier("mailTaskExecutor")
    public Executor mailTaskExecutor(MailProperties mailProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(mailProperties.getAsync().getCorePoolSize());
        executor.setMaxPoolSize(mailProperties.getAsync().getMaxPoolSize());
        executor.setQueueCapacity(mailProperties.getAsync().getQueueCapacity());
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(mailProperties.getAsync().getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }
}
