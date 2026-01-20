package com.bookcover.archive

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BookCoverArchiveApplicationTests : FunSpec({

    test("application main class exists") {
        // Verify the main application class is properly configured
        val appClass = BookCoverArchiveApplication::class
        appClass.simpleName shouldBe "BookCoverArchiveApplication"
    }
})
