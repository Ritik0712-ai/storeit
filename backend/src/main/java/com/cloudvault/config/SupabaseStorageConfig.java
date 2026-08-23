package com.cloudvault.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.supabase")
@Getter
@Setter
public class SupabaseStorageConfig {
    private String url;
    private String serviceRoleKey;
    private String storageBucket;
}
