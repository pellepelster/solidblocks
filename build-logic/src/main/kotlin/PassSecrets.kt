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

abstract class PassSecretValueSource : ValueSource<String, PassSecretValueSourceParameters> {

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val path = parameters.path.get()

        val derivedEnvVarName = path.replace("/", "_").uppercase()
        val envVarName = parameters.environment.getOrElse(derivedEnvVarName)

        return if (System.getenv(envVarName) != null) {
            println("using environment variable '${envVarName}' instead of pass secret from '$path'")

            System.getenv(envVarName)
        } else {
            println("retrieving pass secret from '$path'")

            val output = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("pass", path)
                standardOutput = output
            }

            return String(output.toByteArray(), Charset.defaultCharset()).trim()
        }
    }
}
