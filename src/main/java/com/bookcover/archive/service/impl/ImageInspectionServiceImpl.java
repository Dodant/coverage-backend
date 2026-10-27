package com.bookcover.archive.service.impl;

import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.service.ImageInspectionService;
import com.bookcover.archive.service.ImageService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 기존 표지도 관리 저장소에서만 읽으며, 분석 실패는 파일별로 격리한다. */
@Service
public class ImageInspectionServiceImpl implements ImageInspectionService {
    private final EntityManager em;
    private final ImageService images;
    public ImageInspectionServiceImpl(EntityManager em, ImageService images) { this.em = em; this.images = images; }
    private static final String FROM = """
        FROM book_covers c
        JOIN media m ON m.id = c.book_id OR EXISTS (SELECT 1 FROM original_books ob WHERE ob.id = m.id AND ob.front_cover_id = c.id)
          OR EXISTS (SELECT 1 FROM translated_books tb WHERE tb.id = m.id AND tb.front_cover_id = c.id)
        LEFT JOIN original_books o ON o.id = m.id
        LEFT JOIN translated_books t ON t.id = m.id
        LEFT JOIN image_assets a ON a.image_url = c.image_url
        WHERE NOT c.hidden AND m.status <> 'DELETED' AND COALESCE(a.blocked, false) = false
          AND (o.id IS NOT NULL OR t.id IS NOT NULL)
        """;
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ScanResult scan(UUID after, int limit) {
        requireAdmin();
        int size = Math.max(1, Math.min(limit, 10));
        var query = em.createNativeQuery("SELECT DISTINCT CAST(c.id AS varchar(36)), c.image_url, CAST(m.id AS varchar(36)) " + FROM
                + (after == null ? "" : " AND CAST(c.id AS varchar(36)) > :after") + " ORDER BY CAST(c.id AS varchar(36))");
        if (after != null) query.setParameter("after", after.toString());
        List<?> rows = query.setMaxResults(size + 1).getResultList();
        List<ScanFailure> failures = new ArrayList<>(); int indexed = 0, examined = 0; UUID cursor = after;
        for (Object value : rows.subList(0, Math.min(size, rows.size()))) {
            Object[] row = (Object[]) value;
            cursor = UUID.fromString(row[0].toString()); examined++;
            try { if (images.indexFingerprint((String) row[1])) indexed++; }
            catch (ValidationException e) { failures.add(new ScanFailure(cursor, UUID.fromString(row[2].toString()), e.getMessage())); }
            catch (RuntimeException e) { failures.add(new ScanFailure(cursor, UUID.fromString(row[2].toString()), "파일을 분석하지 못했습니다. 저장소 상태를 확인하세요.")); }
        }
        return new ScanResult(examined, indexed, cursor, rows.size() > size, failures);
    }
    private record Indexed(CoverRef cover, String sha, String perceptual, Integer width, Integer height) {}
    @Override @Transactional(readOnly = true)
    public Results matches(UUID bookId, String kind, int page) {
        requireAdmin();
        // 포인터와 해시만 읽고 원본 파일은 다시 내려받지 않는다. 모든 검토 후보에 페이지를 적용한다.
        List<?> rows = em.createNativeQuery("SELECT CAST(c.id AS varchar(36)), CAST(m.id AS varchar(36)), m.title, m.language_code, COALESCE(o.edition, t.edition), c.image_url, c.thumbnail_url, a.file_sha256, a.perceptual_hash, c.width, c.height " + FROM + " ORDER BY CAST(c.id AS varchar(36))").getResultList();
        List<Indexed> indexed = rows.stream().map(value -> {
            Object[] r = (Object[]) value;
            return new Indexed(new CoverRef(UUID.fromString((String) r[0]), UUID.fromString((String) r[1]), (String) r[2], (String) r[3], (String) r[4], (String) r[5], (String) r[6]),
                    (String) r[7], (String) r[8], r[9] == null ? null : ((Number) r[9]).intValue(), r[10] == null ? null : ((Number) r[10]).intValue());
        }).toList();
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < indexed.size(); i++) for (int j = i + 1; j < indexed.size(); j++) {
            Indexed a = indexed.get(i), b = indexed.get(j);
            if (bookId != null && !bookId.equals(a.cover.bookId()) && !bookId.equals(b.cover.bookId())) continue;
            String matchKind; int distance = 0;
            if (a.sha != null && a.sha.equals(b.sha)) matchKind = "EXACT_FILE";
            else if (Objects.equals(a.cover.imageUrl(), b.cover.imageUrl())) matchKind = "SAME_URL";
            else {
                if (a.perceptual == null || b.perceptual == null || !similarAspect(a, b)) continue;
                distance = ImageFingerprint.distance(a.perceptual, b.perceptual);
                if (distance > 6) continue;
                matchKind = "SIMILAR";
            }
            if (kind != null && !kind.isBlank() && !kind.equals(matchKind)) continue;
            matches.add(new Match(a.cover, b.cover, matchKind, distance));
        }
        matches.sort(Comparator.comparingInt((Match m) -> switch (m.kind()) { case "EXACT_FILE" -> 0; case "SAME_URL" -> 1; default -> 2; }).thenComparingInt(Match::distance));
        int p = Math.max(0, page), start = (int) Math.min((long) p * 20, matches.size());
        return new Results(matches.subList(start, Math.min(start + 20, matches.size())), matches.size(), indexed.size(),
                (int) indexed.stream().filter(i -> i.sha != null && i.perceptual != null).count(), p, (matches.size() + 19) / 20);
    }
    private static boolean similarAspect(Indexed a, Indexed b) {
        if (a.width == null || a.height == null || b.width == null || b.height == null || a.width <= 0 || a.height <= 0 || b.width <= 0 || b.height <= 0) return false;
        double left = (double) a.width / a.height, right = (double) b.width / b.height;
        return Math.abs(left / right - 1) <= .03;
    }
    private static void requireAdmin() { if (!SecurityUtils.hasRole("ADMIN")) throw new ValidationException("관리자만 이미지를 비교할 수 있습니다.", Map.of()); }
}
