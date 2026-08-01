package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.entity.Setting;
import com.bloodbridge.bloodbridge.repository.SettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, JsonNode>> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadCache() {
        cache.clear();
        try {
            for (Setting setting : settingRepository.findAll()) {
                try {
                    JsonNode payload = objectMapper.readTree(setting.getPayload());
                    cache.computeIfAbsent(setting.getGroupName(), k -> new ConcurrentHashMap<>())
                            .put(setting.getName(), payload);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse settings payload for {}.{}", setting.getGroupName(), setting.getName());
                }
            }
            log.info("Loaded {} settings groups into cache", cache.size());
        } catch (Exception e) {
            log.warn("Could not load settings (table may not exist yet): {}", e.getMessage());
        }
    }

    public JsonNode get(String group, String name) {
        Map<String, JsonNode> groupCache = cache.get(group);
        if (groupCache == null) return null;
        return groupCache.get(name);
    }

    public String getString(String group, String name, String defaultValue) {
        JsonNode node = get(group, name);
        if (node == null) return defaultValue;
        return node.asText(defaultValue);
    }

    public int getInt(String group, String name, int defaultValue) {
        JsonNode node = get(group, name);
        if (node == null) return defaultValue;
        return node.asInt(defaultValue);
    }

    public boolean getBoolean(String group, String name, boolean defaultValue) {
        JsonNode node = get(group, name);
        if (node == null) return defaultValue;
        return node.asBoolean(defaultValue);
    }

    public double getDouble(String group, String name, double defaultValue) {
        JsonNode node = get(group, name);
        if (node == null) return defaultValue;
        return node.asDouble(defaultValue);
    }

    public <T> T getObject(String group, String name, Class<T> type) throws JsonProcessingException {
        JsonNode node = get(group, name);
        if (node == null) return null;
        return objectMapper.treeToValue(node, type);
    }

    public void update(String group, String name, Object value) {
        try {
            String payload = objectMapper.writeValueAsString(value);
            Setting setting = settingRepository.findByGroupNameAndName(group, name)
                    .orElse(Setting.builder()
                            .groupName(group)
                            .name(name)
                            .build());
            setting.setPayload(payload);
            settingRepository.save(setting);
            cache.computeIfAbsent(group, k -> new ConcurrentHashMap<>())
                    .put(name, objectMapper.readTree(payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize settings payload", e);
        }
    }

    public Map<String, String> getAllByGroup(String group) {
        Map<String, String> result = new HashMap<>();
        Map<String, JsonNode> groupCache = cache.get(group);
        if (groupCache == null) return result;
        for (Map.Entry<String, JsonNode> entry : groupCache.entrySet()) {
            result.put(entry.getKey(), entry.getValue().asText());
        }
        return result;
    }
}
