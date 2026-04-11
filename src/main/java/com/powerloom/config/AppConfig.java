package com.powerloom.config;

import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application Configuration Class
 * Central place for custom Spring Beans and Configurations
 */
@Configuration
@EnableAsync
public class AppConfig {

    /**
     * You can define any custom beans here if needed in future.
     * Currently using @ComponentScan in main class, so most beans are auto-detected.
     */

    @Bean
    @Primary
    public String applicationName() {
        return "PL GST Reconciliation Tool";
    }

    @Bean
    public FxWeaver fxWeaver(ConfigurableApplicationContext context) {
        return new SpringFxWeaver(context);
    }

    /**
     * Optional: Custom Thread Pool for background tasks (e.g., large Excel processing)
     */
    /*
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("GST-Recon-");
        executor.initialize();
        return executor;
    }
    */
}