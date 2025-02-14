import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.inject.Inject

public interface PassSecretValueSourceParameters : ValueSourceParameters {
    @get:Input
    val passName: Property<String>

}

val secretCache = mutableMapOf<String, String>()

abstract class PassSecretValueSource : ValueSource<String, PassSecretValueSourceParameters> {

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val passName = parameters.passName.get()
        return secretCache.computeIfAbsent(passName) {
            if (System.getenv("CI") != null) {
                val envVarName = passName.replace("/", "_").uppercase()
                if (System.getenv(envVarName) != null) {
                    System.getenv(envVarName)
                } else {
                    throw RuntimeException("missing environment variable $envVarName")
                }
            } else {
                val output = ByteArrayOutputStream()
                execOperations.exec {
                    commandLine("pass", passName)
                    standardOutput = output
                }
                val secret = String(output.toByteArray(), Charset.defaultCharset()).trim()
                secret
            }
        }
    }
}
