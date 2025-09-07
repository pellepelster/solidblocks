package de.solidblocks.cli.docs.ansible

data class Default(val name: String, val value: String)

data class Variable(val name: String, val value: String?)

data class VariableTableRow(
    val name: String,
    val value: String?,
    val description: String?,
    val required: Boolean,
)

fun RoleMetaData.getOption(name: String) = this.options.singleOrNull { it.name == name }

fun List<Variable>.get(name: String) = this.singleOrNull { it.name == name }

fun Role.tableRows(): List<VariableTableRow> {
  val defaults =
      this.defaults.map {
        val option = metaData.getOption(it.name)
        VariableTableRow(
            it.name,
            option?.default ?: it.value,
            option?.description,
            option?.required ?: false,
        )
      }

  val variables =
      this.variables.map {
        val option = this.metaData.getOption(it.name)
        val default = this.defaults.get(it.name)

        VariableTableRow(
            it.name,
            option?.default ?: it.value ?: default?.value,
            option?.description,
            option?.required ?: false,
        )
      }

  val defaultsNotInVariables =
      defaults
          .filter { variables.none { v -> v.name == it.name } }
          .map {
            val option = metaData.getOption(it.name)
            val default = this.defaults.get(it.name)

            VariableTableRow(
                it.name,
                option?.default ?: default?.value,
                it.description,
                it.required,
            )
          }

  val variablesAndDefaults = variables + defaultsNotInVariables

  val optionsNotInVariablesOrDefaults =
      metaData.options
          .filter { option -> variablesAndDefaults.none { it.name == option.name } }
          .map { VariableTableRow(it.name, it.default, it.description, it.required) }

  return (variablesAndDefaults + optionsNotInVariablesOrDefaults).sortedBy { it.name }
}

fun List<VariableTableRow>.toMarkdownTableRow() =
    this.joinToString("\n") {
      "| ${it.name} | ${it.value ?: "&lt;none&gt;"} | ${it.description ?: "&lt;none&gt;"} | ${it.required}  |"
    }

fun emptyRow() = "| &lt;none&gt; | &lt;none&gt; | | &lt;none&gt; |"

data class Role(
    val name: String,
    val defaults: List<Variable>,
    val variables: List<Variable>,
    val metaData: RoleMetaData,
)

data class RoleMetaData(
    val shortDescription: String?,
    val description: String?,
    val options: List<Option>,
)

data class Option(
    val name: String,
    val description: String?,
    val required: Boolean,
    val default: String?,
    val type: String?,
)

data class Collection(val galaxy: Galaxy)

data class Galaxy(
    val namespace: String,
    val name: String,
    val version: String,
    val description: String?,
)
