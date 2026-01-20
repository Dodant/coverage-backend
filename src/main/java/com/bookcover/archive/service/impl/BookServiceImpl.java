package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverType;
import com.bookcover.archive.domain.book.OriginalBook;
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
import com.bookcover.archive.repository.BookSearchRepository;
import com.bookcover.archive.repository.DesignerRepository;
import com.bookcover.archive.repository.OriginalBookRepository;
import com.bookcover.archive.repository.PublisherRepository;
import com.bookcover.archive.repository.TranslatedBookRepository;
import com.bookcover.archive.repository.TranslatorRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.repository.WorkRepository;
import com.bookcover.archive.service.BookService;
import com.bookcover.archive.service.ContributionService;
import com.bookcover.archive.service.dto.CreateOriginalBookRequest;
import com.bookcover.archive.service.dto.CreateTranslatedBookRequest;
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest;
import com.bookcover.archive.service.dto.UpdateTranslatedBookRequest;
import com.bookcover.archive.validation.IsbnValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            EventPublisher eventPublisher) {
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
    }

    @Override
    public OriginalBook createOriginalBook(CreateOriginalBookRequest request) {
        // ISBN 검증 및 중복 검사
        if (request.isbn() != null && !request.isbn().isBlank()) {
            String normalizedIsbn = isbnValidator.normalizeIsbn(request.isbn());
            if (!isbnValidator.isValid(normalizedIsbn)) {
                throw new ValidationException("Invalid ISBN format", Map.of("isbn", request.isbn()));
            }
            checkIsbnDuplicate(normalizedIsbn);
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
        if (request.authorId() != null) {
            Author author = authorRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author", request.authorId()));
            book.setAuthor(author);
        } else if (request.authorName() != null && !request.authorName().isBlank()) {
            Author author = authorRepository.findByName(request.authorName())
                    .orElseGet(() -> {
                        Author newAuthor = new Author();
                        newAuthor.setName(request.authorName());
                        return authorRepository.save(newAuthor);
                    });
            book.setAuthor(author);
        }


        // 출판사 연결
        if (request.publisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.publisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher", request.publisherId()));
            book.setPublisher(publisher);
        } else if (request.publisherName() != null && !request.publisherName().isBlank()) {
            Publisher publisher = publisherRepository.findByName(request.publisherName())
                    .orElseGet(() -> {
                        Publisher newPublisher = new Publisher();
                        newPublisher.setName(request.publisherName());
                        return publisherRepository.save(newPublisher);
                    });
            book.setPublisher(publisher);
        }

        // 디자이너 연결
        if (request.designerId() != null) {
            Designer designer = designerRepository.findById(request.designerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designer", request.designerId()));
            book.setDesigner(designer);
        } else if (request.designerName() != null && !request.designerName().isBlank()) {
            Designer designer = designerRepository.findByName(request.designerName())
                    .orElseGet(() -> {
                        Designer newDesigner = new Designer();
                        newDesigner.setName(request.designerName());
                        return designerRepository.save(newDesigner);
                    });
            book.setDesigner(designer);
        }

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
    public OriginalBook updateOriginalBook(UUID id, UpdateOriginalBookRequest request) {
        OriginalBook book = originalBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", id));

        // ISBN 검증 및 중복 검사 (변경된 경우)
        if (request.isbn() != null) {
            String normalizedIsbn = isbnValidator.normalizeIsbn(request.isbn());
            if (!normalizedIsbn.equals(book.getIsbn())) {
                if (!isbnValidator.isValid(normalizedIsbn)) {
                    throw new ValidationException("Invalid ISBN format", Map.of("isbn", request.isbn()));
                }
                checkIsbnDuplicate(normalizedIsbn);
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
            Author author = authorRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author", request.authorId()));
            book.setAuthor(author);
        }

        if (request.publisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.publisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher", request.publisherId()));
            book.setPublisher(publisher);
        }

        if (request.designerId() != null) {
            Designer designer = designerRepository.findById(request.designerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designer", request.designerId()));
            book.setDesigner(designer);
        }

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
        UUID userId = book.getCreatedBy() != null ? book.getCreatedBy().getId() : null;
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.ORIGINAL_BOOK_UPDATED,
                savedBook.getId(),
                userId
        ));

        return savedBook;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OriginalBook> getOriginalBook(UUID id) {
        return originalBookRepository.findById(id);
    }

    @Override
    public void deleteOriginalBook(UUID id) {
        OriginalBook book = originalBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", id));
        book.setStatus(ContentStatus.DELETED);
        book.setUpdatedAt(Instant.now());
        originalBookRepository.save(book);

        // 콘텐츠 삭제 이벤트 발행
        UUID userId = book.getCreatedBy() != null ? book.getCreatedBy().getId() : null;
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.ORIGINAL_BOOK_DELETED,
                id,
                userId
        ));
    }


    @Override
    public TranslatedBook createTranslatedBook(UUID originalBookId, CreateTranslatedBookRequest request) {
        OriginalBook originalBook = originalBookRepository.findById(originalBookId)
                .orElseThrow(() -> new ResourceNotFoundException("OriginalBook", originalBookId));

        // ISBN 검증 및 중복 검사
        if (request.isbn() != null && !request.isbn().isBlank()) {
            String normalizedIsbn = isbnValidator.normalizeIsbn(request.isbn());
            if (!isbnValidator.isValid(normalizedIsbn)) {
                throw new ValidationException("Invalid ISBN format", Map.of("isbn", request.isbn()));
            }
            checkIsbnDuplicate(normalizedIsbn);
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
        if (request.authorId() != null) {
            Author author = authorRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author", request.authorId()));
            book.setAuthor(author);
        } else if (request.authorName() != null && !request.authorName().isBlank()) {
            Author author = authorRepository.findByName(request.authorName())
                    .orElseGet(() -> {
                        Author newAuthor = new Author();
                        newAuthor.setName(request.authorName());
                        return authorRepository.save(newAuthor);
                    });
            book.setAuthor(author);
        }

        // 번역가 연결
        if (request.translatorId() != null) {
            Translator translator = translatorRepository.findById(request.translatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Translator", request.translatorId()));
            book.setTranslator(translator);
        } else if (request.translatorName() != null && !request.translatorName().isBlank()) {
            Translator translator = translatorRepository.findByName(request.translatorName())
                    .orElseGet(() -> {
                        Translator newTranslator = new Translator();
                        newTranslator.setName(request.translatorName());
                        return translatorRepository.save(newTranslator);
                    });
            book.setTranslator(translator);
        }

        // 출판사 연결
        if (request.publisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.publisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher", request.publisherId()));
            book.setPublisher(publisher);
        } else if (request.publisherName() != null && !request.publisherName().isBlank()) {
            Publisher publisher = publisherRepository.findByName(request.publisherName())
                    .orElseGet(() -> {
                        Publisher newPublisher = new Publisher();
                        newPublisher.setName(request.publisherName());
                        return publisherRepository.save(newPublisher);
                    });
            book.setPublisher(publisher);
        }

        // 디자이너 연결
        if (request.designerId() != null) {
            Designer designer = designerRepository.findById(request.designerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designer", request.designerId()));
            book.setDesigner(designer);
        } else if (request.designerName() != null && !request.designerName().isBlank()) {
            Designer designer = designerRepository.findByName(request.designerName())
                    .orElseGet(() -> {
                        Designer newDesigner = new Designer();
                        newDesigner.setName(request.designerName());
                        return designerRepository.save(newDesigner);
                    });
            book.setDesigner(designer);
        }

        // 등록자 연결
        if (request.createdById() != null) {
            User user = userRepository.findById(request.createdById())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.createdById()));
            book.setCreatedBy(user);
        }

        // 앞면 커버 생성
        BookCover frontCover = createBookCover(request.frontCover(), CoverType.FRONT);
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
    public TranslatedBook updateTranslatedBook(UUID id, UpdateTranslatedBookRequest request) {
        TranslatedBook book = translatedBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TranslatedBook", id));

        // ISBN 검증 및 중복 검사 (변경된 경우)
        if (request.isbn() != null) {
            String normalizedIsbn = isbnValidator.normalizeIsbn(request.isbn());
            if (!normalizedIsbn.equals(book.getIsbn())) {
                if (!isbnValidator.isValid(normalizedIsbn)) {
                    throw new ValidationException("Invalid ISBN format", Map.of("isbn", request.isbn()));
                }
                checkIsbnDuplicate(normalizedIsbn);
                book.setIsbn(normalizedIsbn);
            }
        }

        if (request.title() != null) book.setTitle(request.title());
        if (request.countryCode() != null) book.setCountryCode(request.countryCode());
        if (request.languageCode() != null) book.setLanguageCode(request.languageCode());
        if (request.year() != null) book.setYear(request.year());
        if (request.edition() != null) book.setEdition(request.edition());

        if (request.authorId() != null) {
            Author author = authorRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author", request.authorId()));
            book.setAuthor(author);
        }

        if (request.translatorId() != null) {
            Translator translator = translatorRepository.findById(request.translatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Translator", request.translatorId()));
            book.setTranslator(translator);
        }

        if (request.publisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.publisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher", request.publisherId()));
            book.setPublisher(publisher);
        }

        if (request.designerId() != null) {
            Designer designer = designerRepository.findById(request.designerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designer", request.designerId()));
            book.setDesigner(designer);
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
        TranslatedBook savedBook = translatedBookRepository.save(book);

        // 콘텐츠 수정 이벤트 발행
        UUID userId = book.getCreatedBy() != null ? book.getCreatedBy().getId() : null;
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.TRANSLATED_BOOK_UPDATED,
                savedBook.getId(),
                userId
        ));

        return savedBook;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TranslatedBook> getTranslatedBook(UUID id) {
        return translatedBookRepository.findById(id);
    }

    @Override
    public void deleteTranslatedBook(UUID id) {
        TranslatedBook book = translatedBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TranslatedBook", id));
        book.setStatus(ContentStatus.DELETED);
        book.setUpdatedAt(Instant.now());
        translatedBookRepository.save(book);

        // 콘텐츠 삭제 이벤트 발행
        UUID userId = book.getCreatedBy() != null ? book.getCreatedBy().getId() : null;
        eventPublisher.publishContentEvent(new ContentEvent(
                ContentEventType.TRANSLATED_BOOK_DELETED,
                id,
                userId
        ));
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
    public Page<OriginalBook> getBooksByDesigner(UUID designerId, Pageable pageable) {
        return originalBookRepository.findByDesignerId(designerId, pageable);
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


    // Private helper methods

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
        return cover;
    }

    private BookCover createBookCover(CreateTranslatedBookRequest.BookCoverInfo info, CoverType coverType) {
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
        return cover;
    }

    private void updateBookCover(BookCover cover, UpdateOriginalBookRequest.BookCoverUpdateInfo info) {
        if (info.imageUrl() != null) cover.setImageUrl(info.imageUrl());
        if (info.thumbnailUrl() != null) cover.setThumbnailUrl(info.thumbnailUrl());
        if (info.originalFileName() != null) cover.setOriginalFileName(info.originalFileName());
        if (info.width() != null) cover.setWidth(info.width());
        if (info.height() != null) cover.setHeight(info.height());
        if (info.fileSize() != null) cover.setFileSize(info.fileSize());
        if (info.imageSource() != null) cover.setImageSource(info.imageSource());
        if (info.sourceUrl() != null) cover.setSourceUrl(info.sourceUrl());
    }

    private void updateBookCover(BookCover cover, UpdateTranslatedBookRequest.BookCoverUpdateInfo info) {
        if (info.imageUrl() != null) cover.setImageUrl(info.imageUrl());
        if (info.thumbnailUrl() != null) cover.setThumbnailUrl(info.thumbnailUrl());
        if (info.originalFileName() != null) cover.setOriginalFileName(info.originalFileName());
        if (info.width() != null) cover.setWidth(info.width());
        if (info.height() != null) cover.setHeight(info.height());
        if (info.fileSize() != null) cover.setFileSize(info.fileSize());
        if (info.imageSource() != null) cover.setImageSource(info.imageSource());
        if (info.sourceUrl() != null) cover.setSourceUrl(info.sourceUrl());
    }
}
