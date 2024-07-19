package parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import parser.expressions.BinOp
import parser.expressions.NumberExpr
import parser.expressions.UnOp
import ui.Table
import java.lang.StringBuilder

class ParserOldTest {

    private val table: Table = Table(10, 10)
    private val parserOld:ParserOld = ParserOld(table)

    @Test
    fun handleUnaryOperatorsTest() {
        var expected = "1+~3"
        var input = "+1+-+3"
        assertEquals(expected, parserOld.handleUnaryMinus(input))
        expected = "~ (~3 / 2)"
        input = "- (-3 / 2)"
        assertEquals(expected, parserOld.handleUnaryMinus(input))
    }

    @Test
    fun handleUnaryOperatorsTestInvalid() {
        val input = "16/*8"
        assertThrows(Exception::class.java) {
            parserOld.handleUnaryMinus(input)
        }
    }

    @Test
    fun mapCoordinatesToTableCell() {
        var input = "A1"
        var expected = Pair(0, 1)
        assertEquals(expected, parserOld.mapCoordinatesToTableCell(input))
        input = "B2"
        expected = Pair(1, 2)
        assertEquals(expected, parserOld.mapCoordinatesToTableCell(input))
        input = "F10"
        expected = Pair(9, 6)
        assertEquals(expected, parserOld.mapCoordinatesToTableCell(input))
        input = "AA1"
        expected = Pair(0, 27)
        assertEquals(expected, parserOld.mapCoordinatesToTableCell(input))
    }

    @Test
    fun calculateReferencesTest() {
        table.setValueAt("1", 0, 1)
        table.setValueAt("3", 1, 2)
        val input = "A1+B2"
        val expected = "1.0+3.0"
        assertEquals(expected, parserOld.calculateReferences(input))
    }

    @Test
    fun preprocessNamedFunctionsTest() {
        var input = "pow(3,2)"
        var expected = "(3)pow(2)"
        assertEquals(expected, parserOld.preprocessNamedFunctions(input, StringBuilder(), 0, input.length))
        input = "pow(-3,2+4)"
        expected = "(-3)pow(2+4)"
        assertEquals(expected, parserOld.preprocessNamedFunctions(input, StringBuilder(), 0, input.length))
        input = "pow(3,2+2)/pow(2+5,3)"
        expected = "(3)pow(2+2)/(2+5)pow(3)"
        assertEquals(expected, parserOld.preprocessNamedFunctions(input, StringBuilder(), 0, input.length))
    }

    @Test
    fun preprocessNamedFunctionsUnary() {
        var input = "sqrt(2)"
        var expected = "sqrt(2)"
        assertEquals(expected, parserOld.preprocessNamedFunctions(input, StringBuilder(), 0, input.length))
        input = "fact(-2*8)"
        expected = "fact(-2*8)"
        assertEquals(expected, parserOld.preprocessNamedFunctions(input, StringBuilder(), 0, input.length))
    }

    @Test
    fun infixToPrefixTestSimple() {
        var input = "1+3-2"
        var expected = listOf<String>("+","-","2","3","1")
        assertEquals(expected, parserOld.infixToPrefixTokenize(input))
        input = "5/2+1"
        expected = listOf<String>("+","1","/","2","5")
        assertEquals(expected, parserOld.infixToPrefixTokenize(input))
    }

    @Test
    fun infixToPrefixMultiDigit() {
        val input = "01+13-02" // 1+31-20 reversed
        val expected = listOf<String>("+","-","20","31","10")
        assertEquals(expected, parserOld.infixToPrefixTokenize(input))
    }

    @Test
    fun infixToPrefixTestPriority() {
        var input = "1+3*2"
        var expected = listOf<String>("+", "*", "2", "3", "1")
        assertEquals(expected, parserOld.infixToPrefixTokenize(input))
        // Parentheses appear reversed because the input string is processed in reverse order
        input = ")1+3(*2"
        expected = listOf<String>("*","2","+","3","1")
        assertEquals(expected, parserOld.infixToPrefixTokenize(input))
    }

    @Test
    fun infixToPrefixTestNamedFunctions() {
        var input = ")3(wop)2(" // pow(3,2) reversed
        var expected = listOf<String>("pow", "2", "3")
        assertEquals(expected, parserOld.infixToPrefixTokenize(input))
        input = ")3(wop)2+2(/)2+5(wop)3(" // pow(3,2+2)/pow(2+5,3) reversed
        expected = listOf<String>("/","pow", "3", "+", "5", "2", "pow", "+", "2", "2", "3")
        assertEquals(expected, parserOld.infixToPrefixTokenize(input))
    }

    @Test
    fun generateASTTest() {
        var input = listOf<String>("-","2","+","32","1")
        var expected = BinOp('-',  NumberExpr(2.0), BinOp('+', NumberExpr(32.0), NumberExpr(1.0)))
        assertEquals(expected, parserOld.generateAST(input, 0 ).first)
        input = listOf<String>("pow","~","20","/","2","3")
        expected = BinOp('^', UnOp('-', NumberExpr(20.0)), BinOp('/', NumberExpr(2.0), NumberExpr(3.0)))
        assertEquals(expected, parserOld.generateAST(input, 0).first)
    }

    @Test
    fun parseASTTest() {
        var input = BinOp('-', BinOp('+', NumberExpr(1.0), NumberExpr(3.0)), NumberExpr(2.0))
        var expected = 2.0
        assertEquals(expected, parserOld.parseAST(input), 0.01)
        input = BinOp('^' , BinOp('+', UnOp('-', NumberExpr(-2.0)), NumberExpr(3.0)), NumberExpr(2.0))
        expected = 25.0
        assertEquals(expected, parserOld.parseAST(input), 0.01)
    }

    @Test
    fun parseCellTestSimple() {
        table.setValueAt("1+3", 2, 3)
        var expected = 4.0
        assertEquals(expected, parserOld.parseCell(2,3), 0.01)
        table.setValueAt("(((1-3)))", 2, 3)
        expected = -2.0
        assertEquals(expected, parserOld.parseCell(2,3), 0.01)
    }

    @Test
    fun parseCellTestReferences() {
        table.setValueAt("1", 0, 1)
        table.setValueAt("3", 1, 2)
        table.setValueAt("A1+B2", 2, 3)
        val expected = 4.0
        assertEquals(expected, parserOld.parseCell(2,3), 0.01)
    }

    @Test
    fun parseCellComplex() {
        table.setValueAt("16/8", 0, 1)
        table.setValueAt("8-5-1", 1, 2)
        table.setValueAt("pow(-2, A1 - 3) * (42 + B2)", 2, 3)
        var expected = -22.0
        assertEquals(expected, parserOld.parseCell(2,3), 0.01)
        table.setValueAt("sqrt(64) + fact(2)", 0, 1)
        table.setValueAt("max(A1, 2)", 1, 2)
        table.setValueAt("min(3, B2) % 5", 5, 6)
        expected = 3.0
        assertEquals(expected, parserOld.parseCell(5,6), 0.01)
    }


}