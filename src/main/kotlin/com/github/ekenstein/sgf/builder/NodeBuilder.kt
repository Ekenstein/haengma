package com.github.ekenstein.sgf.builder

import com.github.ekenstein.sgf.SgfProperty

interface NodeBuilder {
    fun property(value: SgfProperty)
    fun property(identifier: String, values: List<String>)
}
