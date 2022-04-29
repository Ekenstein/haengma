package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfDouble
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.addProperty
import com.github.ekenstein.sgf.extensions.property

interface NodeBuilder {
    fun property(value: SgfProperty)
    fun property(identifier: String, values: List<String>)
    fun annotate(annotation: NodeAnnotation)
    var comment: String
}

internal abstract class DefaultNodeBuilder : NodeBuilder {
    abstract var node: SgfNode

    override fun property(value: SgfProperty) {
        node = node.addProperty(value)
    }

    override fun property(identifier: String, values: List<String>) {
        node = node.addProperty(SgfProperty.Private(identifier, values))
    }

    override var comment: String
        get() = node.property<SgfProperty.NodeAnnotation.C>()?.comment ?: ""
        set(value) {
            node.addProperty(SgfProperty.NodeAnnotation.C(value))
        }

    override fun annotate(annotation: NodeAnnotation) {
        val property = when (annotation) {
            NodeAnnotation.EvenPosition -> SgfProperty.NodeAnnotation.DM(SgfDouble.Normal)
            NodeAnnotation.GoodForBlack -> SgfProperty.NodeAnnotation.GB(SgfDouble.Normal)
            NodeAnnotation.GoodForWhite -> SgfProperty.NodeAnnotation.GW(SgfDouble.Normal)
            NodeAnnotation.Hotspot -> SgfProperty.NodeAnnotation.HO(SgfDouble.Normal)
            NodeAnnotation.Unclear -> SgfProperty.NodeAnnotation.UC(SgfDouble.Normal)
        }

        node = node.addProperty(property)
    }
}

enum class NodeAnnotation {
    EvenPosition,
    GoodForBlack,
    GoodForWhite,
    Hotspot,
    Unclear
}
