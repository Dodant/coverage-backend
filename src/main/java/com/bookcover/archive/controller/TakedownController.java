package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.admin.TakedownResponse;
import com.bookcover.archive.domain.takedown.TakedownRequest;
import com.bookcover.archive.service.RateLimitAction;
import com.bookcover.archive.service.RateLimitService;
import com.bookcover.archive.service.TakedownService;
import com.bookcover.archive.validation.annotation.ValidUrl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 저작권 삭제 요청 접수 API.
 * 권리자(출판사/대리인)는 회원이 아닐 수 있으므로 인증 없이 접수합니다.
 *
 * Requirements: 17.1, 17.2 - 삭제 요청 접수 폼, 요청자/대상/사유/증빙 기록
 */
@RestController
@RequestMapping("/takedowns")
@Tag(name = "Takedowns", description = "저작권 삭제 요청 API")
public class TakedownController {

    private final TakedownService takedownService;
    private final RateLimitService rateLimitService;

    public TakedownController(TakedownService takedownService, RateLimitService rateLimitService) {
        this.takedownService = takedownService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    @Operation(summary = "저작권 삭제 요청", description = "권리자가 저작물에 대한 삭제를 요청합니다. 로그인이 필요 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "요청 접수"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "429", description = "요청 횟수 초과")
    })
    public ResponseEntity<TakedownResponse> createRequest(
            @Valid @RequestBody CreateTakedownRequest request,
            HttpServletRequest httpRequest) {
        // 익명 접수라 사용자 대신 IP 단위로 신고와 같은 한도(시간당 5회)를 적용
        // ponytail: getRemoteAddr is the proxy IP behind a load balancer; set server.forward-headers-strategy then
        UUID ipKey = UUID.nameUUIDFromBytes(("takedown:" + httpRequest.getRemoteAddr()).getBytes(StandardCharsets.UTF_8));
        rateLimitService.checkAndRecord(ipKey, RateLimitAction.REPORT);

        TakedownRequest created = takedownService.createRequest(
                request.requesterName(),
                request.requesterEmail(),
                request.requesterOrganization(),
                request.targetUrl(),
                request.targetId(),
                request.reason(),
                request.evidenceUrl(),
                request.evidenceDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TakedownResponse.from(created));
    }

    public record CreateTakedownRequest(
            @NotBlank(message = "요청자 이름은 필수입니다")
            @Size(max = 255)
            String requesterName,

            @NotBlank(message = "요청자 이메일은 필수입니다")
            @Email(message = "이메일 형식이 올바르지 않습니다")
            @Size(max = 255)
            String requesterEmail,

            @Size(max = 255)
            String requesterOrganization,

            @NotBlank(message = "대상 URL은 필수입니다")
            @ValidUrl(optional = false)
            @Size(max = 255)
            String targetUrl,

            UUID targetId,

            @NotBlank(message = "삭제 요청 사유는 필수입니다")
            @Size(max = 2000, message = "사유는 2000자 이하여야 합니다")
            String reason,

            @ValidUrl
            @Size(max = 255)
            String evidenceUrl,

            @Size(max = 1000, message = "증빙 설명은 1000자 이하여야 합니다")
            String evidenceDescription
    ) {}
}
