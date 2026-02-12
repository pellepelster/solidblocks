package de.solidblocks.shell.test

import de.solidblocks.shell.StorageLibrary
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

public class StorageTest {

  @Test
  fun testLibrarySource() {
    StorageLibrary.source() shouldContain "storage_mount"
  }
}
