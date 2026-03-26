package de.solidblocks.cloud

public infix fun <T> List<T>.equalsIgnoreOrder(other: List<T>) =
    this.size == other.size && this.toSet() == other.toSet()

public fun <T> Iterable<T>.joinToStringOrEmpty(
    separator: CharSequence = ", ",
    empty: String = "<none>",
    transform: (T) -> CharSequence,
): String =
    if (this.count() == 0) {
      empty
    } else {
      this.joinToStringOrEmpty(separator) { transform.invoke(it) }
    }
