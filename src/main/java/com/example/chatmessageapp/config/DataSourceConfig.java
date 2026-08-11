package com.example.chatmessageapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${app.datasource.write.url:jdbc:mysql://localhost:3306/chat_app?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true}")
    private String url;

    @Value("${app.datasource.write.username:root}")
    private String username;

    @Value("${app.datasource.write.password:root}")
    private String password;

    @Bean
    public DataSource dataSource() {
        // Ensure createDatabaseIfNotExist=true is present in connection URL
        String finalUrl = url;
        if (!finalUrl.contains("createDatabaseIfNotExist=true")) {
            String separator = finalUrl.contains("?") ? "&" : "?";
            finalUrl = finalUrl + separator + "createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        }

        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(finalUrl)
                .username(username)
                .password(password)
                .build();
    }
}
