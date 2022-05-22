package com.github.ekenstein.sgf

class PropertySet internal constructor(private val properties: Map<String, SgfProperty>) : AbstractSet<SgfProperty>() {
    override val size: Int = properties.size

    override fun contains(element: SgfProperty): Boolean = properties.containsKey(element.identifier)

    override fun iterator(): Iterator<SgfProperty> = properties.values.iterator()

    operator fun plus(property: SgfProperty) = PropertySet(
        properties = properties + (property.identifier to property)
    )

    operator fun plus(set: PropertySet) = PropertySet(
        properties = properties + set.properties
    )

    operator fun minus(property: SgfProperty) = PropertySet(
        properties = properties - property.identifier
    )
}

fun propertySetOf(vararg property: SgfProperty) = PropertySet(property.associateBy { it.identifier })
fun emptyPropertySet() = PropertySet(emptyMap())
fun propertySetOfNotNull(vararg property: SgfProperty?) = property.filterNotNull().toPropertySet()

fun Iterable<SgfProperty>.toPropertySet() = PropertySet(
    properties = associateBy { it.identifier }
)
