package com.bookcover.archive.service.impl

import com.bookcover.archive.domain.book.BookCover
import com.bookcover.archive.domain.book.OriginalBook
import com.bookcover.archive.domain.book.TranslatedBook
import com.bookcover.archive.domain.common.ContentStatus
import com.bookcover.archive.domain.contributor.Author
import com.bookcover.archive.domain.contributor.Designer
import com.bookcover.archive.domain.contributor.Publisher
import com.bookcover.archive.domain.contributor.Translator
import com.bookcover.archive.domain.media.Media
import com.bookcover.archive.exception.DuplicateIsbnException
import com.bookcover.archive.exception.ResourceNotFoundException
import com.bookcover.archive.exception.ValidationException
import com.bookcover.archive.repository.book.OriginalBookCustomRepository
import com.bookcover.archive.repository.book.OriginalBookRepository
import com.bookcover.archive.repository.book.TranslatedBookRepository
import com.bookcover.archive.repository.contributor.AuthorRepository
import com.bookcover.archive.repository.contributor.DesignerRepository
import com.bookcover.archive.repository.contributor.PublisherRepository
import com.bookcover.archive.repository.contributor.TranslatorRepository
import com.bookcover.archive.repository.user.UserRepository
import com.bookcover.archive.repository.work.WorkRepository
import com.bookcover.archive.service.BookService
import com.bookcover.archive.service.dto.CreateBookCoverRequest
import com.bookcover.archive.service.dto.CreateOriginalBookRequest
import com.bookcover.archive.service.dto.CreateTranslatedBookRequest
import com.bookcover.archive.service.dto.DuplicateCheckResult
import com.bookcover.archive.service.dto.DuplicateType
import com.bookcover.archive.service.dto.SimilarBookInfo
import com.bookcover.archive.service.dto.UpdateOriginalBookRequest
import com.bookcover.archive.service.dto.UpdateTranslatedBookRequest
import com.bookcover.archive.validation.IsbnValidator
import com.bookcover.archive.validation.LocaleValidator
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class BookServiceImpl(
    private val originalBookRepository: OriginalBookRepository,
    private val originalBookCustomRepository: OriginalBookCustomRepository,
    private val translatedBookRepository: TranslatedBookRepository,
    private val authorRepository: AuthorRepository,
    private val publisherRepository: PublisherRepository,
    private val designerRepository: DesignerRepository,
    private val translatorRepository: TranslatorRepository,
    private val workRepository: WorkRepository,
    private val userRepository: UserRepository,
    private val contributionService: com.bookcover.archive.service.ContributionService
) : BookService {

    override fun createOriginalBook(request: CreateOriginalBookRequest, createdById: UUID): OriginalBook {
        validateOriginalBookRequest(request)

        request.isbn?.let { isbn ->
            val normalizedIsbn = IsbnValidator.normalizeIsbn(isbn)
            checkIsbnDuplicate(normalizedIsbn)
        }

        val author = resolveAuthor(request.authorId, request.authorName)
        val publisher = resolvePublisher(request.publisherId, request.publisherName)
        val designer = resolveDesigner(request.designerId, request.designerName)

        val work = request.workId?.let { workId ->
            workRepository.findById(workId).orElseThrow {
                ResourceNotFoundException("Work", workId)
            }
        }

        val frontCover = createBookCover(request.frontCover)
        val backCover = request.backCover?.let { createBookCover(it) }

        val originalBook = OriginalBook(
            title = request.title.trim(),
            originalTitle = request.originalTitle.trim(),
            countryCode = LocaleValidator.normalizeCountryCode(request.countryCode),
            languageCode = LocaleValidator.normalizeLanguageCode(request.languageCode),
            year = request.year,
            genre = request.genre.trim(),
            isbn = request.isbn?.let { IsbnValidator.normalizeIsbn(it) },
            author = author,
            publisher = publisher,
            designer = designer,
            edition = request.edition?.trim(),
            work = work,
            frontCover = frontCover,
            backCover = backCover,
            createdById = createdById
        )

        val savedBook = originalBookRepository.save(originalBook)

        author.bookCount++
        authorRepository.save(author)

        publisher.bookCount++
        publisherRepository.save(publisher)

        designer?.let {
            it.coverCount++
            designerRepository.save(it)
        }

        work?.let {
            it.incrementEditionCount()
            workRepository.save(it)
        }

        // Track user contribution for title eligibility (Requirement 7.5, 7.6)
        contributionService.recordContribution(createdById)

        return savedBook
    }

    override fun updateOriginalBook(id: UUID, request: UpdateOriginalBookRequest): OriginalBook {
        val book = originalBookRepository.findById(id).orElseThrow {
            ResourceNotFoundException("OriginalBook", id)
        }

        request.title?.let { book.title = it.trim() }
        request.originalTitle?.let { book.originalTitle = it.trim() }
        request.genre?.let { book.genre = it.trim() }
        request.edition?.let { book.edition = it.trim() }

        request.isbn?.let { newIsbn ->
            val normalizedIsbn = IsbnValidator.normalizeIsbn(newIsbn)
            if (normalizedIsbn != book.isbn) {
                val isbnValidation = IsbnValidator.validate(normalizedIsbn)
                if (!isbnValidation.valid) {
                    throw ValidationException("Invalid ISBN format", mapOf("isbn" to (isbnValidation.errorMessage ?: "Invalid")))
                }
                checkIsbnDuplicate(normalizedIsbn, excludeBookId = id)
                book.isbn = normalizedIsbn
            }
        }

        request.authorId?.let { authorId ->
            val oldAuthor = book.author
            val newAuthor = authorRepository.findById(authorId).orElseThrow {
                ResourceNotFoundException("Author", authorId)
            }
            if (oldAuthor.id != newAuthor.id) {
                oldAuthor.bookCount--
                authorRepository.save(oldAuthor)
                newAuthor.bookCount++
                authorRepository.save(newAuthor)
                book.author = newAuthor
            }
        }

        request.publisherId?.let { publisherId ->
            val oldPublisher = book.publisher
            val newPublisher = publisherRepository.findById(publisherId).orElseThrow {
                ResourceNotFoundException("Publisher", publisherId)
            }
            if (oldPublisher.id != newPublisher.id) {
                oldPublisher.bookCount--
                publisherRepository.save(oldPublisher)
                newPublisher.bookCount++
                publisherRepository.save(newPublisher)
                book.publisher = newPublisher
            }
        }

        request.designerId?.let { designerId ->
            val oldDesigner = book.designer
            val newDesigner = designerRepository.findById(designerId).orElseThrow {
                ResourceNotFoundException("Designer", designerId)
            }
            if (oldDesigner?.id != newDesigner.id) {
                oldDesigner?.let {
                    it.coverCount--
                    designerRepository.save(it)
                }
                newDesigner.coverCount++
                designerRepository.save(newDesigner)
                book.designer = newDesigner
            }
        }

        request.workId?.let { workId ->
            val oldWork = book.work
            val newWork = workRepository.findById(workId).orElseThrow {
                ResourceNotFoundException("Work", workId)
            }
            if (oldWork?.id != newWork.id) {
                oldWork?.let {
                    it.decrementEditionCount()
                    workRepository.save(it)
                }
                newWork.incrementEditionCount()
                workRepository.save(newWork)
                book.work = newWork
            }
        }

        return originalBookRepository.save(book)
    }

    @Transactional(readOnly = true)
    override fun getOriginalBook(id: UUID): OriginalBook? {
        return originalBookRepository.findById(id).orElse(null)
    }

    override fun deleteOriginalBook(id: UUID) {
        val book = originalBookRepository.findById(id).orElseThrow {
            ResourceNotFoundException("OriginalBook", id)
        }

        book.status = ContentStatus.DELETED

        book.author.bookCount--
        authorRepository.save(book.author)

        book.publisher.bookCount--
        publisherRepository.save(book.publisher)

        book.designer?.let {
            it.coverCount--
            designerRepository.save(it)
        }

        book.work?.let {
            it.decrementEditionCount()
            workRepository.save(it)
        }

        originalBookRepository.save(book)
    }

    override fun createTranslatedBook(request: CreateTranslatedBookRequest, createdById: UUID): TranslatedBook {
        validateTranslatedBookRequest(request)

        val originalBook = originalBookRepository.findById(request.originalBookId).orElseThrow {
            ResourceNotFoundException("OriginalBook", request.originalBookId)
        }

        request.isbn?.let { isbn ->
            val normalizedIsbn = IsbnValidator.normalizeIsbn(isbn)
            checkIsbnDuplicate(normalizedIsbn)
        }

        val author = request.authorId?.let { authorId ->
            authorRepository.findById(authorId).orElseThrow {
                ResourceNotFoundException("Author", authorId)
            }
        } ?: originalBook.author

        val translator = resolveTranslator(request.translatorId, request.translatorName)
        val publisher = resolvePublisher(request.publisherId, request.publisherName)

        val frontCover = createBookCover(request.frontCover)
        val backCover = request.backCover?.let { createBookCover(it) }

        val translatedBook = TranslatedBook(
            title = request.title.trim(),
            countryCode = LocaleValidator.normalizeCountryCode(request.countryCode),
            languageCode = LocaleValidator.normalizeLanguageCode(request.languageCode),
            year = request.year,
            isbn = request.isbn?.let { IsbnValidator.normalizeIsbn(it) },
            originalBook = originalBook,
            author = author,
            translator = translator,
            publisher = publisher,
            edition = request.edition?.trim(),
            frontCover = frontCover,
            backCover = backCover,
            createdById = createdById
        )

        val savedBook = translatedBookRepository.save(translatedBook)
        originalBook.addTranslation(savedBook)

        if (author.id != originalBook.author.id) {
            author.bookCount++
            authorRepository.save(author)
        }

        translator.bookCount++
        translatorRepository.save(translator)

        publisher.bookCount++
        publisherRepository.save(publisher)

        // Track user contribution for title eligibility (Requirement 7.5, 7.6)
        contributionService.recordContribution(createdById)

        return savedBook
    }

    override fun updateTranslatedBook(id: UUID, request: UpdateTranslatedBookRequest): TranslatedBook {
        val book = translatedBookRepository.findById(id).orElseThrow {
            ResourceNotFoundException("TranslatedBook", id)
        }

        request.title?.let { book.title = it.trim() }
        request.edition?.let { book.edition = it.trim() }

        request.isbn?.let { newIsbn ->
            val normalizedIsbn = IsbnValidator.normalizeIsbn(newIsbn)
            if (normalizedIsbn != book.isbn) {
                val isbnValidation = IsbnValidator.validate(normalizedIsbn)
                if (!isbnValidation.valid) {
                    throw ValidationException("Invalid ISBN format", mapOf("isbn" to (isbnValidation.errorMessage ?: "Invalid")))
                }
                checkIsbnDuplicate(normalizedIsbn, excludeBookId = id)
                book.isbn = normalizedIsbn
            }
        }

        request.translatorId?.let { translatorId ->
            val oldTranslator = book.translator
            val newTranslator = translatorRepository.findById(translatorId).orElseThrow {
                ResourceNotFoundException("Translator", translatorId)
            }
            if (oldTranslator.id != newTranslator.id) {
                oldTranslator.bookCount--
                translatorRepository.save(oldTranslator)
                newTranslator.bookCount++
                translatorRepository.save(newTranslator)
                book.translator = newTranslator
            }
        }

        request.publisherId?.let { publisherId ->
            val oldPublisher = book.publisher
            val newPublisher = publisherRepository.findById(publisherId).orElseThrow {
                ResourceNotFoundException("Publisher", publisherId)
            }
            if (oldPublisher.id != newPublisher.id) {
                oldPublisher.bookCount--
                publisherRepository.save(oldPublisher)
                newPublisher.bookCount++
                publisherRepository.save(newPublisher)
                book.publisher = newPublisher
            }
        }

        return translatedBookRepository.save(book)
    }

    @Transactional(readOnly = true)
    override fun getTranslatedBook(id: UUID): TranslatedBook? {
        return translatedBookRepository.findById(id).orElse(null)
    }

    override fun deleteTranslatedBook(id: UUID) {
        val book = translatedBookRepository.findById(id).orElseThrow {
            ResourceNotFoundException("TranslatedBook", id)
        }

        book.status = ContentStatus.DELETED

        book.translator.bookCount--
        translatorRepository.save(book.translator)

        book.publisher.bookCount--
        publisherRepository.save(book.publisher)

        translatedBookRepository.save(book)
    }

    @Transactional(readOnly = true)
    override fun getTranslationsByOriginalBook(originalBookId: UUID): List<TranslatedBook> {
        return translatedBookRepository.findByOriginalBookId(originalBookId)
    }

    @Transactional(readOnly = true)
    override fun getBooksByAuthor(authorId: UUID, pageable: Pageable): Page<Media> {
        val originalBooks = originalBookRepository.findByAuthorId(authorId, pageable)
        val translatedBooks = translatedBookRepository.findByAuthorId(authorId, pageable)

        val allBooks = mutableListOf<Media>()
        allBooks.addAll(originalBooks.content)
        allBooks.addAll(translatedBooks.content)
        allBooks.sortByDescending { it.createdAt }

        val totalCount = originalBooks.totalElements + translatedBooks.totalElements
        return PageImpl(allBooks.take(pageable.pageSize), pageable, totalCount)
    }

    @Transactional(readOnly = true)
    override fun getBooksByPublisher(publisherId: UUID, pageable: Pageable): Page<Media> {
        val originalBooks = originalBookRepository.findByPublisherId(publisherId, pageable)
        val translatedBooks = translatedBookRepository.findByPublisherId(publisherId, pageable)

        val allBooks = mutableListOf<Media>()
        allBooks.addAll(originalBooks.content)
        allBooks.addAll(translatedBooks.content)
        allBooks.sortByDescending { it.createdAt }

        val totalCount = originalBooks.totalElements + translatedBooks.totalElements
        return PageImpl(allBooks.take(pageable.pageSize), pageable, totalCount)
    }

    @Transactional(readOnly = true)
    override fun getBooksByDesigner(designerId: UUID, pageable: Pageable): Page<OriginalBook> {
        return originalBookRepository.findByDesignerId(designerId, pageable)
    }

    @Transactional(readOnly = true)
    override fun checkDuplicateByIsbn(isbn: String): DuplicateCheckResult {
        val normalizedIsbn = IsbnValidator.normalizeIsbn(isbn)

        val originalBook = originalBookRepository.findByIsbn(normalizedIsbn)
        if (originalBook != null) {
            return DuplicateCheckResult(
                hasDuplicate = true,
                duplicateType = DuplicateType.EXACT_ISBN_MATCH,
                existingBookId = originalBook.id,
                existingBookTitle = originalBook.title,
                existingBookType = "OriginalBook"
            )
        }

        val translatedBook = translatedBookRepository.findByIsbn(normalizedIsbn)
        if (translatedBook != null) {
            return DuplicateCheckResult(
                hasDuplicate = true,
                duplicateType = DuplicateType.EXACT_ISBN_MATCH,
                existingBookId = translatedBook.id,
                existingBookTitle = translatedBook.title,
                existingBookType = "TranslatedBook"
            )
        }

        return DuplicateCheckResult(hasDuplicate = false)
    }

    @Transactional(readOnly = true)
    override fun findSimilarBooks(title: String, authorName: String, year: Int): DuplicateCheckResult {
        val similarOriginalBooks = originalBookCustomRepository.findPotentialDuplicates(
            title, authorName, year, Pageable.ofSize(10)
        )

        val similarBooks = similarOriginalBooks.content.map { book ->
            SimilarBookInfo(
                id = book.id,
                title = book.title,
                authorName = book.author.name,
                year = book.year,
                type = "OriginalBook"
            )
        }

        return DuplicateCheckResult(
            hasDuplicate = similarBooks.isNotEmpty(),
            duplicateType = if (similarBooks.isNotEmpty()) DuplicateType.SIMILAR_TITLE_AUTHOR_YEAR else null,
            similarBooks = similarBooks
        )
    }

    override fun linkBookToWork(bookId: UUID, workId: UUID): OriginalBook {
        val book = originalBookRepository.findById(bookId).orElseThrow {
            ResourceNotFoundException("OriginalBook", bookId)
        }

        val work = workRepository.findById(workId).orElseThrow {
            ResourceNotFoundException("Work", workId)
        }

        book.work?.let { oldWork ->
            oldWork.decrementEditionCount()
            workRepository.save(oldWork)
        }

        book.work = work
        work.incrementEditionCount()
        workRepository.save(work)

        return originalBookRepository.save(book)
    }

    private fun validateOriginalBookRequest(request: CreateOriginalBookRequest) {
        val errors = mutableMapOf<String, String>()

        if (request.title.isBlank()) {
            errors["title"] = "Title is required"
        }
        if (request.originalTitle.isBlank()) {
            errors["originalTitle"] = "Original title is required"
        }
        if (request.genre.isBlank()) {
            errors["genre"] = "Genre is required"
        }

        val countryValidation = LocaleValidator.validateCountryCode(request.countryCode)
        if (!countryValidation.valid) {
            errors["countryCode"] = countryValidation.errorMessage ?: "Invalid country code"
        }

        val languageValidation = LocaleValidator.validateLanguageCode(request.languageCode)
        if (!languageValidation.valid) {
            errors["languageCode"] = languageValidation.errorMessage ?: "Invalid language code"
        }

        val yearValidation = LocaleValidator.validateYear(request.year)
        if (!yearValidation.valid) {
            errors["year"] = yearValidation.errorMessage ?: "Invalid year"
        }

        request.isbn?.let { isbn ->
            val isbnValidation = IsbnValidator.validate(isbn)
            if (!isbnValidation.valid) {
                errors["isbn"] = isbnValidation.errorMessage ?: "Invalid ISBN"
            }
        }

        if (request.authorId == null && request.authorName.isNullOrBlank()) {
            errors["author"] = "Either authorId or authorName must be provided"
        }

        if (request.publisherId == null && request.publisherName.isNullOrBlank()) {
            errors["publisher"] = "Either publisherId or publisherName must be provided"
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed for original book creation", errors)
        }
    }

    private fun validateTranslatedBookRequest(request: CreateTranslatedBookRequest) {
        val errors = mutableMapOf<String, String>()

        if (request.title.isBlank()) {
            errors["title"] = "Title is required"
        }

        val countryValidation = LocaleValidator.validateCountryCode(request.countryCode)
        if (!countryValidation.valid) {
            errors["countryCode"] = countryValidation.errorMessage ?: "Invalid country code"
        }

        val languageValidation = LocaleValidator.validateLanguageCode(request.languageCode)
        if (!languageValidation.valid) {
            errors["languageCode"] = languageValidation.errorMessage ?: "Invalid language code"
        }

        val yearValidation = LocaleValidator.validateYear(request.year)
        if (!yearValidation.valid) {
            errors["year"] = yearValidation.errorMessage ?: "Invalid year"
        }

        request.isbn?.let { isbn ->
            val isbnValidation = IsbnValidator.validate(isbn)
            if (!isbnValidation.valid) {
                errors["isbn"] = isbnValidation.errorMessage ?: "Invalid ISBN"
            }
        }

        if (request.translatorId == null && request.translatorName.isNullOrBlank()) {
            errors["translator"] = "Either translatorId or translatorName must be provided"
        }

        if (request.publisherId == null && request.publisherName.isNullOrBlank()) {
            errors["publisher"] = "Either publisherId or publisherName must be provided"
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Validation failed for translated book creation", errors)
        }
    }

    private fun checkIsbnDuplicate(isbn: String, excludeBookId: UUID? = null) {
        val originalBook = originalBookRepository.findByIsbn(isbn)
        if (originalBook != null && originalBook.id != excludeBookId) {
            throw DuplicateIsbnException(isbn, originalBook.id, "OriginalBook")
        }

        val translatedBook = translatedBookRepository.findByIsbn(isbn)
        if (translatedBook != null && translatedBook.id != excludeBookId) {
            throw DuplicateIsbnException(isbn, translatedBook.id, "TranslatedBook")
        }
    }

    private fun resolveAuthor(authorId: UUID?, authorName: String?): Author {
        return when {
            authorId != null -> authorRepository.findById(authorId).orElseThrow {
                ResourceNotFoundException("Author", authorId)
            }
            !authorName.isNullOrBlank() -> {
                authorRepository.findByNameIgnoreCase(authorName.trim())
                    ?: authorRepository.save(Author(name = authorName.trim()))
            }
            else -> throw ValidationException("Author information is required")
        }
    }

    private fun resolvePublisher(publisherId: UUID?, publisherName: String?): Publisher {
        return when {
            publisherId != null -> publisherRepository.findById(publisherId).orElseThrow {
                ResourceNotFoundException("Publisher", publisherId)
            }
            !publisherName.isNullOrBlank() -> {
                publisherRepository.findByNameIgnoreCase(publisherName.trim())
                    ?: publisherRepository.save(Publisher(name = publisherName.trim()))
            }
            else -> throw ValidationException("Publisher information is required")
        }
    }

    private fun resolveDesigner(designerId: UUID?, designerName: String?): Designer? {
        return when {
            designerId != null -> designerRepository.findById(designerId).orElseThrow {
                ResourceNotFoundException("Designer", designerId)
            }
            !designerName.isNullOrBlank() -> {
                designerRepository.findByNameIgnoreCase(designerName.trim())
                    ?: designerRepository.save(Designer(name = designerName.trim()))
            }
            else -> null
        }
    }

    private fun resolveTranslator(translatorId: UUID?, translatorName: String?): Translator {
        return when {
            translatorId != null -> translatorRepository.findById(translatorId).orElseThrow {
                ResourceNotFoundException("Translator", translatorId)
            }
            !translatorName.isNullOrBlank() -> {
                translatorRepository.findByNameIgnoreCase(translatorName.trim())
                    ?: translatorRepository.save(Translator(name = translatorName.trim()))
            }
            else -> throw ValidationException("Translator information is required")
        }
    }

    private fun createBookCover(request: CreateBookCoverRequest): BookCover {
        return BookCover(
            imageUrl = request.imageUrl,
            thumbnailUrl = request.thumbnailUrl,
            originalFileName = request.originalFileName,
            width = request.width,
            height = request.height,
            fileSize = request.fileSize,
            coverType = request.coverType,
            imageSource = request.imageSource,
            sourceUrl = request.sourceUrl,
            dominantColor = request.dominantColor
        )
    }
}
