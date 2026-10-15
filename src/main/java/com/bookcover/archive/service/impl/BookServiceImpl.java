package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.audit.AuditEntityType;
import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverCredit;
import com.bookcover.archive.domain.book.CoverType;
import com.bookcover.archive.domain.book.CreditRole;
import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.book.Series;
import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.book.Work;
import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.domain.contributor.Publisher;
import com.bookcover.archive.domain.contributor.Translator;
import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.event.EventPublisher;
import com.bookcover.archive.exception.DuplicateIsbnException;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.AuthorRepository;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.BookSearchRepository;
import com.bookcover.archive.repository.DesignerRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.PublisherRepository;
import com.bookcover.archive.repository.SeriesRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.TranslatorRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.repository.WorkRepository;
import com.bookcover.archive.service.BookService;
import com.bookcover.archive.service.AuditService;
import com.bookcover.archive.service.ContributionService;
import com.bookcover.archive.service.ImageService;
import com.bookcover.archive.service.ImageService.ImageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bookcover.archive.service.dto.AddCoverRequest;
import com.bookcover.archive.service.dto.CreateOriginalBookRequest;
import com.bookcover.archive.service.dto.CreateTranslatedBookRequest;
import com.bookcover.archive.service.dto.CreditInfo;
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest;
import com.bookcover.archive.service.dto.UpdateTranslatedBookRequest;
import com.bookcover.archive.validation.IsbnValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 책 관리 서비스 구현체.
 */
@Service
@Transactional
public class BookServiceImpl implements BookService {

    private final OriginalBookRepository originalBookRepository;
    private final TranslatedBookRepository translatedBookRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final DesignerRepository designerRepository;
    private final TranslatorRepository translatorRepository;
    private final WorkRepository workRepository;
    private final UserRepository userRepository;
    private final BookSearchRepository bookSearchRepository;
    private final IsbnValidator isbnValidator;
    private final ContributionService contributionService;
    private final EventPublisher eventPublisher;
    private final BookCoverRepository bookCoverRepository;
    private final SeriesRepository seriesRepository;
    private final AuditService auditService;
    private final ImageService imageService;

    public BookServiceImpl(
            OriginalBookRepository originalBookRepository,
            TranslatedBookRepository translatedBookRepository,
            AuthorRepository authorRepository,
            PublisherRepository publisherRepository,
            DesignerRepository designerRepository,
            TranslatorRepository translatorRepository,
            WorkRepository workRepository,
            UserRepository userRepository,
            BookSearchRepository bookSearchRepository,
            IsbnValidator isbnValidator,
            ContributionService contributionService,
            EventPublisher eventPublisher,
            BookCoverRepository bookCoverRepository,
            SeriesRepository seriesRepository,
            AuditService auditService,
            ImageService imageService) {
        this.originalBookRepository = originalBookRepository;
        this.translatedBookRepository = translatedBookRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.designerRepository = designerRepository;
        this.translatorRepository = translatorRepository;
        this.workRepository = workRepository;
        this.userRepository = userRepository;
        this.bookSearchRepository = bookSearchRepository;
        this.isbnValidator = isbnValidator;
        this.contributionService = contributionService;
        this.eventPublisher = eventPublisher;
        this.bookCoverRepository = bookCoverRepository;
        this.seriesRepository = seriesRepository;
        this.auditService = auditService;
        this.imageService = imageService;
    }

    @Override
    public OriginalBook createOriginalBook(CreateOriginalBookRequest request) {
        // ISBN 검증 및 중복 검사
        if (request.isbn() != null && !request.isbn().isBlank()) {
            validateNewIsbn(isbnValidator.normalizeIsbn(request.isbn()), request.isbn());
        }

        OriginalBook book = new OriginalBook();
        book.setTitle(request.title());
        book.setOriginalTitle(request.originalTitle());
        book.setCountryCode(request.countryCode());
        book.setLanguageCode(request.languageCode());
        book.setYear(request.year());
        book.setGenre(request.genre());
        book.setIsbn(request.isbn() != null ? isbnValidator.normalizeIsbn(request.isbn()) : null);
        book.setEdition(request.edition());
        book.setStatus(ContentStatus.PUBLISHED);

        // 작가 연결
        book.setAuthor(resolveAuthor(request.authorId(), request.authorName()));


        // 출판사 연결
        book.setPublisher(resolvePublisher(request.publisherId(), request.publisherName()));

        // 시리즈 연결 (출판사가 정해진 뒤에 해야 이름으로 찾을 때 출판사 범위가 맞는다)
        book.setSeries(resolveSeries(request.seriesId(), request.seriesName(), book.getPublisher()));
        book.setSeriesNumber(request.seriesNumber());

        // 작품 연결
        if (request.workId() != null) {
            Work work = workRepository.findById(request.workId())
                    .orElseThrow(() -> new ResourceNotFoundException("Work", request.workId()));
            book.setWork(work);
        }

        // 등록자 연결
        if (request.createdById() != null) {
            User user = userRepository.findById(request.createdById())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.createdById()));
            book.setCreatedBy(user);
        }

        // 앞면 커버 생성
        BookCover frontCover = createBookCover(request.frontCover(), CoverType.FRONT);
        frontCover.setBook(book);
        book.getCovers().add(frontCover);
        book.setFrontCover(frontCover);

        // 뒷면 커버 생성 (선택)
        if (request.backCover() != null) {
            BookCover backCover = createBookCover(request.backCover(), CoverType.BACK);
            book.setBackCover(backCover);
        }

        OriginalBook savedBook = originalBookRepository.save(book);

        // 기여 횟수 증가 및 칭호 업데이트
        if (request.createdById() != null) {
            contributionService.incrementContribution(request.createdById());
        }

        // 콘텐츠 생성 이벤트 발행
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.ORIGINAL_BOOK_CREATED,
                savedBook.getId(),
                request.createdById()
        ));

        return savedBook;
    }


    @Override
    public OriginalBook updateOriginalBook(UUID id, UpdateOriginalBookRequest request, UUID editorId) {
        OriginalBook book = originalBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", id));
        Map<String, Object> before = snapshot(book);

        // ISBN 검증 및 중복 검사 (변경된 경우)
        if (request.isbn() != null) {
            String normalizedIsbn = isbnValidator.normalizeIsbn(request.isbn());
            if (!normalizedIsbn.equals(book.getIsbn())) {
                validateNewIsbn(normalizedIsbn, request.isbn());
                book.setIsbn(normalizedIsbn);
            }
        }

        if (request.title() != null) book.setTitle(request.title());
        if (request.originalTitle() != null) book.setOriginalTitle(request.originalTitle());
        if (request.countryCode() != null) book.setCountryCode(request.countryCode());
        if (request.languageCode() != null) book.setLanguageCode(request.languageCode());
        if (request.year() != null) book.setYear(request.year());
        if (request.genre() != null) book.setGenre(request.genre());
        if (request.edition() != null) book.setEdition(request.edition());

        if (request.authorId() != null) {
            book.setAuthor(resolveAuthor(request.authorId(), null));
        }

        if (request.publisherId() != null) {
            book.setPublisher(resolvePublisher(request.publisherId(), null));
        }

        if (request.seriesId() != null) {
            book.setSeries(resolveSeries(request.seriesId(), null, null));
        }
        if (request.seriesNumber() != null) book.setSeriesNumber(request.seriesNumber());

        if (request.workId() != null) {
            Work work = workRepository.findById(request.workId())
                    .orElseThrow(() -> new ResourceNotFoundException("Work", request.workId()));
            book.setWork(work);
        }

        if (request.frontCover() != null) {
            updateBookCover(book.getFrontCover(), request.frontCover());
        }

        if (request.backCover() != null) {
            if (book.getBackCover() == null) {
                BookCover backCover = new BookCover();
                backCover.setCoverType(CoverType.BACK);
                book.setBackCover(backCover);
            }
            updateBookCover(book.getBackCover(), request.backCover());
        }

        book.setUpdatedAt(Instant.now());
        OriginalBook savedBook = originalBookRepository.save(book);

        // 콘텐츠 수정 이벤트 발행
        publishBookUpdated(savedBook);

        logEdit(savedBook, before, editorId);
        return savedBook;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OriginalBook> getOriginalBook(UUID id) {
        // 문제 16 해결: 삭제된 콘텐츠는 조회하지 않음
        return originalBookRepository.findById(id)
                .filter(book -> book.getStatus() != ContentStatus.DELETED);
    }

    @Override
    public void deleteOriginalBook(UUID id) {
        OriginalBook book = originalBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", id));
        softDelete(book, ContentEventType.ORIGINAL_BOOK_DELETED);
    }


    @Override
    public TranslatedBook createTranslatedBook(UUID originalBookId, CreateTranslatedBookRequest request) {
        OriginalBook originalBook = originalBookRepository.findById(originalBookId)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", originalBookId));

        // 문제 17 해결: 삭제된 원서에는 번역서를 연결할 수 없음
        if (originalBook.getStatus() == ContentStatus.DELETED) {
            throw new ValidationException("Cannot create translation for deleted original book",
                    Map.of("originalBookId", originalBookId, "status", originalBook.getStatus().name()));
        }

        // ISBN 검증 및 중복 검사
        if (request.isbn() != null && !request.isbn().isBlank()) {
            validateNewIsbn(isbnValidator.normalizeIsbn(request.isbn()), request.isbn());
        }

        TranslatedBook book = new TranslatedBook();
        book.setOriginalBook(originalBook);
        book.setTitle(request.title());
        book.setCountryCode(request.countryCode());
        book.setLanguageCode(request.languageCode());
        book.setYear(request.year());
        book.setIsbn(request.isbn() != null ? isbnValidator.normalizeIsbn(request.isbn()) : null);
        book.setEdition(request.edition());
        book.setStatus(ContentStatus.PUBLISHED);

        // 작가 연결
        book.setAuthor(resolveAuthor(request.authorId(), request.authorName()));

        // 번역가 연결
        book.setTranslator(resolveTranslator(request.translatorId(), request.translatorName()));

        // 출판사 연결
        book.setPublisher(resolvePublisher(request.publisherId(), request.publisherName()));

        // 시리즈 연결 (출판사가 정해진 뒤에 해야 이름으로 찾을 때 출판사 범위가 맞는다)
        book.setSeries(resolveSeries(request.seriesId(), request.seriesName(), book.getPublisher()));
        book.setSeriesNumber(request.seriesNumber());

        // 등록자 연결
        if (request.createdById() != null) {
            User user = userRepository.findById(request.createdById())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.createdById()));
            book.setCreatedBy(user);
        }

        // 앞면 커버 생성
        BookCover frontCover = createBookCover(request.frontCover(), CoverType.FRONT);
        frontCover.setBook(book);
        book.getCovers().add(frontCover);
        book.setFrontCover(frontCover);

        // 뒷면 커버 생성 (선택)
        if (request.backCover() != null) {
            BookCover backCover = createBookCover(request.backCover(), CoverType.BACK);
            book.setBackCover(backCover);
        }

        TranslatedBook savedBook = translatedBookRepository.save(book);

        // 기여 횟수 증가 및 칭호 업데이트
        if (request.createdById() != null) {
            contributionService.incrementContribution(request.createdById());
        }

        // 콘텐츠 생성 이벤트 발행
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.TRANSLATED_BOOK_CREATED,
                savedBook.getId(),
                request.createdById()
        ));

        return savedBook;
    }


    @Override
    public TranslatedBook updateTranslatedBook(UUID id, UpdateTranslatedBookRequest request, UUID editorId) {
        TranslatedBook book = translatedBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TranslatedBook", id));
        Map<String, Object> before = snapshot(book);

        // ISBN 검증 및 중복 검사 (변경된 경우)
        if (request.isbn() != null) {
            String normalizedIsbn = isbnValidator.normalizeIsbn(request.isbn());
            if (!normalizedIsbn.equals(book.getIsbn())) {
                validateNewIsbn(normalizedIsbn, request.isbn());
                book.setIsbn(normalizedIsbn);
            }
        }

        if (request.title() != null) book.setTitle(request.title());
        if (request.countryCode() != null) book.setCountryCode(request.countryCode());
        if (request.languageCode() != null) book.setLanguageCode(request.languageCode());
        if (request.year() != null) book.setYear(request.year());
        if (request.edition() != null) book.setEdition(request.edition());

        if (request.authorId() != null) {
            book.setAuthor(resolveAuthor(request.authorId(), null));
        }

        if (request.translatorId() != null) {
            book.setTranslator(resolveTranslator(request.translatorId(), null));
        }

        if (request.publisherId() != null) {
            book.setPublisher(resolvePublisher(request.publisherId(), null));
        }

        if (request.seriesId() != null) {
            book.setSeries(resolveSeries(request.seriesId(), null, null));
        }
        if (request.seriesNumber() != null) book.setSeriesNumber(request.seriesNumber());

        if (request.frontCover() != null) {
            updateBookCover(book.getFrontCover(), request.frontCover());
        }

        if (request.backCover() != null) {
            if (book.getBackCover() == null) {
                BookCover backCover = new BookCover();
                backCover.setCoverType(CoverType.BACK);
                book.setBackCover(backCover);
            }
            updateBookCover(book.getBackCover(), request.backCover());
        }

        book.setUpdatedAt(Instant.now());
        TranslatedBook savedBook = translatedBookRepository.save(book);

        // 콘텐츠 수정 이벤트 발행
        publishBookUpdated(savedBook);

        logEdit(savedBook, before, editorId);
        return savedBook;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TranslatedBook> getTranslatedBook(UUID id) {
        // 문제 16 해결: 삭제된 콘텐츠는 조회하지 않음
        return translatedBookRepository.findById(id)
                .filter(book -> book.getStatus() != ContentStatus.DELETED);
    }

    @Override
    public void deleteTranslatedBook(UUID id) {
        TranslatedBook book = translatedBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TranslatedBook", id));
        softDelete(book, ContentEventType.TRANSLATED_BOOK_DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranslatedBook> getTranslationsByOriginalBook(UUID originalBookId) {
        return translatedBookRepository.findByOriginalBookId(originalBookId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OriginalBook> getBooksByAuthor(UUID authorId, Pageable pageable) {
        return originalBookRepository.findByAuthorId(authorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OriginalBook> getBooksByPublisher(UUID publisherId, Pageable pageable) {
        return originalBookRepository.findByPublisherId(publisherId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Media> findDuplicatesByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return List.of();
        }
        return bookSearchRepository.findDuplicatesByIsbn(isbnValidator.normalizeIsbn(isbn));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Media> findSimilarBooks(String title, String authorName, int year) {
        return bookSearchRepository.findSimilarBooks(title, authorName, year);
    }

    @Override
    public void linkToWork(UUID bookId, UUID workId) {
        OriginalBook book = originalBookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", bookId));
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("Work", workId));
        book.setWork(work);
        book.setUpdatedAt(Instant.now());
        originalBookRepository.save(book);
    }

    @Override
    public BookCover addCover(UUID bookId, AddCoverRequest request, UUID userId) {
        Media book = findActiveBook(bookId);

        imageService.requireVerifiedImage(request.imageUrl(), request.thumbnailUrl(), ImageType.COVER,
                request.width(), request.height(), request.fileSize());

        BookCover cover = new BookCover();
        cover.setImageUrl(request.imageUrl());
        cover.setThumbnailUrl(request.thumbnailUrl());
        cover.setOriginalFileName(request.originalFileName());
        cover.setWidth(request.width());
        cover.setHeight(request.height());
        cover.setFileSize(request.fileSize());
        cover.setCoverType(CoverType.FRONT);
        cover.setImageSource(request.imageSource());
        cover.setSourceUrl(request.sourceUrl());
        cover.setSourceName(request.sourceName());
        cover.setLabel(request.label());
        cover.setPrintingFrom(request.printingFrom());
        cover.setUsedFrom(request.usedFrom());
        if (request.countryCode() != null && !request.countryCode().isBlank()) {
            cover.setCountryCode(request.countryCode().trim().toUpperCase());
        }
        if (request.languageCode() != null && !request.languageCode().isBlank()) {
            cover.setLanguageCode(request.languageCode().trim().toLowerCase());
        }
        cover.setBook(book);
        applyCredits(cover, request.credits());
        BookCover saved = bookCoverRepository.save(cover);
        book.getCovers().add(saved);
        if (userId != null) {
            auditService.logUpdate(auditTypeOf(book), book.getId(), userId, null,
                    toJson(Map.of("addedCover", coverSnapshot(saved))));
        }

        if (userId != null) {
            contributionService.incrementContribution(userId);
        }

        // 새 표지의 크레딧이 검색 인덱스에 반영되도록 수정 이벤트 발행
        publishBookUpdated(book);
        return saved;
    }

    @Override
    public void setRepresentativeCover(UUID bookId, UUID coverId, UUID userId) {
        Media book = findActiveBook(bookId);
        Map<String, Object> before = snapshot(book);
        BookCover cover = bookCoverRepository.findById(coverId)
                .orElseThrow(() -> new ResourceNotFoundException("BookCover", coverId));
        if (cover.getBook() == null || !cover.getBook().getId().equals(bookId)) {
            throw new ValidationException("Cover does not belong to this book", Map.of("coverId", coverId.toString()));
        }
        book.setFrontCover(cover);
        book.setUpdatedAt(Instant.now());
        logEdit(book, before, userId);

        // 검색 인덱스의 썸네일이 대표 표지를 따라가도록 수정 이벤트 발행
        publishBookUpdated(book);
    }

    private void publishBookUpdated(Media book) {
        eventPublisher.publishContentEvent(new ContentEvent(
                book instanceof OriginalBook ? ContentEventType.ORIGINAL_BOOK_UPDATED : ContentEventType.TRANSLATED_BOOK_UPDATED,
                book.getId(),
                creatorIdOf(book)
        ));
    }

    /** 삭제 상태로 바꾸고 등록자 기여 수를 줄인 뒤 삭제 이벤트를 발행한다. 영속 상태라 커밋 때 반영된다 */
    private void softDelete(Media book, ContentEventType deletedEvent) {
        // 문제 18 해결: 이미 삭제된 콘텐츠는 다시 삭제하지 않음
        if (book.getStatus() == ContentStatus.DELETED) {
            return;
        }
        book.setStatus(ContentStatus.DELETED);
        book.setUpdatedAt(Instant.now());

        // 문제 7 해결: 기여 카운트 감소
        UUID userId = creatorIdOf(book);
        if (userId != null) {
            contributionService.decrementContribution(userId);
        }
        eventPublisher.publishContentEvent(new ContentEvent(deletedEvent, book.getId(), userId));
    }

    private static UUID creatorIdOf(Media book) {
        return book.getCreatedBy() != null ? book.getCreatedBy().getId() : null;
    }


    // Private helper methods

    private Media findActiveBook(UUID id) {
        return originalBookRepository.findById(id).<Media>map(b -> b)
                .or(() -> translatedBookRepository.findById(id))
                .filter(b -> b.getStatus() != ContentStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    /** 새로 저장할 ISBN(정규화된 값)의 형식과 중복을 검사한다. raw는 오류 응답에 담을 입력 원문 */
    private void validateNewIsbn(String isbn, String raw) {
        if (!isbnValidator.isValid(isbn)) {
            throw new ValidationException("Invalid ISBN format", Map.of("isbn", raw));
        }
        checkIsbnDuplicate(isbn);
    }

    private void checkIsbnDuplicate(String isbn) {
        Optional<OriginalBook> existingOriginal = originalBookRepository.findByIsbn(isbn);
        if (existingOriginal.isPresent()) {
            throw new DuplicateIsbnException(isbn, existingOriginal.get().getId());
        }

        Optional<TranslatedBook> existingTranslated = translatedBookRepository.findByIsbn(isbn);
        if (existingTranslated.isPresent()) {
            throw new DuplicateIsbnException(isbn, existingTranslated.get().getId());
        }
    }

    private BookCover createBookCover(CreateOriginalBookRequest.BookCoverInfo info, CoverType coverType) {
        imageService.requireVerifiedImage(info.imageUrl(), info.thumbnailUrl(), ImageType.COVER,
                info.width(), info.height(), info.fileSize());
        BookCover cover = new BookCover();
        cover.setImageUrl(info.imageUrl());
        cover.setThumbnailUrl(info.thumbnailUrl());
        cover.setOriginalFileName(info.originalFileName());
        cover.setWidth(info.width());
        cover.setHeight(info.height());
        cover.setFileSize(info.fileSize());
        cover.setCoverType(coverType);
        cover.setImageSource(info.imageSource());
        cover.setSourceUrl(info.sourceUrl());
        cover.setSourceName(info.sourceName());
        applyCredits(cover, info.credits());
        return cover;
    }

    private BookCover createBookCover(CreateTranslatedBookRequest.BookCoverInfo info, CoverType coverType) {
        imageService.requireVerifiedImage(info.imageUrl(), info.thumbnailUrl(), ImageType.COVER,
                info.width(), info.height(), info.fileSize());
        BookCover cover = new BookCover();
        cover.setImageUrl(info.imageUrl());
        cover.setThumbnailUrl(info.thumbnailUrl());
        cover.setOriginalFileName(info.originalFileName());
        cover.setWidth(info.width());
        cover.setHeight(info.height());
        cover.setFileSize(info.fileSize());
        cover.setCoverType(coverType);
        cover.setImageSource(info.imageSource());
        cover.setSourceUrl(info.sourceUrl());
        cover.setSourceName(info.sourceName());
        applyCredits(cover, info.credits());
        return cover;
    }

    private void updateBookCover(BookCover cover, UpdateOriginalBookRequest.BookCoverUpdateInfo info) {
        if (info.imageUrl() != null || info.thumbnailUrl() != null || info.width() != null
                || info.height() != null || info.fileSize() != null) {
            imageService.requireVerifiedImage(info.imageUrl() != null ? info.imageUrl() : cover.getImageUrl(),
                    info.thumbnailUrl() != null ? info.thumbnailUrl() : cover.getThumbnailUrl(), ImageType.COVER,
                    info.imageUrl() != null ? info.width() : (info.width() != null ? info.width() : cover.getWidth()),
                    info.imageUrl() != null ? info.height() : (info.height() != null ? info.height() : cover.getHeight()),
                    info.imageUrl() != null ? info.fileSize() : (info.fileSize() != null ? info.fileSize() : cover.getFileSize()));
        }
        if (info.imageUrl() != null) cover.setImageUrl(info.imageUrl());
        if (info.thumbnailUrl() != null) cover.setThumbnailUrl(info.thumbnailUrl());
        if (info.originalFileName() != null) cover.setOriginalFileName(info.originalFileName());
        if (info.width() != null || info.imageUrl() != null) cover.setWidth(info.width());
        if (info.height() != null || info.imageUrl() != null) cover.setHeight(info.height());
        if (info.fileSize() != null || info.imageUrl() != null) cover.setFileSize(info.fileSize());
        if (info.imageSource() != null) cover.setImageSource(info.imageSource());
        if (info.sourceUrl() != null) cover.setSourceUrl(info.sourceUrl());
        if (info.sourceName() != null) cover.setSourceName(info.sourceName());
        applyCredits(cover, info.credits());
    }

    private void updateBookCover(BookCover cover, UpdateTranslatedBookRequest.BookCoverUpdateInfo info) {
        if (info.imageUrl() != null || info.thumbnailUrl() != null || info.width() != null
                || info.height() != null || info.fileSize() != null) {
            imageService.requireVerifiedImage(info.imageUrl() != null ? info.imageUrl() : cover.getImageUrl(),
                    info.thumbnailUrl() != null ? info.thumbnailUrl() : cover.getThumbnailUrl(), ImageType.COVER,
                    info.imageUrl() != null ? info.width() : (info.width() != null ? info.width() : cover.getWidth()),
                    info.imageUrl() != null ? info.height() : (info.height() != null ? info.height() : cover.getHeight()),
                    info.imageUrl() != null ? info.fileSize() : (info.fileSize() != null ? info.fileSize() : cover.getFileSize()));
        }
        if (info.imageUrl() != null) cover.setImageUrl(info.imageUrl());
        if (info.thumbnailUrl() != null) cover.setThumbnailUrl(info.thumbnailUrl());
        if (info.originalFileName() != null) cover.setOriginalFileName(info.originalFileName());
        if (info.width() != null || info.imageUrl() != null) cover.setWidth(info.width());
        if (info.height() != null || info.imageUrl() != null) cover.setHeight(info.height());
        if (info.fileSize() != null || info.imageUrl() != null) cover.setFileSize(info.fileSize());
        if (info.imageSource() != null) cover.setImageSource(info.imageSource());
        if (info.sourceUrl() != null) cover.setSourceUrl(info.sourceUrl());
        if (info.sourceName() != null) cover.setSourceName(info.sourceName());
        applyCredits(cover, info.credits());
    }

    /**
     * 표지 크레딧을 요청 목록과 같게 맞춘다. credits가 null이면 그대로 둔다.
     * 전부 지우고 다시 넣으면 flush 때 INSERT가 DELETE보다 먼저 나가 유니크 제약에 걸리므로,
     * 그대로인 (디자이너, 역할)은 남기고 차이만 반영한다.
     */
    private void applyCredits(BookCover cover, List<CreditInfo> credits) {
        if (credits == null) {
            return;
        }
        // 이름 없는 디자이너를 새로 만들므로 한 요청이 공개 디자이너 목록을 채우지 못하게 막는다
        if (credits.size() > MAX_CREDITS_PER_COVER) {
            throw new ValidationException("표지 크레딧은 " + MAX_CREDITS_PER_COVER + "개까지 넣을 수 있습니다",
                    Map.of("credits", credits.size()));
        }
        List<CoverCredit> wanted = new ArrayList<>();
        Set<String> wantedKeys = new HashSet<>();
        for (CreditInfo info : credits) {
            if (info == null) {
                throw new ValidationException("Credit entry must not be null", Map.of());
            }
            if (info.role() == null) {
                throw new ValidationException("Credit role is required", Map.of());
            }
            Designer designer = resolveDesigner(info.designerId(), info.designerName());
            if (designer == null) {
                throw new ValidationException("Credit requires designerId or designerName",
                        Map.of("role", info.role().name()));
            }
            if (wantedKeys.add(creditKey(designer, info.role()))) {
                wanted.add(new CoverCredit(cover, designer, info.role()));
            }
        }

        cover.getCredits().removeIf(c -> !wantedKeys.contains(creditKey(c.getDesigner(), c.getRole())));
        Set<String> existingKeys = new HashSet<>();
        cover.getCredits().forEach(c -> existingKeys.add(creditKey(c.getDesigner(), c.getRole())));
        wanted.stream()
                .filter(c -> !existingKeys.contains(creditKey(c.getDesigner(), c.getRole())))
                .forEach(cover.getCredits()::add);
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 위키식 편집 기록. 수정 전후 스냅샷에서 바뀐 항목만 감사 로그에 남긴다 (조회: GET /admin/dashboard/logs/entity/{type}/{id}).
     * 이전 값(previousValue)을 그대로 남기므로 장난성 수정을 관리자가 보고 되돌릴 수 있다.
     */
    // ponytail: 되돌리기는 수동(이전 값을 보고 PUT). 반복되면 로그 id로 되돌리는 관리자 API를 추가
    private void logEdit(Media book, Map<String, Object> before, UUID userId) {
        if (userId == null) {
            return;
        }
        Map<String, Object> after = snapshot(book);
        Map<String, Object> previous = new LinkedHashMap<>();
        Map<String, Object> changed = new LinkedHashMap<>();
        after.forEach((key, value) -> {
            if (!Objects.equals(before.get(key), value)) {
                previous.put(key, before.get(key));
                changed.put(key, value);
            }
        });
        if (!changed.isEmpty()) {
            auditService.logUpdate(auditTypeOf(book), book.getId(), userId, toJson(previous), toJson(changed));
        }
    }

    /** 수정 API로 바꿀 수 있는 항목의 스냅샷 (연관은 id로) */
    private static Map<String, Object> snapshot(Media book) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", book.getTitle());
        s.put("countryCode", book.getCountryCode());
        s.put("languageCode", book.getLanguageCode());
        s.put("year", book.getYear());
        s.put("seriesId", book.getSeries() != null ? book.getSeries().getId() : null);
        s.put("seriesNumber", book.getSeriesNumber());
        BookCover backCover = null;
        if (book instanceof OriginalBook ob) {
            s.put("originalTitle", ob.getOriginalTitle());
            s.put("genre", ob.getGenre());
            s.put("isbn", ob.getIsbn());
            s.put("edition", ob.getEdition());
            s.put("authorId", ob.getAuthor() != null ? ob.getAuthor().getId() : null);
            s.put("publisherId", ob.getPublisher() != null ? ob.getPublisher().getId() : null);
            s.put("workId", ob.getWork() != null ? ob.getWork().getId() : null);
            backCover = ob.getBackCover();
        } else if (book instanceof TranslatedBook tb) {
            s.put("isbn", tb.getIsbn());
            s.put("edition", tb.getEdition());
            s.put("authorId", tb.getAuthor() != null ? tb.getAuthor().getId() : null);
            s.put("translatorId", tb.getTranslator() != null ? tb.getTranslator().getId() : null);
            s.put("publisherId", tb.getPublisher() != null ? tb.getPublisher().getId() : null);
            backCover = tb.getBackCover();
        }
        s.put("frontCover", coverSnapshot(book.getFrontCover()));
        s.put("backCover", coverSnapshot(backCover));
        return s;
    }

    private static Map<String, Object> coverSnapshot(BookCover cover) {
        if (cover == null) {
            return null;
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", cover.getId());
        s.put("imageUrl", cover.getImageUrl());
        s.put("thumbnailUrl", cover.getThumbnailUrl());
        s.put("sourceUrl", cover.getSourceUrl());
        s.put("sourceName", cover.getSourceName());
        s.put("credits", cover.getCredits().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("designerId", c.getDesigner().getId());
                    m.put("name", c.getDesigner().getName());
                    m.put("role", c.getRole());
                    return m;
                })
                .sorted(Comparator.comparing((Map<String, Object> m) -> m.get("designerId") + ":" + m.get("role")))
                .toList());
        return s;
    }

    private static AuditEntityType auditTypeOf(Media book) {
        return book instanceof OriginalBook ? AuditEntityType.ORIGINAL_BOOK : AuditEntityType.TRANSLATED_BOOK;
    }

    private static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit value", e);
        }
    }

    private static final int MAX_CREDITS_PER_COVER = 20;
    private static final int MAX_NAME_LENGTH = 255;

    /** 이름 컬럼(varchar 255)을 넘으면 DB 오류(500) 대신 400으로 거절한다 */
    private static String checkNameLength(String name, String field) {
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(field + "은 " + MAX_NAME_LENGTH + "자 이하여야 합니다", Map.of(field, name.length()));
        }
        return name;
    }

    private static String creditKey(Designer designer, CreditRole role) {
        return designer.getId() + ":" + role;
    }

    private Designer resolveDesigner(UUID designerId, String designerName) {
        if (designerId != null) {
            return designerRepository.findById(designerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Designer", designerId));
        }
        if (designerName == null || designerName.isBlank()) {
            return null;
        }
        String name = checkNameLength(designerName.trim(), "designerName");
        // ponytail: 이름 유니크 제약이 없어 동시 등록이면 중복 행이 생길 수 있다. 조회가 첫 행을 쓰므로 500은 안 나고, 중복은 관리자 병합으로 정리
        return designerRepository.findExactNameOrAlias(name).stream().findFirst()
                .orElseGet(() -> {
                    Designer newDesigner = new Designer();
                    newDesigner.setName(name);
                    return designerRepository.save(newDesigner);
                });
    }

    /** authorId가 있으면 그것을, 없으면 이름으로 찾거나 새로 만든다. 둘 다 없으면 null */
    private Author resolveAuthor(UUID authorId, String authorName) {
        if (authorId != null) {
            return authorRepository.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author", authorId));
        }
        if (authorName == null || authorName.isBlank()) {
            return null;
        }
        return authorRepository.findByName(authorName).orElseGet(() -> {
            Author author = new Author();
            author.setName(authorName);
            return authorRepository.save(author);
        });
    }

    /** publisherId가 있으면 그것을, 없으면 이름으로 찾거나 새로 만든다. 둘 다 없으면 null */
    private Publisher resolvePublisher(UUID publisherId, String publisherName) {
        if (publisherId != null) {
            return publisherRepository.findById(publisherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher", publisherId));
        }
        if (publisherName == null || publisherName.isBlank()) {
            return null;
        }
        return publisherRepository.findByName(publisherName).orElseGet(() -> {
            Publisher publisher = new Publisher();
            publisher.setName(publisherName);
            return publisherRepository.save(publisher);
        });
    }

    /** translatorId가 있으면 그것을, 없으면 이름으로 찾거나 새로 만든다. 둘 다 없으면 null */
    private Translator resolveTranslator(UUID translatorId, String translatorName) {
        if (translatorId != null) {
            return translatorRepository.findById(translatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Translator", translatorId));
        }
        if (translatorName == null || translatorName.isBlank()) {
            return null;
        }
        return translatorRepository.findByName(translatorName).orElseGet(() -> {
            Translator translator = new Translator();
            translator.setName(translatorName);
            return translatorRepository.save(translator);
        });
    }

    /** seriesId가 있으면 그것을, 없으면 출판사 안에서 이름으로 찾거나 새로 만든다 */
    private Series resolveSeries(UUID seriesId, String seriesName, Publisher publisher) {
        if (seriesId != null) {
            return seriesRepository.findById(seriesId)
                    .orElseThrow(() -> new ResourceNotFoundException("Series", seriesId));
        }
        if (seriesName == null || seriesName.isBlank()) {
            return null;
        }
        String name = checkNameLength(seriesName.trim(), "seriesName");
        // ponytail: (이름, 출판사) 유니크 제약이 없어 동시 등록이면 중복 행이 생길 수 있다. 조회는 첫 행을 쓰므로 500은 안 난다
        Optional<Series> existing = publisher != null
                ? seriesRepository.findFirstByNameAndPublisherIdOrderByCreatedAtAsc(name, publisher.getId())
                : seriesRepository.findFirstByNameAndPublisherIsNullOrderByCreatedAtAsc(name);
        return existing.orElseGet(() -> {
            Series series = new Series();
            series.setName(name);
            series.setPublisher(publisher);
            return seriesRepository.save(series);
        });
    }
}
