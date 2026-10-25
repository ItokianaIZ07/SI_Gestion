package com.si.rh.controller;

import com.si.rh.dto.ManagerDashboardResponse;
import com.si.rh.service.ManagerDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/dashboard")
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    public ManagerDashboardController(ManagerDashboardService managerDashboardService) {
        this.managerDashboardService = managerDashboardService;
    }

    @GetMapping
    public ManagerDashboardResponse getDashboard(Authentication authentication) {
        return managerDashboardService.getDashboard(authentication.getName());
    }
}
