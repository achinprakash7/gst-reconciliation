package com.powerloom.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // FXMLLoader bean intentionally removed — it is stateful.
    // StageInitializer creates a fresh FXMLLoader directly.

    // TaskExecutor remains if you want a named thread pool:
    // @Bean
    // public TaskExecutor taskExecutor() {
    //     SimpleAsyncTaskExecutor ex = new SimpleAsyncTaskExecutor("gst-recon-");
    //     ex.setConcurrencyLimit(2);
    //     return ex;
    // }
}