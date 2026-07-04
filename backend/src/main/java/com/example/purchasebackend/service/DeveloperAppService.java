package com.example.purchasebackend.service;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.repository.DeveloperAppRepository;
import org.springframework.stereotype.Service;

/** Lookups for developer apps (used by internal endpoints that receive a developerAppId directly). */
@Service
public class DeveloperAppService {

    private final DeveloperAppRepository developerAppRepository;

    public DeveloperAppService(DeveloperAppRepository developerAppRepository) {
        this.developerAppRepository = developerAppRepository;
    }

    public DeveloperApp getById(String developerAppId) {
        return developerAppRepository.findById(developerAppId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "Unknown developerAppId: " + developerAppId));
    }
}
