package com.example.shorturl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Application application = new Application();
    private Server server = new Server();
    private ShortUrl shortUrl = new ShortUrl();
    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Security security = new Security();
    private Pagination pagination = new Pagination();
    private Dashboard dashboard = new Dashboard();

    @Data
    public static class Application {
        private String name;
    }

    @Data
    public static class Server {
        private int port;
    }

    @Data
    public static class ShortUrl {
        private String domain;
        private int keyLength;
        private int cacheExpireDays;
    }

    @Data
    public static class Jwt {
        private String secret;
        private long expiration;
    }

    @Data
    public static class Admin {
        private OnlineUser onlineUser = new OnlineUser();

        @Data
        public static class OnlineUser {
            private long ttlSeconds;
        }
    }

    @Data
    public static class Security {
        private Cors cors = new Cors();

        @Data
        public static class Cors {
            private List<String> allowedOrigins;
            private List<String> allowedMethods;
            private List<String> allowedHeaders;
            private List<String> exposedHeaders;
            private long maxAge;
        }
    }

    @Data
    public static class Pagination {
        private int defaultPage;
        private int defaultSize;
        private int maxSize;
    }

    @Data
    public static class Dashboard {
        private int defaultTrendDays;
    }
}
