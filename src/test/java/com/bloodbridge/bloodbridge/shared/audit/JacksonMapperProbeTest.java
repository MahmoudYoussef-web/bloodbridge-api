package com.bloodbridge.bloodbridge.shared.audit;

import com.bloodbridge.bloodbridge.dto.AchievementView;
import com.bloodbridge.bloodbridge.dto.DonorAchievementView;
import com.bloodbridge.bloodbridge.dto.DonorAchievementsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Import(JacksonMapperProbeTest.ProbeWebConfig.class)
class JacksonMapperProbeTest {

    @TestConfiguration
    static class ProbeWebConfig {
        @RestController
        static class ProbeController {
            @GetMapping("/v1/public/probe/achievements")
            DonorAchievementsResponse response() {
                AchievementView ach = new AchievementView(
                        1L, "{\"en\":\"First Donation\",\"ar\":\"تبرع أول\"}",
                        "{\"en\":\"Donated once\"}", 10, "bronze.png", "bronze", "donations", 1, 1);
                DonorAchievementView dav = new DonorAchievementView(2L, 1L, 6L, Instant.now().toString(), ach);
                return new DonorAchievementsResponse(List.of(dav), List.of(ach), 10, 1);
            }
        }
    }

    @Autowired
    ObjectMapper liveMapper;

    @Autowired
    MockMvc mockMvc;

    private AchievementView ach() {
        return new AchievementView(
                1L, "{\"en\":\"First Donation\",\"ar\":\"تبرع أول\"}",
                "{\"en\":\"Donated once\"}", 10, "bronze.png", "bronze", "donations", 1, 1);
    }

    @Test
    void springBuilderMapper_keepsRawValue() throws Exception {
        String json = Jackson2ObjectMapperBuilder.json().build().writeValueAsString(ach());
        System.out.println("[PROBE] springBuilderMapper = " + json);
        assertTrue(json.contains("\"name\":{\"en\""), "springBuilder must emit raw object: " + json);
    }

    @Test
    void liveContextMapper_keepsRawValue() throws Exception {
        String json = liveMapper.writeValueAsString(ach());
        System.out.println("[PROBE] liveContextMapper = " + json);
        assertTrue(json.contains("\"name\":{\"en\""), "liveContextMapper must emit raw object: " + json);
    }

    @Test
    void liveContextMapper_nested_keepsRawValue() throws Exception {
        DonorAchievementView dav = new DonorAchievementView(2L, 1L, 6L, Instant.now().toString(), ach());
        String json = liveMapper.writeValueAsString(
                new DonorAchievementsResponse(List.of(dav), List.of(ach()), 10, 1));
        System.out.println("[PROBE] liveContextMapperNested = " + json);
        assertTrue(json.contains("\"name\":{\"en\""), "nested response must emit raw object: " + json);
    }

    @Test
    void mvcStack_keepsRawValue() throws Exception {
        String json = mockMvc.perform(get("/v1/public/probe/achievements"))
                .andReturn().getResponse().getContentAsString();
        System.out.println("[PROBE] mvcStack = " + json);
        assertTrue(json.contains("\"name\":{\"en\""), "MVC stack must emit raw object: " + json);
    }
}
