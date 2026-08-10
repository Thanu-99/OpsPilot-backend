package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.InventoryRequestDto;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;

import java.util.List;

public interface InventoryService {

    InventoryResponseDto createInventory(InventoryRequestDto request);

    List<InventoryResponseDto> getAllInventory();

    InventoryResponseDto getInventoryById(Long id);

    InventoryResponseDto updateInventory(Long id, InventoryRequestDto request);

    void deleteInventory(Long id);
}