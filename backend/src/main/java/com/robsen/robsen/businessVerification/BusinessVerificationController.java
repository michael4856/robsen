package com.robsen.robsen.businessVerification;

import java.io.IOException;

import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.robsen.robsen.businessVerification.BusinessVerificationService.ReviewDecision;
import com.robsen.robsen.businessVerificationDocument.BusinessVerificationDocument.DocumentType;
import com.robsen.robsen.common.VerificationStatus;

@RestController
@RequestMapping("/api/business-verifications")
public class BusinessVerificationController {
    private final BusinessVerificationService service;
    public BusinessVerificationController(BusinessVerificationService service) { this.service = service; }

    @GetMapping("/business/{businessId}")
    public BusinessVerificationService.VerificationView get(@PathVariable Long businessId) {
        return service.getForBusiness(businessId);
    }
    @PostMapping(value = "/business/{businessId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BusinessVerificationService.DocumentView upload(@PathVariable Long businessId,
            @RequestParam DocumentType type, @RequestPart("file") MultipartFile file) {
        return service.upload(businessId, type, file);
    }
    @PostMapping("/business/{businessId}/submit")
    public BusinessVerificationService.VerificationView submit(@PathVariable Long businessId) {
        return service.submit(businessId);
    }
    @GetMapping("/admin")
    public Object list(@RequestParam(required = false) VerificationStatus status,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(status, q, page, size); }
    @GetMapping("/admin/{id}")
    public BusinessVerificationService.VerificationView getAdmin(@PathVariable Long id) { return service.getForAdmin(id); }
    @PostMapping("/admin/{id}/review")
    public BusinessVerificationService.VerificationView review(@PathVariable Long id, @RequestBody ReviewDecision decision) {
        return service.review(id, decision);
    }
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<PathResource> document(@PathVariable Long documentId) throws IOException {
        var document = service.getDocument(documentId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.fileName().replace("\"", "") + "\"")
                .body(new PathResource(document.path()));
    }
}
