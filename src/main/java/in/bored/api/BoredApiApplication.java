package in.bored.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BoredApiApplication {

    @jakarta.annotation.PostConstruct
    public void init() {
        // Ensure the application runs in IST
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BoredApiApplication.class, args);
    }

}
