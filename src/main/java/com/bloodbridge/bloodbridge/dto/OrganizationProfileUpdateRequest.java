package com.bloodbridge.bloodbridge.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrganizationProfileUpdateRequest {
    private String orgName;
    private String contactEmail;
    private String contactPhone;
    private String description;
    private String streetAddress;
    private String autoLocationAddress;
    private Double lat;
    private Double lng;
    private String openingTime;
    private String closingTime;
    private List<Integer> workingDays;
    private Integer dailyCapacity;
    private String licenseNumber;
}
