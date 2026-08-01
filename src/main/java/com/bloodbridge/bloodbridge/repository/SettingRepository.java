package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    List<Setting> findByGroupName(String groupName);
    Optional<Setting> findByGroupNameAndName(String groupName, String name);
}
