package pl.goeuropa.station.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.goeuropa.station.service.HmacVerificationService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VerifyController.class)
@AutoConfigureMockMvc(addFilters = false)
class VerifyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private HmacVerificationService service;

    @Test
    void verify_returns200_whenServiceAccepts() throws Exception {
        when(service.verify("sig", "1700000000000")).thenReturn(true);

        mvc.perform(get("/verify-signature")
                        .header("x-signature", "sig")
                        .header("x-timestamp", "1700000000000"))
                .andExpect(status().isOk());

        verify(service).verify("sig", "1700000000000");
    }

    @Test
    void verify_returns401_whenServiceRejects() throws Exception {
        when(service.verify("bad", "1700000000000")).thenReturn(false);

        mvc.perform(get("/verify-signature")
                        .header("x-signature", "bad")
                        .header("x-timestamp", "1700000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_returns401_whenHeadersMissing() throws Exception {
        when(service.verify(null, null)).thenReturn(false);

        mvc.perform(get("/verify-signature"))
                .andExpect(status().isUnauthorized());

        verify(service).verify(null, null);
    }
}
