package pl.goeuropa.station;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.goeuropa.station.service.HmacVerificationService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HmacVerificationServiceContextSmokeTest {

    @Autowired
    private HmacVerificationService service;

    @Test
    void contextLoadsAndServiceIsWired() {
        assertThat(service).isNotNull();
    }
}
