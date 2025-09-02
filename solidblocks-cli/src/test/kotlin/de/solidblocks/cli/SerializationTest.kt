package de.solidblocks.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.lang.reflect.Modifier

@Serializable
data class SerializationConfig(
    val name: String,
    val fields: List<SerializationConfigField>,
    val methods: List<SerializationConfigMethod>
)

@Serializable
data class SerializationConfigField(val name: String)

@Serializable
data class SerializationConfigMethod(val name: String, val parameterTypes: List<String> = emptyList())

class SerializationTest {

    fun findSerializableClasses(packageName: String): Set<Class<*>> {
        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackages(packageName)
                .addScanners(Scanners.TypesAnnotated)
        )

        return reflections.getTypesAnnotatedWith(Serializable::class.java)
            .filter { !Modifier.isAbstract(it.modifiers) }
            .toSet()
    }

    @Test
    fun generateConfig() {
        val config = findSerializableClasses("de.solidblocks.cli.hetzner").flatMap {
            listOf(

                SerializationConfig(
                    it.name, listOf(SerializationConfigField("Companion")), emptyList()
                ),
                SerializationConfig(
                    "${it.name}\$Companion", emptyList(), listOf(
                        SerializationConfigMethod("serializer")
                    )
                ),
            )
        }

        println(Json.encodeToString(config))
    }
}

