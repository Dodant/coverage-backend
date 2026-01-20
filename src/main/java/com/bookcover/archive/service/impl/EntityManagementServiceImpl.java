package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.audit.AuditAction;
import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.domain.contributor.Publisher;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.AuthorRepository;
import com.bookcover.archive.repository.DesignerRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.PublisherRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.service.AuditService;
import com.bookcover.archive.service.EntityManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 엔티티 관리 서비스 구현체.
 * 엔티티 병합 및 별칭 관리 기능을 구현합니다.
 *
 * Requirements: 19.5, 19.6
 */
@Service
@Transactional
public class EntityManagementServiceImpl implements EntityManagementService {

    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final DesignerRepository designerRepository;
    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final AuditService auditService;

    public EntityManagementServiceImpl(
            AuthorRepository authorRepository,
            PublisherRepository publisherRepository,
            DesignerRepository designerRepository,
            OriginalBookRepository originalBookRepository,
            TranslatedBookRepository translatedBookRepository,
            AuditService auditService) {
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.designerRepository = designerRepository;
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.auditService = auditService;
    }

    // 작가 별칭 관리

    @Override
    public Author addAuthorAlias(UUID authorId, String alias) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", authorId));

        if (alias == null || alias.isBlank()) {
            throw new ValidationException("Alias cannot be empty", Map.of("alias", "null or blank"));
        }

        author.addAlias(alias.trim());
        return authorRepository.save(author);
    }

    @Override
    public Author removeAuthorAlias(UUID authorId, String alias) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", authorId));

        author.removeAlias(alias);
        return authorRepository.save(author);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAuthorAliases(UUID authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", authorId));
        return author.getAliases();
    }

    // 출판사 별칭 관리

    @Override
    public Publisher addPublisherAlias(UUID publisherId, String alias) {
        Publisher publisher = publisherRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", publisherId));

        if (alias == null || alias.isBlank()) {
            throw new ValidationException("Alias cannot be empty", Map.of("alias", "null or blank"));
        }

        publisher.addAlias(alias.trim());
        return publisherRepository.save(publisher);
    }

    @Override
    public Publisher removePublisherAlias(UUID publisherId, String alias) {
        Publisher publisher = publisherRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", publisherId));

        publisher.removeAlias(alias);
        return publisherRepository.save(publisher);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getPublisherAliases(UUID publisherId) {
        Publisher publisher = publisherRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", publisherId));
        return publisher.getAliases();
    }

    // 디자이너 별칭 관리

    @Override
    public Designer addDesignerAlias(UUID designerId, String alias) {
        Designer designer = designerRepository.findById(designerId)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", designerId));

        if (alias == null || alias.isBlank()) {
            throw new ValidationException("Alias cannot be empty", Map.of("alias", "null or blank"));
        }

        designer.addAlias(alias.trim());
        return designerRepository.save(designer);
    }

    @Override
    public Designer removeDesignerAlias(UUID designerId, String alias) {
        Designer designer = designerRepository.findById(designerId)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", designerId));

        designer.removeAlias(alias);
        return designerRepository.save(designer);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getDesignerAliases(UUID designerId) {
        Designer designer = designerRepository.findById(designerId)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", designerId));
        return designer.getAliases();
    }

    // 엔티티 병합

    @Override
    public void mergeAuthors(UUID sourceId, UUID targetId, UUID performedById) {
        if (sourceId.equals(targetId)) {
            throw new ValidationException("Cannot merge entity with itself",
                    Map.of("sourceId", sourceId, "targetId", targetId));
        }

        Author source = authorRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", sourceId));
        Author target = authorRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", targetId));

        // source의 별칭을 target에 추가
        target.getAliases().addAll(source.getAliases());
        target.addAlias(source.getName()); // source 이름도 별칭으로 추가

        // source의 책들을 target으로 이전
        originalBookRepository.findByAuthorId(sourceId, null).forEach(book -> {
            book.setAuthor(target);
            originalBookRepository.save(book);
        });

        translatedBookRepository.findByAuthorId(sourceId, null).forEach(book -> {
            book.setAuthor(target);
            translatedBookRepository.save(book);
        });

        // 감사 로그 기록
        String mergeInfo = String.format("{\"sourceId\":\"%s\",\"sourceName\":\"%s\",\"targetId\":\"%s\",\"targetName\":\"%s\"}",
                sourceId, source.getName(), targetId, target.getName());
        auditService.log(AuditAction.MERGE, AuditEntityType.AUTHOR, targetId, performedById, null, mergeInfo);

        // target 저장 및 source 삭제
        authorRepository.save(target);
        authorRepository.delete(source);
    }

    @Override
    public void mergePublishers(UUID sourceId, UUID targetId, UUID performedById) {
        if (sourceId.equals(targetId)) {
            throw new ValidationException("Cannot merge entity with itself",
                    Map.of("sourceId", sourceId, "targetId", targetId));
        }

        Publisher source = publisherRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", sourceId));
        Publisher target = publisherRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", targetId));

        // source의 별칭을 target에 추가
        target.getAliases().addAll(source.getAliases());
        target.addAlias(source.getName());

        // source의 책들을 target으로 이전
        originalBookRepository.findByPublisherId(sourceId, null).forEach(book -> {
            book.setPublisher(target);
            originalBookRepository.save(book);
        });

        translatedBookRepository.findByPublisherId(sourceId, null).forEach(book -> {
            book.setPublisher(target);
            translatedBookRepository.save(book);
        });

        // 감사 로그 기록
        String mergeInfo = String.format("{\"sourceId\":\"%s\",\"sourceName\":\"%s\",\"targetId\":\"%s\",\"targetName\":\"%s\"}",
                sourceId, source.getName(), targetId, target.getName());
        auditService.log(AuditAction.MERGE, AuditEntityType.PUBLISHER, targetId, performedById, null, mergeInfo);

        // target 저장 및 source 삭제
        publisherRepository.save(target);
        publisherRepository.delete(source);
    }

    @Override
    public void mergeDesigners(UUID sourceId, UUID targetId, UUID performedById) {
        if (sourceId.equals(targetId)) {
            throw new ValidationException("Cannot merge entity with itself",
                    Map.of("sourceId", sourceId, "targetId", targetId));
        }

        Designer source = designerRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", sourceId));
        Designer target = designerRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Designer", targetId));

        // source의 별칭을 target에 추가
        target.getAliases().addAll(source.getAliases());
        target.addAlias(source.getName());

        // source의 원서들을 target으로 이전
        originalBookRepository.findByDesignerId(sourceId, null).forEach(book -> {
            book.setDesigner(target);
            originalBookRepository.save(book);
        });

        // source의 번역서들을 target으로 이전
        translatedBookRepository.findByDesignerId(sourceId, null).forEach(book -> {
            book.setDesigner(target);
            translatedBookRepository.save(book);
        });

        // 감사 로그 기록
        String mergeInfo = String.format("{\"sourceId\":\"%s\",\"sourceName\":\"%s\",\"targetId\":\"%s\",\"targetName\":\"%s\"}",
                sourceId, source.getName(), targetId, target.getName());
        auditService.log(AuditAction.MERGE, AuditEntityType.DESIGNER, targetId, performedById, null, mergeInfo);

        // target 저장 및 source 삭제
        designerRepository.save(target);
        designerRepository.delete(source);
    }
}
