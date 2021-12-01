package de.solidblocks.config

import de.solidblocks.cloud.config.DbConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan(basePackages = ["de.solidblocks.cloud.config"])
@Import(DbConfiguration::class)
open class TestConfiguration
