package de.solidblocks.debug.container

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class SolidblocksDebugContainerApplication

fun main(args: Array<String>) {
  runApplication<SolidblocksDebugContainerApplication>(*args)
}
