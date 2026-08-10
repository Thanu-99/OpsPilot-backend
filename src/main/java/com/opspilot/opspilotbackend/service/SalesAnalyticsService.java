package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.SalesAnalyticsDto;

import java.time.LocalDate;
import java.util.List;

public interface SalesAnalyticsService {

    List<SalesAnalyticsDto> getSalesAnalytics(
            LocalDate startDate,
            LocalDate endDate
    );
}