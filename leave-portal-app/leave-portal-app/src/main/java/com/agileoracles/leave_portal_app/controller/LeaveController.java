package com.agileoracles.leave_portal_app.controller;

import com.agileoracles.leave_portal_app.model.LeaveCategory;
import com.agileoracles.leave_portal_app.model.LeaveResponse;
import com.agileoracles.leave_portal_app.model.LeaveUploadRecord;
import com.agileoracles.leave_portal_app.repository.LeaveUploadRepository;
import com.agileoracles.leave_portal_app.service.CategorizationService;
import com.agileoracles.leave_portal_app.service.OciStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final CategorizationService categorizationService;
    private final OciStorageService ociStorageService;
    private final LeaveUploadRepository leaveUploadRepository;

    public LeaveController(CategorizationService categorizationService,
                           OciStorageService ociStorageService,
                           LeaveUploadRepository leaveUploadRepository) {
        this.categorizationService = categorizationService;
        this.ociStorageService = ociStorageService;
        this.leaveUploadRepository = leaveUploadRepository;
    }

    @GetMapping("/status")
    public String status(@AuthenticationPrincipal OAuth2User user) {
        return "Logged in as: " + emailOf(user);
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

        String email = emailOf(user);

        LeaveResponse response = new LeaveResponse(
                email,
                fileName,
                category,
                matchedKeywords,
                Instant.now(),
                ociResult.get("objectName"),
                ociResult.get("objectId"));

        leaveUploadRepository.save(buildRecord(email, fileName, category, ociResult));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/records")
    public List<LeaveUploadRecord> records(@AuthenticationPrincipal OAuth2User user) {
        return leaveUploadRepository.findByUserEmail(emailOf(user));
    }

    @GetMapping(value = "/files/{objectName}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> viewFile(@PathVariable String objectName) {
        byte[] content = ociStorageService.downloadFile(objectName);
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + objectName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    private String emailOf(OAuth2User user) {
        return user != null ? user.getAttribute("email") : "unknown";
    }

    private LeaveUploadRecord buildRecord(String email, String fileName,
                                          LeaveCategory category, Map<String, String> ociResult) {
        LeaveUploadRecord record = new LeaveUploadRecord();
        record.setUserEmail(email);
        record.setAttachedFilename(fileName);
        record.setLeaveCategory(category);
        record.setCreatedAt(Instant.now());
        record.setOciObjectName(ociResult.get("objectName"));
        record.setOciObjectId(ociResult.get("objectId"));
        record.setOciBucketName(ociResult.get("bucketName"));
        return record;
    }
}
