package com.bookcover.archive.controller.admin;

import com.bookcover.archive.service.ImageInspectionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/admin/image-inspection")
@PreAuthorize("hasRole('ADMIN')")
public class AdminImageInspectionController {
    private final ImageInspectionService service;
    public AdminImageInspectionController(ImageInspectionService service) { this.service = service; }
    public record ScanRequest(UUID after, int limit) {}
    @PostMapping("/scan")
    public ImageInspectionService.ScanResult scan(@RequestBody ScanRequest request) { return service.scan(request.after(), request.limit()); }
    @GetMapping("/matches")
    public ImageInspectionService.Results matches(@RequestParam(required = false) UUID bookId,
            @RequestParam(defaultValue = "") String kind, @RequestParam(defaultValue = "0") int page) { return service.matches(bookId, kind, page); }
}
