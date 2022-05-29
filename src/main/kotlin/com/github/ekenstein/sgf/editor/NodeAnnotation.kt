package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.toPropertySet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class Remark {
    /**
     * The position is unclear.
     */
    object Unclear : Remark()

    /**
     * The position is even.
     */
    object Even : Remark()

    /**
     * Even result for both players and that this is a main variation of this opening.
     */
    object MainVariation : Remark()

    /**
     * Something good for [color].
     */
    data class Good(val color: SgfColor) : Remark()
}

private fun SgfNode.removeRemarks(): SgfNode = copy(
    properties = properties.filter {
        when (it) {
            is SgfProperty.NodeAnnotation.GB,
            is SgfProperty.NodeAnnotation.GW,
            is SgfProperty.NodeAnnotation.UC,
            is SgfProperty.NodeAnnotation.DM -> false
            else -> true
        }
    }.toPropertySet()
)

private fun SgfNode.addRemark(remark: Remark): SgfNode {
    val property = remark.toSgf()
    return removeRemarks().let {
        it.copy(properties = it.properties + property)
    }
}

private fun getRemarkFromNode(node: SgfNode): Remark? = listOfNotNull(
    node.property<SgfProperty.NodeAnnotation.GB>()?.let { Remark.Good(SgfColor.Black) },
    node.property<SgfProperty.NodeAnnotation.GW>()?.let { Remark.Good(SgfColor.White) },
    node.property<SgfProperty.NodeAnnotation.DM>()?.let {
        when (it.value) {
            SgfDouble.Normal -> Remark.Even
            SgfDouble.Emphasized -> Remark.MainVariation
        }
    },
    node.property<SgfProperty.NodeAnnotation.UC>()?.let { Remark.Unclear }
).singleOrNull()

private fun Remark.toSgf() = when (this) {
    Remark.Even -> SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)
    is Remark.Good -> when (color) {
        SgfColor.Black -> SgfProperty.NodeAnnotation.GB(SgfDouble.Normal)
        SgfColor.White -> SgfProperty.NodeAnnotation.GW(SgfDouble.Normal)
    }
    Remark.MainVariation -> SgfProperty.NodeAnnotation.DM(SgfDouble.Emphasized)
    Remark.Unclear -> SgfProperty.NodeAnnotation.UC(SgfDouble.Normal)
}

@DslMarker
private annotation class NodeAnnotationDslMarker

/**
 * Annotates the current node.
 */
@NodeAnnotationDslMarker
interface NodeAnnotationBuilder {
    /**
     * Sets a comment to the current node. Null if there is no comment.
     */
    var comment: String?

    /**
     * Whether the current node is a hotspot or not.
     */
    var isHotspot: Boolean

    /**
     * The name of the current node, if it has a name. Null if it hasn't a name.
     */
    var name: String?

    /**
     * Remark the position. E.g. that the position is even, or that something is good for black.
     */
    var remark: Remark?
}

private class NodeAnnotationBuilderImpl(var node: SgfNode) : NodeAnnotationBuilder {
    override var comment: String?
        get() = node.property<SgfProperty.NodeAnnotation.C>()?.comment
        set(value) {
            node = when (value) {
                null -> node.copy(properties = node.properties.removePropertiesOfType<SgfProperty.NodeAnnotation.C>())
                else -> node.copy(properties = node.properties + SgfProperty.NodeAnnotation.C(value))
            }
        }

    override var isHotspot: Boolean
        get() = node.hasProperty<SgfProperty.NodeAnnotation.HO>()
        set(value) {
            node = if (value) {
                node.copy(
                    properties = node.properties + SgfProperty.NodeAnnotation.HO(SgfDouble.Normal)
                )
            } else {
                node.copy(
                    properties = node.properties.removePropertiesOfType<SgfProperty.NodeAnnotation.HO>()
                )
            }
        }

    override var name: String?
        get() = node.property<SgfProperty.NodeAnnotation.N>()?.name
        set(value) {
            node = when (value) {
                null -> node.copy(
                    properties = node.properties.removePropertiesOfType<SgfProperty.NodeAnnotation.N>()
                )
                else -> node.copy(
                    properties = node.properties + SgfProperty.NodeAnnotation.N(value)
                )
            }
        }

    override var remark: Remark?
        get() = getRemarkFromNode(node)
        set(value) {
            node = if (value == null) {
                node.removeRemarks()
            } else {
                node.addRemark(value)
            }
        }
}

/**
 * Adds or removes annotations from the current node by executing the given [block].
 */
@OptIn(ExperimentalContracts::class)
fun SgfEditor.annotateNode(block: NodeAnnotationBuilder.() -> Unit): SgfEditor {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val builder = NodeAnnotationBuilderImpl(currentNode)
    builder.block()

    return updateCurrentNode { builder.node }
}
