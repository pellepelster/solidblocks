package de.solidblocks.cli.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile

@SpringBootApplication(scanBasePackages = ["de.solidblocks"])
@Profile("!CloudCreate")
open class CliApplication
