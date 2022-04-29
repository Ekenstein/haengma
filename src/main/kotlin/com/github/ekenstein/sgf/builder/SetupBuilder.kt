package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.extensions.plus

@SgfDslMarker
interface SetupBuilder {
    fun stones(color: SgfColor, points: Set<SgfPoint>)
    fun clear(points: Set<SgfPoint>)
    fun colorToPlay(color: SgfColor)
}

internal class DefaultSetupBuilder(override var node: SgfNode) : DefaultNodeBuilder(), SetupBuilder {
    override fun stones(color: SgfColor, points: Set<SgfPoint>) {
        val property = when (color) {
            SgfColor.Black -> SgfProperty.Setup.AB(points)
            SgfColor.White -> SgfProperty.Setup.AW(points)
        }

        node += property
    }

    override fun clear(points: Set<SgfPoint>) {
        node += SgfProperty.Setup.AE(points)
    }

    override fun colorToPlay(color: SgfColor) {
        node += SgfProperty.Setup.PL(color)
    }
}

fun SetupBuilder.stones(color: SgfColor, vararg points: SgfPoint) = stones(color, points.toSet())
fun SetupBuilder.clear(vararg points: SgfPoint) = clear(points.toSet())
