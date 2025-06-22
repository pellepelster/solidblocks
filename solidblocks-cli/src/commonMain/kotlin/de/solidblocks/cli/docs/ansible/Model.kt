package de.solidblocks.cli.docs.ansible

data class Default(val name: String, val value: String)

data class Variable(val name: String, val value: String?)

data class Role(val name: String, val defaults: List<Variable>?, val variables: List<Variable>?)

data class Collection(val galaxy: Galaxy)

data class Galaxy(val namespace: String, val name: String, val version: String, val description: String?)