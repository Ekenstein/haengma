package com.github.ekenstein.sgf.parser

import com.github.ekenstein.sgf.GameDate
import com.github.ekenstein.sgf.GameResult
import com.github.ekenstein.sgf.GameType
import com.github.ekenstein.sgf.SgfCollection
import com.github.ekenstein.sgf.SgfColor
import com.github.ekenstein.sgf.SgfPoint
import com.github.ekenstein.sgf.SgfProperty
import com.github.ekenstein.sgf.sgf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.charset.Charset
import kotlin.test.assertEquals

class SgfParserTest {
    @Nested
    inner class `private properties` {
        @Test
        fun `unrecognized properties are saved as private properties`() {
            fun expected(identifier: String, vararg values: String) = sgf {
                tree { node { property(SgfProperty.Private(identifier, values.toList())) } }
            }
            assertAll(
                {
                    val sgf = "(;FOO[a][b][c])"
                    val collection = SgfCollection.from(sgf)
                    val expected = expected("FOO", "a", "b", "c")
                    assertEquals(expected, collection)
                },
                {
                    val sgf = "(;APA[[Shusaku\\]: Hello!])"
                    val collection = SgfCollection.from(sgf)
                    val expected = expected("APA", "[Shusaku\\]: Hello!")
                    assertEquals(expected, collection)
                }
            )
        }
    }

    @Nested
    inner class `root properties` {
        @ParameterizedTest
        @CsvSource(
            "(;FF[5])",
            "(;FF[0])"
        )
        fun `FF value must be in range 1-4`(sgf: String) {
            assertThrows<SgfParseException> { SgfCollection.from(sgf) }
        }

        @ParameterizedTest
        @CsvSource(
            "(;FF[1]), 1",
            "(;FF[2]), 2",
            "(;FF[3]), 3",
            "(;FF[4]), 4",
        )
        fun `FF can be parsed iff the number is in range 1-4`(sgf: String, number: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Root.FF(number)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;ST[-1])",
            "(;ST[4])"
        )
        fun `ST must be in range 0-3`(sgf: String) {
            assertThrows<SgfParseException> { SgfCollection.from(sgf) }
        }

        @ParameterizedTest
        @CsvSource(
            "(;ST[0]), 0",
            "(;ST[1]), 1",
            "(;ST[2]), 2",
            "(;ST[3]), 3",
        )
        fun `ST can be parsed iff the number is in range 0-3`(sgf: String, number: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Root.ST(number)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;SZ[19]), 19, 19",
            "(;SZ[18:15]), 18, 15"
        )
        fun `SZ can both have a composed value and a single value`(sgf: String, width: Int, height: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Root.SZ(width, height)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;GM[0])",
            "(;GM[41])"
        )
        fun `GM value must be in range 1-16`(sgf: String) {
            assertThrows<SgfParseException> { SgfCollection.from(sgf) }
        }

        @Test
        fun `GM can be parsed iff number can be translated to a game type`() {
            val sgf = GameType.values().map { "(;GM[${it.value}])" to it }
            val assertions = sgf.map { (sgf, gameType) ->
                {
                    val collection = SgfCollection.from(sgf)
                    val expected = sgf { tree { node { property(SgfProperty.Root.GM(gameType)) } } }
                    assertEquals(expected, collection)
                }
            }

            assertAll(assertions)
        }

        @ParameterizedTest
        @CsvSource(
            "(;SZ[0]), 6, 7",
            "(;SZ[0:1]), 6, 9",
            "(;SZ[53:1]), 6, 10",
            "(;SZ[99:103]), 6, 12",
            "(;SZ[53]), 6, 8"
        )
        fun `SZ must contain a number or composed number between 1-52`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "(;SZ[19]), 19",
            "(;SZ[13]), 13",
            "(;SZ[9]), 9",
        )
        fun `SZ can have a number as a value`(sgf: String, size: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Root.SZ(size)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;SZ[7:5]), 7, 5",
            "(;SZ[19:19]), 19, 19",
            "(;SZ[52:51]), 52, 51"
        )
        fun `SZ can have a composed value`(sgf: String, width: Int, height: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Root.SZ(width, height)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;SZ[foo:19]), 6, 12",
            "(;SZ[19:foo]), 6, 12"
        )
        fun `SZ with composed value must contain number on both sides`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "(;AP[CGoban:1.6.2]), CGoban, 1.6.2",
            "(;AP[Hibiscus:2.1]), Hibiscus, 2.1",
            "(;AP[IGS:5.0]), IGS, 5.0",
            "(;AP[Many Faces of Go:10.0]), Many Faces of Go, 10.0",
            "(;AP[MGT:?]), MGT, ?",
            "(;AP[NNGS:?]), NNGS, ?",
            "(;AP[Primiview:3.0]), Primiview, 3.0",
            "(;AP[SGB:?]), SGB, ?",
            "(;AP[SmartGo:1.0]), SmartGo, 1.0",
        )
        fun `AP parses to name and version`(sgf: String, name: String, version: String) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Root.AP(name, version)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;CA[UTF-8]), UTF-8",
            "(;CA[ISO-8859-1]), ISO-8859-1"
        )
        fun `CA parses to the charset`(sgf: String, charset: String) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Root.CA(Charset.forName(charset))) } } }
            assertEquals(expected, collection)
        }
    }

    @Nested
    inner class `timing properties` {
        @ParameterizedTest
        @CsvSource(
            "(;OB[foo]), 6, 9",
            "(;OB[aa]), 6, 8",
            "(;OB[4.5]), 6, 9",

        )
        fun `OB must contain a real value`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "(;OB[4]), 4",
            "(;OB[+7]), 7",
            "(;OB[-7]), -7"
        )
        fun `OB can parse a value that is a number`(sgf: String, number: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Timing.OB(number)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;OW[foo]), 6, 9",
            "(;OW[aa]), 6, 8",
            "(;OW[4.5]), 6, 9",

        )
        fun `OW must contain a real value`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "(;OW[4]), 4",
            "(;OW[+7]), 7",
            "(;OW[-7]), -7"
        )
        fun `OW can parse a value that is a number`(sgf: String, number: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Timing.OW(number)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;BL[foo]), 6, 9",
            "(;BL[aa]), 6, 8"
        )
        fun `BL must contain a real value`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "(;BL[4]), 4.0",
            "(;BL[4.5]), 4.5",
            "(;BL[-4.5]), -4.5",
            "(;BL[+4.5]), 4.5",
            "(;BL[+7]), 7.0",
            "(;BL[-7]), -7.0"
        )
        fun `BL can parse a value that is a number or real`(sgf: String, number: Double) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Timing.BL(number)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;WL[foo]), 6, 9",
            "(;WL[aa]), 6, 8"
        )
        fun `WL must contain a real value`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "(;WL[4]), 4.0",
            "(;WL[4.5]), 4.5",
            "(;WL[-4.5]), -4.5",
            "(;WL[+4.5]), 4.5",
            "(;WL[+7]), 7.0",
            "(;WL[-7]), -7.0"
        )
        fun `WL can parse a value that is a number or real`(sgf: String, number: Double) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Timing.WL(number)) } } }
            assertEquals(expected, collection)
        }
    }

    @Nested
    inner class `misc properties` {
        @ParameterizedTest
        @CsvSource(
            "(;FG[aa]), 6, 8",
            "(;FG[aa:bb]), 6, 8"
        )
        fun `FG must either either be none or a composition of number and simpletext`(
            sgf: String,
            startColumn: Int,
            endColumn: Int
        ) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @Test
        fun `FG can have no value`() {
            val sgf = "(;FG[])"
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Misc.FG()) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;FG[1:[Shusaku\\]\\: Hello!]), 1, [Shusaku]: Hello!",
            "(;FG[515:Foo]), 515, Foo"
        )
        fun `FG can have a figure flag and a diagram name`(sgf: String, flag: Int, name: String) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Misc.FG(name, flag)) } } }
            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;VW[apa][B][1]), 6, 9"
        )
        fun `VW must have a list of points or an empty list`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @Test
        fun `VW can have an empty list`() {
            val collection = SgfCollection.from("(;VW[])")
            val expected = sgf { tree { node { property(SgfProperty.Misc.VW(emptyList())) } } }
            assertEquals(expected, collection)
        }

        @Test
        fun `VW can contain a list of points`() {
            val collection = SgfCollection.from("(;VW[aa][bb][cc])")
            val expected = sgf {
                tree {
                    node {
                        property(
                            SgfProperty.Misc.VW(
                                listOf(
                                    SgfPoint(1, 1),
                                    SgfPoint(2, 2),
                                    SgfPoint(3, 3)
                                )
                            )
                        )
                    }
                }
            }

            assertEquals(expected, collection)
        }

        @ParameterizedTest
        @CsvSource(
            "(;PM[4.5]), 6, 9",
            "(;PM[aa]), 6, 8"
        )
        fun `PM can only contain numbers`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @ParameterizedTest
        @CsvSource(
            "(;PM[0]), 0",
            "(;PM[1]), 1",
            "(;PM[2]), 2"
        )
        fun `PM will parse to print mode`(sgf: String, number: Int) {
            val collection = SgfCollection.from(sgf)
            val expected = sgf { tree { node { property(SgfProperty.Misc.PM(number)) } } }
            assertEquals(expected, collection)
        }
    }

    @Nested
    inner class `markup properties` {
        @ParameterizedTest
        @CsvSource(
            "(;DD[apa][B][1]), 6, 9"
        )
        fun `DD must have a list of points or an empty list`(sgf: String, startColumn: Int, endColumn: Int) {
            assertThrowsParseException(marker(startColumn, endColumn)) {
                SgfCollection.from(sgf)
            }
        }

        @Test
        fun `DD can have an empty list`() {
            val collection = SgfCollection.from("(;DD[])")
            val expected = sgf { tree { node { property(SgfProperty.Markup.DD(emptyList())) } } }
            assertEquals(expected, collection)
        }

        @Test
        fun `DD can contain a list of points`() {
            val collection = SgfCollection.from("(;DD[aa][bb][cc])")
            val expected = sgf {
                tree {
                    node {
                        property(
                            SgfProperty.Markup.DD(
                                listOf(
                                    SgfPoint(1, 1),
                                    SgfPoint(2, 2),
                                    SgfPoint(3, 3)
                                )
                            )
                        )
                    }
                }
            }

            assertEquals(expected, collection)
        }
    }

    @Nested
    inner class `game info properties` {
        @Test
        fun `RE contains the game result`() {
            assertAll(
                {
                    val sgf = "(;RE[B+0.5])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(SgfProperty.GameInfo.RE(GameResult.Score(SgfColor.Black, 0.5)))
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
                {
                    val sgf = "(;RE[W+0.5])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(SgfProperty.GameInfo.RE(GameResult.Score(SgfColor.White, 0.5)))
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
                {
                    val sgf = "(;RE[W+5])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(SgfProperty.GameInfo.RE(GameResult.Score(SgfColor.White, 5.0)))
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
                {
                    val sgf = "(;RE[0])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(SgfProperty.GameInfo.RE(GameResult.Draw))
                            }
                        }
                    }
                    assertEquals(expected, actual)
                }
            )
        }

        @Test
        fun `DT contains local dates`() {
            assertAll(
                {
                    val sgf = "(;DT[2022-04-20])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(SgfProperty.GameInfo.DT(listOf(GameDate.of(2022, 4, 20))))
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
                {
                    val sgf = "(;DT[2022-04-20,21])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(
                                    SgfProperty.GameInfo.DT(
                                        listOf(
                                            GameDate.of(2022, 4, 20),
                                            GameDate.of(2022, 4, 21)
                                        )
                                    )
                                )
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
                {
                    val sgf = "(;DT[2022-04-20,21,2023-05-06])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(
                                    SgfProperty.GameInfo.DT(
                                        listOf(
                                            GameDate.of(2022, 4, 20),
                                            GameDate.of(2022, 4, 21),
                                            GameDate.of(2023, 5, 6)
                                        )
                                    )
                                )
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
                {
                    val sgf = "(;DT[2022-04-20,05-06,07])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(
                                    SgfProperty.GameInfo.DT(
                                        listOf(
                                            GameDate.of(2022, 4, 20),
                                            GameDate.of(2022, 5, 6),
                                            GameDate.of(2022, 5, 7)
                                        )
                                    )
                                )
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
                {
                    val sgf = "(;DT[2022-04-06,07,06-01,06])"
                    val actual = SgfCollection.from(sgf)
                    val expected = sgf {
                        tree {
                            node {
                                property(
                                    SgfProperty.GameInfo.DT(
                                        listOf(
                                            GameDate.of(2022, 4, 6),
                                            GameDate.of(2022, 4, 7),
                                            GameDate.of(2022, 6, 1),
                                            GameDate.of(2022, 6, 6)
                                        )
                                    )
                                )
                            }
                        }
                    }
                    assertEquals(expected, actual)
                },
            )
        }
    }

    private fun assertThrowsParseException(marker: Marker, block: () -> Unit) = try {
        block()
        assertTrue(false, "Expected an SgfParseException")
    } catch (ex: SgfParseException) {
        println(ex)
        assertEquals(marker, ex.marker)
    }

    fun marker(startColumn: Int, endColumn: Int) = Marker(1, startColumn, 1, endColumn)
}
