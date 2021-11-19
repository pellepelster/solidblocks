package de.solidblocks.core

interface IHealthcheck {
    fun check(resource: IResource): Boolean
}