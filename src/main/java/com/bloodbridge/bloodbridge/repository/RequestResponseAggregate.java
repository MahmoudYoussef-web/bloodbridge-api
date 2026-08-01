package com.bloodbridge.bloodbridge.repository;

public interface RequestResponseAggregate {
    Long getBloodRequestId();
    Long getTotal();
    Long getAcceptedCount();
    Long getCompletedCount();
}
