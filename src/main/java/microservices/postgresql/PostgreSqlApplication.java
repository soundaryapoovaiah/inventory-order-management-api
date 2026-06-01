package microservices.postgresql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PostgreSqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostgreSqlApplication.class, args);
    }

}
