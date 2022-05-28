package com.github.ekenstein.sgf.editor

/**
 * Checks if the current node is the root node.
 */
fun SgfEditor.isRootNode() = currentTree.top.isEmpty() && currentSequence.left.isEmpty()
