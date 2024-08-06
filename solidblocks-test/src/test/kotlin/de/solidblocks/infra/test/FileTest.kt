import de.solidblocks.infra.test.tempDir
import de.solidblocks.infra.test.createZipFile
import org.junit.jupiter.api.Test

public class FileTest {

    @Test
    fun testExtractToDirectory() {
        val tempDir = tempDir()

        val zipFile = tempDir.createZipFile("test.zip")
            .addFile("file1.txt", "some content")
            .addFile("file2.txt", "some more content").create()

        /*
        # extract file to folder
        file_extract_to_directory "${TEMP_DIR}/file_extract_to_directory_$$/file.zip" "${TEMP_DIR}/file_extract_to_directory_$$"
        test_assert_file_exists "${TEMP_DIR}/file_extract_to_directory_$$/nomad"
        test_assert_file_exists "${TEMP_DIR}/file_extract_to_directory_$$/.file.zip.extracted"

        # ensure it is not extracted again when marker file is still present
        rm -f "${TEMP_DIR}/file_extract_to_directory_$$/nomad"
        file_extract_to_directory "${TEMP_DIR}/file_extract_to_directory_$$/file.zip" "${TEMP_DIR}/file_extract_to_directory_$$"

        test_assert_file_not_exists "${TEMP_DIR}/file_extract_to_directory_$$/nomad"
        test_assert_file_exists "${TEMP_DIR}/file_extract_to_directory_$$/.file.zip.extracted"
         */
    }
}