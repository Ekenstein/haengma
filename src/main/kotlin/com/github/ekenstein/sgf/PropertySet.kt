package com.github.ekenstein.sgf

/**
 * Represents a set of [SgfProperty]. This set guarantees that it contains unique types of [SgfProperty].
 * E.g. a [PropertySet] can not contain two [SgfProperty.Move.B].
 */
class PropertySet internal constructor(private val properties: Map<String, SgfProperty>) : AbstractSet<SgfProperty>() {
    override val size: Int = properties.size

    /**
     * Checks if the current set contains a property of the same type as [element].
     * True if there is a property of the same type, otherwise false.
     *
     * E.g. propertySetOf(SgfProperty.Move.B.pass()).contains(SgfProperty.Move.B(3, 3)) == true
     */
    override fun contains(element: SgfProperty): Boolean = properties.containsKey(element.identifier)

    override fun iterator(): Iterator<SgfProperty> = properties.values.iterator()

    /**
     * Creates a new [PropertySet] by replacing or adding the given [property] to the current set of [SgfProperty].
     *
     * If there is a property of same type as the given [property] that property will be replaced
     * by the given [property], otherwise the given [property] will be appended to the current set of properties.
     */
    operator fun plus(property: SgfProperty) = PropertySet(
        properties = properties + (property.identifier to property)
    )

    /**
     * Creates a new [PropertySet] by replacing or adding the given [set] of [SgfProperty] onto the current set of
     * [SgfProperty].
     */
    operator fun plus(set: PropertySet) = PropertySet(
        properties = properties + set.properties
    )

    /**
     * Creates a new [PropertySet] by removing the given [property] from the current set.
     *
     * If there is no property in the current set with the same type as the given [property], the new [PropertySet]
     * will contain the same properties at the current property set.
     */
    operator fun minus(property: SgfProperty) = PropertySet(
        properties = properties - property.identifier
    )

    operator fun minus(properties: Set<SgfProperty>): PropertySet {
        val propertiesToRemove = properties.map { it.identifier }.toSet()

        return PropertySet(
            properties = this.properties - propertiesToRemove
        )
    }
}

/**
 * Returns a [PropertySet] containing the given [properties]. If there are any duplicates in
 * the given collection of [properties], e.g. two [SgfProperty.Move.B], the last property of that type
 * will be added to the [PropertySet].
 */
fun propertySetOf(vararg properties: SgfProperty) = PropertySet(properties.associateBy { it.identifier })

/**
 * Returns an empty [PropertySet].
 */
fun emptyPropertySet() = PropertySet(emptyMap())

/**
 * Returns a [PropertySet] containing the given [properties] which are not null.
 *
 * If any of the given [properties] that aren't null are duplicates, e.g. two [SgfProperty.Move.B], the last
 * property of that type will be added to the [PropertySet]
 */
fun propertySetOfNotNull(vararg properties: SgfProperty?) = properties.filterNotNull().toPropertySet()

/**
 * Converts the given [Iterable] to a [PropertySet]. If any of the properties in the iterable are duplicates,
 * e.g. two [SgfProperty.Move.B], the last property of that type will be added to the [PropertySet].
 */
fun Iterable<SgfProperty>.toPropertySet() = PropertySet(
    properties = associateBy { it.identifier }
)
