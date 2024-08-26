package de.solidblocks.infra.test.files

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.should
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.readBytes
import kotlin.io.path.readText

infix fun DirectoryBuilder.shouldContainNFiles(fileCount: Int): DirectoryBuilder {
    this.files() shouldHaveSize fileCount
    return this
}

infix fun Path.shouldHaveChecksum(checksum: String) {
    val bytes = this.readBytes()
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes).fold("", { str, it -> str + "%02x".format(it) })

    digest shouldBeEqual checksum
}

infix fun Path.shouldHaveName(name: String) = this should haveName(name)

fun haveName(name: String) = object : Matcher<Path> {
    override fun test(value: Path): MatcherResult {
        val actual = value.fileName.toString()
        return MatcherResult(
            actual == name,
            { "Path $value should have name $name but was $actual" },
            {
                "Path $value should not have name of $name"
            })
    }
}

infix fun DirectoryBuilder.singleFile(file: String) = path.resolve(file)

infix fun DirectoryBuilder.matchSingleFile(regex: String) = files(regex).singleOrNull() ?: path

infix fun Path.shouldHaveContent(content: String) = this should haveContent(content)

fun haveContent(content: String) = object : Matcher<Path> {
    override fun test(value: Path): MatcherResult {
        val actual = value.readText()
        return MatcherResult(
            actual == content,
            { "Path $value should have content '$content' but was '$actual'" },
            {
                "Path $value should not have content '$content'"
            })
    }
}



