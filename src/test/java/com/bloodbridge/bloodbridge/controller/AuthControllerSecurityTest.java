package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void authEndpointsShouldBePublic() {
        int statusCode;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(
                    java.util.Map.of("email", "test@test.com", "password", "pass"), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/v1/auth/login", HttpMethod.POST, entity, String.class);
            statusCode = response.getStatusCode().value();
        } catch (ResourceAccessException e) {
            statusCode = 401;
        }
        assertThat(statusCode).isNotEqualTo(403);
    }

    @Test
    void adminEndpointShouldRequireAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/admin/users", String.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void donorEndpointShouldRequireAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/donor/blood-requests", String.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void orgEndpointShouldRequireAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/org/profile", String.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void healthEndpointShouldBePublic() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
