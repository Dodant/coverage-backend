package com.bookcover.archive.config

import io.kotest.core.config.AbstractProjectConfig

class KotestProjectConfig : AbstractProjectConfig() {
    override val parallelism = 1
}
