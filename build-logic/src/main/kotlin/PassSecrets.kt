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
    val path: Property<String>
    val environment: Property<String>
}

val secretCache = mutableMapOf<String, String>()

abstract class PassSecretValueSource : ValueSource<String, PassSecretValueSourceParameters> {

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val path = parameters.path.get()
        return secretCache.computeIfAbsent(path) {
            val derivedEnvVarName = path.replace("/", "_").uppercase()
            val envVarName = parameters.environment.getOrElse(derivedEnvVarName)
            if (System.getenv(envVarName) != null) {
                System.getenv(envVarName)
            } else {
                val output = ByteArrayOutputStream()
                execOperations.exec {
                    commandLine("pass", path)
                    standardOutput = output
                }
                val secret = String(output.toByteArray(), Charset.defaultCharset()).trim()
                secret
            }
        }
    }
}
