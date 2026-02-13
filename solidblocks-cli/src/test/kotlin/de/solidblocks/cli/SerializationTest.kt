package de.solidblocks.cli

import java.lang.reflect.Modifier
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

@Serializable
data class SerializationConfig(
    val name: String,
    val fields: List<SerializationConfigField>,
    val methods: List<SerializationConfigMethod>,
)

@Serializable data class SerializationConfigField(val name: String)

@Serializable
data class SerializationConfigMethod(
    val name: String,
    val parameterTypes: List<String> = emptyList(),
)

class SerializationConfigGenerator {

  fun findSerializableClasses(packageName: String): Set<Class<*>> {
    val reflections =
        Reflections(
            ConfigurationBuilder().forPackages(packageName).addScanners(Scanners.TypesAnnotated),
        )

    return reflections
        .getTypesAnnotatedWith(Serializable::class.java)
        .filter { !Modifier.isAbstract(it.modifiers) }
        .toSet()
  }

  @Test
  @Tag("generate")
  fun generateConfig() {
    val config =
        findSerializableClasses("de.solidblocks").flatMap {
          listOf(
              SerializationConfig(
                  it.name,
                  listOf(SerializationConfigField("Companion")),
                  emptyList(),
              ),
              SerializationConfig(
                  "${it.name}\$Companion",
                  emptyList(),
                  listOf(
                      SerializationConfigMethod("serializer"),
                  ),
              ),
          )
        }

    val reflectConfigFile =
        Path("").resolve("src/main/resources/META-INF/native-image/reflect-config.json")

    println("writing reflect config to '$reflectConfigFile'")
    reflectConfigFile.writeText(Json.encodeToString(config))
  }
}
