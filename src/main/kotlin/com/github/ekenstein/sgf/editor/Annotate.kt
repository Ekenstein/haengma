package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.SgfProperty

/**
 * Describes how to add a comment to the current node.
 */
sealed class CommentMode {
    /**
     * Replace the old comment with the new comment.
     */
    object Replace : CommentMode()

    /**
     * Append the comment to the old comment and use the given [separator].
     */
    data class Append(val separator: CharSequence) : CommentMode()

    /**
     * Prepends the comment to the old comment and uses the given [separator].
     */
    data class Prepend(val separator: CharSequence) : CommentMode()
}

/**
 * Adds the [comment] to the current node. Depending on the [mode] it either replace the old comment,
 * prepend or append the [comment] to the old comment (if there is an old comment).
 *
 * @param comment The comment to add to the node.
 * @param mode The mode to use to add the comment to the node. Default is to append the comment to the node
 *  separated by a new line.
 * @return An updated [SgfEditor] containing the [comment] on the current node.
 */
fun SgfEditor.addComment(comment: String, mode: CommentMode = CommentMode.Append("\r\n")): SgfEditor {
    val oldComment = getComment().takeIf { it.isNotEmpty() }
    val updatedComment = when (mode) {
        is CommentMode.Append -> listOfNotNull(oldComment, comment).joinToString(mode.separator)
        CommentMode.Replace -> comment
        is CommentMode.Prepend -> listOfNotNull(comment, oldComment).joinToString(mode.separator)
    }

    return updateCurrentNode {
        it.copy(properties = it.properties + SgfProperty.NodeAnnotation.C(updatedComment))
    }
}

/**
 * Returns the comments on the current node or an empty string if there are no comments on the current node.
 */
fun SgfEditor.getComment(): String = currentNode.property<SgfProperty.NodeAnnotation.C>()?.comment
    ?: ""
