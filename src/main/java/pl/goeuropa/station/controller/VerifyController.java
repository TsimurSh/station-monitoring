package pl.goeuropa.station.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import pl.goeuropa.station.service.HmacVerificationService;

@Slf4j
@RestController
@RequiredArgsConstructor
@SecurityRequirements
public class VerifyController {

    private final HmacVerificationService hmacVerificationService;

    @GetMapping("/verify-signature")
    @Operation(summary = "Validate x-signature/x-timestamp headers for Apache2 sub-request auth")
    public ResponseEntity<Void> verify(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-timestamp", required = false) String timestamp) {

        if (hmacVerificationService.verify(signature, timestamp)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
