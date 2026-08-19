package com.opspilot.opspilotbackend.controller.manager;

import com.opspilot.opspilotbackend.dto.UserResponseDto;
import com.opspilot.opspilotbackend.service.ManagerWorkspaceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager")
public class ManagerWorkspaceController {

    private final ManagerWorkspaceService managerWorkspaceService;

    public ManagerWorkspaceController(
            ManagerWorkspaceService managerWorkspaceService) {

        this.managerWorkspaceService = managerWorkspaceService;
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('MANAGER')")
    public List<UserResponseDto> getMyTeam(
            Authentication authentication) {

        return managerWorkspaceService.getMyTeam(
                authentication.getName()
        );
    }
}