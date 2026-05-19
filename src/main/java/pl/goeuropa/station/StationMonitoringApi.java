package pl.goeuropa.station;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StationMonitoringApi {

    public static void main(String[] args) {
        SpringApplication.run(StationMonitoringApi.class, args);

        System.out.println(" --- Application started successfully! \n 👉 OpenApi Documentation: http://localhost:8080/swagger-ui/index.html");
    }
}
