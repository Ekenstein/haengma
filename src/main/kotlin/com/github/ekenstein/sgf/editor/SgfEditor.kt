package com.github.ekenstein.sgf.editor

import com.github.ekenstein.sgf.GameInfo
import com.github.ekenstein.sgf.GameInfoBuilder
import com.github.ekenstein.sgf.Move
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfException
import com.github.ekenstein.sgf.SgfGameTree
import com.github.ekenstein.sgf.SgfNode
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.asPointOrNull
import com.github.ekenstein.sgf.gameInfo
import com.github.ekenstein.sgf.getGameInfo
import com.github.ekenstein.sgf.toPropertySet
import com.github.ekenstein.sgf.toSgfProperties
import com.github.ekenstein.sgf.utils.LinkedList
import com.github.ekenstein.sgf.utils.MoveResult
import com.github.ekenstein.sgf.utils.NonEmptyList
import com.github.ekenstein.sgf.utils.NonEmptySet
import com.github.ekenstein.sgf.utils.TreeZipper
import com.github.ekenstein.sgf.utils.Unzip
import com.github.ekenstein.sgf.utils.Zipper
import com.github.ekenstein.sgf.utils.commit
import com.github.ekenstein.sgf.utils.commitAtCurrentPosition
import com.github.ekenstein.sgf.utils.flatMap
import com.github.ekenstein.sgf.utils.get
import com.github.ekenstein.sgf.utils.goRightUnsafe
import com.github.ekenstein.sgf.utils.goUp
import com.github.ekenstein.sgf.utils.indexOfCurrent
import com.github.ekenstein.sgf.utils.insertDownLeft
import com.github.ekenstein.sgf.utils.insertRight
import com.github.ekenstein.sgf.utils.linkedListOfNotNull
import com.github.ekenstein.sgf.utils.map
import com.github.ekenstein.sgf.utils.nelOf
import com.github.ekenstein.sgf.utils.nonEmptySetOf
import com.github.ekenstein.sgf.utils.onSuccess
import com.github.ekenstein.sgf.utils.orElse
import com.github.ekenstein.sgf.utils.orNull
import com.github.ekenstein.sgf.utils.toLinkedList
import com.github.ekenstein.sgf.utils.toNel
import com.github.ekenstein.sgf.utils.toNonEmptySetUnsafe
import com.github.ekenstein.sgf.utils.toZipper
import com.github.ekenstein.sgf.utils.update
import com.github.ekenstein.sgf.utils.withOrigin

private object GameTreeUnzip : Unzip<SgfGameTree> {
    override fun unzip(node: SgfGameTree): LinkedList<SgfGameTree> = node.trees.toLinkedList()

    override fun zip(node: SgfGameTree, children: LinkedList<SgfGameTree>): SgfGameTree = node.copy(
        trees = children
    )
}

/**
 * An editor for a [SgfGameTree]. Enables navigation and alteration of a tree.
 *
 * You can navigate through sequences with:
 * - [SgfEditor.goToNextNode]
 * - [SgfEditor.goToPreviousNode]
 *
 * Traverse the trees with:
 * - [SgfEditor.goToParentTree]
 * - [SgfEditor.goToNextTree]
 * - [SgfEditor.goToPreviousTree]
 * - [SgfEditor.goToLeftMostChildTree]
 *
 * You can quick-move through the tree with:
 * - [SgfEditor.goToRootNode]
 * - [SgfEditor.goToLastNode]
 *
 * You can alter the tree by using:
 * - [SgfEditor.placeStone]
 * - [SgfEditor.pass],
 * - [SgfEditor.addStones]
 * - [SgfEditor.setNextToPlay]
 */
data class SgfEditor(
    val currentSequence: Zipper<SgfNode>,
    val currentTree: TreeZipper<SgfGameTree>
) {
    constructor(gameTree: SgfGameTree) : this(
        gameTree.sequence.toZipper(),
        TreeZipper.ofNode(gameTree, GameTreeUnzip)
    )
    constructor(gameInfo: GameInfo) : this(SgfGameTree(nelOf(SgfNode(gameInfo.toSgfProperties()))))
    constructor(block: GameInfoBuilder.() -> Unit = { }) : this(gameInfo(block))

    val currentNode: SgfNode = currentSequence.focus
    val gameInfo: GameInfo by lazy { goToRootNode().currentNode.getGameInfo() }
}

/**
 * Returns the game information of the tree. If game information is missing, the default values will be used.
 */
fun SgfEditor.getGameInfo(): GameInfo = goToRootNode().currentNode.getGameInfo()

/**
 * Will update the game info of the tree regardless of the position the editor is currently at.
 * Will always return an updated editor located at the same position as the given editor.
 */
fun SgfEditor.updateGameInfo(block: GameInfoBuilder.() -> Unit): SgfEditor {
    val backtracking = mutableListOf<(SgfEditor) -> SgfEditor>()

    fun SgfEditor.goToPreviousNodeWithBackTracking() = goToPreviousNodeInSequence()
        .onSuccess { _ ->
            backtracking.add {
                it.goToNextNodeInSequence().get()
            }
        }.orElse { _ ->
            goToParentTree().onSuccess { _ ->
                val index = currentTree.indexOfCurrent()
                backtracking.add { parent ->
                    parent.goToLeftMostChildTree().map { child ->
                        child.repeat(index) { it.goToNextTree() }
                    }.get()
                }
            }
        }

    tailrec fun SgfEditor.goToRootNodeWithBackTracking(): SgfEditor = when (val previous = goToPreviousNodeWithBackTracking()) {
        is MoveResult.Failure -> this
        is MoveResult.Success -> previous.position.goToRootNodeWithBackTracking()
    }

    val root = goToRootNodeWithBackTracking()
    val gameInfo = gameInfo(root.currentNode.getGameInfo(), block)
    val properties = gameInfo.toSgfProperties()

    val newRoot = root.updateCurrentNode { rootNode ->
        rootNode.copy(properties = rootNode.properties + properties)
    }

    return backtracking.reversed().fold(newRoot) { r, b -> b(r) }
}

private fun SgfEditor.insertBranch(node: SgfNode): SgfEditor {
    val mainVariation = SgfGameTree(nelOf(node))
    val restOfSequence = currentSequence.right.toNel()?.let {
        SgfGameTree(
            sequence = it,
            trees = currentTree.focus.trees
        )
    }
    val newTree = currentTree.update {
        it.copy(
            sequence = currentSequence.commitAtCurrentPosition(),
            trees = if (restOfSequence == null) {
                it.trees
            } else {
                emptyList()
            }
        )
    }

    return copy(
        currentSequence = mainVariation.sequence.toZipper(),
        currentTree = newTree.insertDownLeft(linkedListOfNotNull(mainVariation, restOfSequence))
    )
}

private fun SgfEditor.insertNodeToTheRight(node: SgfNode): SgfEditor {
    val sequence = currentSequence.insertRight(node).goRightUnsafe()

    return copy(
        currentSequence = sequence,
        currentTree = currentTree.update {
            it.copy(sequence = sequence.commit())
        }
    )
}

/**
 * Executes the [block] and updates the current node to the resulting node.
 */
fun SgfEditor.updateCurrentNode(block: (SgfNode) -> SgfNode): SgfEditor {
    val newSequence = currentSequence.update(block)
    return copy(
        currentSequence = newSequence,
        currentTree = currentTree.update {
            it.copy(sequence = newSequence.commit())
        }
    )
}

/**
 * Saves the state of the editor to a [SgfGameTree]
 */
fun SgfEditor.commit() = currentTree.commit()

private fun SgfNode.hasSetupProperties() = hasProperty<SgfProperty.Setup>()
private fun SgfNode.hasRootProperties() = hasProperty<SgfProperty.Root>()
private fun SgfNode.hasMoveProperties() = hasProperty<SgfProperty.Move>()

private fun SgfEditor.getFullSequence(): NonEmptyList<SgfNode> {
    tailrec fun TreeZipper<SgfGameTree>.nodes(result: NonEmptyList<SgfNode>): NonEmptyList<SgfNode> =
        when (val parent = goUp()) {
            is MoveResult.Failure -> result
            is MoveResult.Success -> parent.position.nodes(focus.sequence + result)
        }

    return currentTree.nodes(currentSequence.commitAtCurrentPosition())
}

private fun applyNodePropertiesToBoard(
    board: Board,
    node: SgfNode
) = node.properties.fold(board) { b, property ->
    val newBoard = when (property) {
        is SgfProperty.Move.B -> property.move.asPointOrNull?.let {
            b.placeStone(SgfColor.Black, it)
        }
        is SgfProperty.Move.W -> property.move.asPointOrNull?.let {
            b.placeStone(SgfColor.White, it)
        }
        is SgfProperty.Setup.AB -> b.copy(
            stones = b.stones + property.points.map { it to SgfColor.Black }
        )
        is SgfProperty.Setup.AW -> b.copy(
            stones = b.stones + property.points.map { it to SgfColor.White }
        )
        is SgfProperty.Setup.AE -> b.copy(
            stones = b.stones - property.points
        )
        else -> b
    }

    newBoard ?: b
}

/**
 * Extracts the current board position.
 */
fun SgfEditor.extractBoard(): Board {
    val sequence = getFullSequence()
    val boardSize = when (val size = goToRootNode().currentSequence.focus.property<SgfProperty.Root.SZ>()) {
        null -> 19 to 19
        else -> size.width to size.height
    }

    return sequence.fold(Board.empty(boardSize), ::applyNodePropertiesToBoard)
}

private fun SgfEditor.startingColor(): SgfColor = if (getGameInfo().rules.handicap >= 2) {
    SgfColor.White
} else {
    SgfColor.Black
}

/**
 * Returns whose turn it is to play at the current position.
 */
fun SgfEditor.nextToPlay(): SgfColor {
    fun SgfNode.nextToPlay() = properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B -> SgfColor.White
            is SgfProperty.Move.W -> SgfColor.Black
            is SgfProperty.Setup.PL -> it.color
            else -> null
        }
    }.singleOrNull()

    tailrec fun SgfEditor.nextToPlay(): SgfColor? = when (val color = currentSequence.focus.nextToPlay()) {
        null -> goToPreviousNode().orNull()?.nextToPlay()
        else -> color
    }

    return nextToPlay() ?: startingColor()
}

/**
 * Clears the given [points] from stones in the current position.
 */
fun SgfEditor.removeStones(point: SgfPoint, vararg points: SgfPoint) = removeStones(nonEmptySetOf(point, *points))

/**
 * Clears the given set of [points] from stones in the current position.
 */
fun SgfEditor.removeStones(points: NonEmptySet<SgfPoint>) = addSetupProperty(SgfProperty.Setup.AE(points))

/**
 * Adds the given stones to the position regardless of what was at the position before.
 */
fun SgfEditor.addStones(color: SgfColor, point: SgfPoint, vararg points: SgfPoint) =
    addStones(color, nonEmptySetOf(point, *points))

/**
 * Adds the given set of stones to the position regardless of what was at the position before.
 */
fun SgfEditor.addStones(color: SgfColor, points: NonEmptySet<SgfPoint>) = when (color) {
    SgfColor.Black -> addSetupProperty(SgfProperty.Setup.AB(points))
    SgfColor.White -> addSetupProperty(SgfProperty.Setup.AW(points))
}

/**
 * Sets whose turn it is to play at the current position.
 */
fun SgfEditor.setNextToPlay(color: SgfColor) = addSetupProperty(SgfProperty.Setup.PL(color))

/**
 * The player of [color] passes at the current position.
 *
 * Will throw [SgfException.IllegalMove] iff [force] is false, and it's not [color]'s turn to play.
 *
 * @param color The color of the player who passes
 * @param force Whether the execution of the move should be forced or not. If true, no validation will occur,
 *              otherwise the move must valid for the current position.
 * @throws [SgfException.IllegalMove] if the move was invalid and the [force] flag was false.
 */
fun SgfEditor.pass(color: SgfColor, force: Boolean = false): SgfEditor {
    val property = when (color) {
        SgfColor.Black -> SgfProperty.Move.B(Move.Pass)
        SgfColor.White -> SgfProperty.Move.W(Move.Pass)
    }

    val result = if (force) {
        addMoveProperty(property).flatMap {
            it.updateCurrentNode { node ->
                node.copy(properties = node.properties + SgfProperty.Move.KO)
            }.stay()
        }
    } else {
        checkMove(nextToPlay() == color) {
            "It's not ${color.asString}'s turn to play"
        }

        addMoveProperty(property)
    }

    return result.get()
}

/**
 * Places a stone at the given point at the current position.
 *
 * Will throw [SgfException.IllegalMove] iff:
 *  - The stone is placed outside the board or ...
 *  - [force] is false and ...
 *  - It's not the [color]'s turn to play.
 *  - The point is occupied by another stone.
 *  - The placed stone results in repetition of the position (ko).
 *  - The stone immediately dies when placed on the board (suicide).
 *  @param color The color of the stone to place
 *  @param x The x-coordinate for the stone
 *  @param y The y-coordinate for the stone.
 *  @param force Whether the execution of the move should be forced or not. If true, no validation will occur,
 *               otherwise the move must be a valid move at the current position.
 *  @throws [SgfException.IllegalMove] If the move would result in placing the stone outside the board, or if the move
 *  was invalid at the current position and the [force] flag was false.
 */
fun SgfEditor.placeStone(color: SgfColor, x: Int, y: Int, force: Boolean = false) = placeStone(
    color = color,
    point = SgfPoint(x, y),
    force = force
)

/**
 * Places a stone at the given point at the current position.
 *
 * Will throw [SgfException.IllegalMove] iff:
 *  - The stone is placed outside the board or ...
 *  - [force] is false and ...
 *  - It's not the [color]'s turn to play.
 *  - The point is occupied by another stone.
 *  - The placed stone results in repetition of the position (ko).
 *  - The stone immediately dies when placed on the board (suicide).
 *  @param color The color of the stone to place
 *  @param point The point to place the stone
 *  @param force Whether the execution of the move should be forced or not. If true, no validation will occur,
 *               otherwise the move must be a valid move at the current position.
 *  @throws [SgfException.IllegalMove] If the move would result in placing the stone outside the board, or if the move
 *  was invalid at the current position and the [force] flag was false.
 */
fun SgfEditor.placeStone(color: SgfColor, point: SgfPoint, force: Boolean = false): SgfEditor {
    val currentBoard = extractBoard()
    checkMove(point.x in 1..currentBoard.width && point.y in 1..currentBoard.height) {
        "The stone is placed outside of the board"
    }
    val property = when (color) {
        SgfColor.Black -> SgfProperty.Move.B(Move.Stone(point))
        SgfColor.White -> SgfProperty.Move.W(Move.Stone(point))
    }

    val result = if (force) {
        addMoveProperty(property).flatMap {
            it.updateCurrentNode { node ->
                node.copy(properties = node.properties + SgfProperty.Move.KO)
            }.stay()
        }
    } else {
        checkIfMoveIsValid(color, point, currentBoard)
        addMoveProperty(property)
    }

    return result.get()
}

private fun SgfEditor.checkIfMoveIsValid(color: SgfColor, point: SgfPoint, currentBoard: Board) {
    checkMove(nextToPlay() == color) {
        "It's not ${color.asString}'s turn to play"
    }

    checkMove(!currentBoard.isOccupied(point)) {
        "The point ${point.x}, ${point.y} is occupied"
    }

    val previousBoard = goToPreviousNode().orNull()?.extractBoard()
    val nextBoard = currentBoard.placeStone(color, point)

    checkMove(nextBoard.stones != previousBoard?.stones) {
        "The position is repeating"
    }

    checkMove(nextBoard.stones.containsKey(point)) {
        "It is suicide to play at the point ${point.x}, ${point.y}"
    }
}

private val SgfNode.move: SgfProperty?
    get() = properties.mapNotNull {
        when (it) {
            is SgfProperty.Move.B,
            is SgfProperty.Move.W -> it
            else -> null
        }
    }.singleOrNull()

private val SgfColor.asString
    get() = when (this) {
        SgfColor.Black -> "black"
        SgfColor.White -> "white"
    }

private fun checkMove(value: Boolean, reason: () -> String) {
    if (!value) {
        throw SgfException.IllegalMove(reason())
    }
}

private fun SgfProperty.Setup.getPoints() = when (this) {
    is SgfProperty.Setup.AB -> points
    is SgfProperty.Setup.AE -> points
    is SgfProperty.Setup.AW -> points
    is SgfProperty.Setup.PL -> emptySet()
}

private fun SgfProperty.Setup.setPoints(points: NonEmptySet<SgfPoint>) = when (this) {
    is SgfProperty.Setup.AB -> copy(points = points)
    is SgfProperty.Setup.AE -> copy(points = points)
    is SgfProperty.Setup.AW -> copy(points = points)
    is SgfProperty.Setup.PL -> this
}

private fun SgfNode.updateSetupProperties(
    property: SgfProperty.Setup,
    opposing: List<SgfProperty.Setup>
): SgfNode {
    val pointsByProperty = opposing.associateWith { it.getPoints() - property.getPoints() }
    val removedProperties = pointsByProperty.filterValues { it.isEmpty() }.keys
    val updatedProperties = (pointsByProperty - removedProperties).map { (property, points) ->
        property.setPoints(points.toNonEmptySetUnsafe())
    }

    val newProperties = properties + property + updatedProperties - removedProperties

    return copy(
        properties = newProperties.toPropertySet()
    )
}

private fun SgfEditor.addSetupProperty(property: SgfProperty.Setup): SgfEditor =
    if (!currentNode.hasMoveProperties()) {
        updateCurrentNode { node ->
            when (property) {
                is SgfProperty.Setup.AB -> {
                    val updatedProperty = node.property<SgfProperty.Setup.AB>()
                        ?.let { it.copy(points = it.points + property.points) }
                        ?: property

                    node.updateSetupProperties(
                        updatedProperty,
                        listOfNotNull(
                            node.property<SgfProperty.Setup.AE>(),
                            node.property<SgfProperty.Setup.AW>()
                        )
                    )
                }
                is SgfProperty.Setup.AE -> {
                    val updatedProperty = node.property<SgfProperty.Setup.AE>()
                        ?.let { it.copy(points = it.points + property.points) }
                        ?: property

                    node.updateSetupProperties(
                        updatedProperty,
                        listOfNotNull(
                            node.property<SgfProperty.Setup.AB>(),
                            node.property<SgfProperty.Setup.AW>()
                        )
                    )
                }
                is SgfProperty.Setup.AW -> {
                    val updatedProperty = node.property<SgfProperty.Setup.AW>()
                        ?.let { it.copy(points = it.points + property.points) }
                        ?: property

                    node.updateSetupProperties(
                        updatedProperty,
                        listOfNotNull(
                            node.property<SgfProperty.Setup.AE>(),
                            node.property<SgfProperty.Setup.AB>()
                        )
                    )
                }
                is SgfProperty.Setup.PL -> node.copy(properties = node.properties + property)
            }
        }
    } else {
        val node = SgfNode(property)
        when (currentSequence.right) {
            is LinkedList.Cons -> insertBranch(node)
            LinkedList.Nil -> if (currentTree.focus.trees.isEmpty()) {
                insertNodeToTheRight(node)
            } else {
                insertBranch(node)
            }
        }
    }

private fun SgfEditor.goToTreeThatStartsWithProperty(property: SgfProperty): MoveResult<SgfEditor> {
    fun sequenceStartsWith(editor: SgfEditor) = editor.currentSequence.focus.move == property
    return tryRepeatWhileNot(::sequenceStartsWith) {
        it.goToNextTree()
    }
}

private fun SgfEditor.insertInNextNodeOrBranchOut(
    property: SgfProperty.Move
) = when (val right = currentSequence.right) {
    is LinkedList.Cons -> when (right.head.move) {
        property -> copy(currentSequence = currentSequence.goRightUnsafe()).stay()
        else -> goToLeftMostChildTree()
            .flatMap { it.goToTreeThatStartsWithProperty(property) }
            .withOrigin(this)
            .orElse {
                val node = SgfNode(property)
                it.insertBranch(node).stay()
            }
    }
    LinkedList.Nil -> goToLeftMostChildTree()
        .flatMap { it.goToTreeThatStartsWithProperty(property) }
        .withOrigin(this)
        .orElse {
            val node = SgfNode(property)
            if (it.currentTree.focus.trees.isEmpty()) {
                it.insertNodeToTheRight(node).stay()
            } else {
                it.insertBranch(node).stay()
            }
        }
}

private fun SgfEditor.addMoveProperty(property: SgfProperty.Move): MoveResult<SgfEditor> {
    val result = if (currentNode.hasRootProperties() || currentNode.hasSetupProperties()) {
        insertInNextNodeOrBranchOut(property)
    } else {
        when (currentNode.move) {
            property -> stay() // no-op
            null -> updateCurrentNode {
                it.copy(
                    properties = it.properties + property
                )
            }.stay()
            else -> insertInNextNodeOrBranchOut(property)
        }
    }

    return result.withOrigin(this)
}
