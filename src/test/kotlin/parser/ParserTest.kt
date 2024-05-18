package parser

import org.junit.Assert.*
import org.junit.Test
import javax.swing.JTable

class ParserTest {

    private val parser:Parser = Parser(JTable())

    @Test
    fun transformUnitaryOperatorsTest() {
        var expected = "1+~3"
        var input = "1+-+3"
        assertEquals(expected, parser.transformUnaryMinus(input))
        expected = "~(~3/2)"
        input = "- (-3 / 2)"
        assertEquals(expected, parser.transformUnaryMinus(input))
    }

    @Test
    fun transformUnitaryOperatorsTestNoUnary() {
        val input = "(1+3)*3+2-1"
        assertEquals(input, parser.transformUnaryMinus(input))
    }

    @Test
    fun infixToPrefixTest() {
        val input = "1+3-2"
        val expected = listOf<String>("-","2","+","3","1")
        assertEquals(expected, parser.infixToPrefixTokenize(input))
    }
}