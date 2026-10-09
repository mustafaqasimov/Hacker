package az.hacktrain.app;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages = "az.hacktrain")
@EntityScan("az.hacktrain")
@EnableJpaRepositories("az.hacktrain")
@EnableScheduling
public class HackTrainApplication {
    public static void main(String[] args) { SpringApplication.run(HackTrainApplication.class, args); }
}
