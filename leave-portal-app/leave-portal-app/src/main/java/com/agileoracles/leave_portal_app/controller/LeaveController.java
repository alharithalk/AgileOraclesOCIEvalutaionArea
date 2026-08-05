package com.agileoracles.leave_portal_app.controller;

import com.agileoracles.leave_portal_app.model.LeaveCategory;
import com.agileoracles.leave_portal_app.model.LeaveResponse;
import com.agileoracles.leave_portal_app.service.CategorizationService;
import com.agileoracles.leave_portal_app.service.OciStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final CategorizationService categorizationService;
    private final OciStorageService ociStorageService;

    public LeaveController(CategorizationService categorizationService, OciStorageService ociStorageService) {
        this.categorizationService = categorizationService;
        this.ociStorageService = ociStorageService;
    }

    @GetMapping("/status")
    public String status(@AuthenticationPrincipal OAuth2User user) {
        String email = user != null ? user.getAttribute("email") : "unknown";
        return "Logged in as: " + email;
    }

    @PostMapping("/upload")
    public ResponseEntity<LeaveResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User user) throws IOException {

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("Only .txt files are allowed");
        }

        byte[] contentBytes = file.getBytes();
        String content = new String(contentBytes, StandardCharsets.UTF_8);

        LeaveCategory category = categorizationService.categorize(content);
        String matchedKeywords = categorizationService.findMatchedKeywords(content);
        Map<String, String> ociResult = ociStorageService.uploadFile(contentBytes, fileName);

        String email = user != null ? user.getAttribute("email") : "unknown";

        LeaveResponse response = new LeaveResponse(
                email,
                fileName,
                category,
                matchedKeywords,
                Instant.now(),
                ociResult.get("objectName"),
                ociResult.get("objectId"));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
