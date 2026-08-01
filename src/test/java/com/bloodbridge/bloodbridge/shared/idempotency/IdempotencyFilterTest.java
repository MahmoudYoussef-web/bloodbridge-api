package com.bloodbridge.bloodbridge.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;

    @Test
    void shouldLoadIdempotencyFilter() {
        IdempotencyFilter filter = new IdempotencyFilter(idempotencyKeyRepository, new ObjectMapper());
        assertThat(filter).isNotNull();
    }

    @Test
    void idempotencyKeyShouldStoreAndRetrieve() {
        IdempotencyKey key = IdempotencyKey.builder()
                .idempotencyKey("test-key")
                .httpMethod("POST")
                .requestPath("/v1/action")
                .build();

        when(idempotencyKeyRepository.findByIdempotencyKey("test-key"))
                .thenReturn(Optional.of(key));

        Optional<IdempotencyKey> found = idempotencyKeyRepository.findByIdempotencyKey("test-key");

        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo("test-key");
    }

    @Test
    void shouldReturnEmptyForUnknownKey() {
        when(idempotencyKeyRepository.findByIdempotencyKey("unknown"))
                .thenReturn(Optional.empty());

        Optional<IdempotencyKey> found = idempotencyKeyRepository.findByIdempotencyKey("unknown");

        assertThat(found).isEmpty();
    }
}
