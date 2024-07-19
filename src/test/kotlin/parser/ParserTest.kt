package parser

import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import tokenizer.NumberToken
import tokenizer.OperatorToken
import tokenizer.Token
import tokenizer.Tokenizer

class ParserTest {

    private val tk = Tokenizer()
    private val pr = Parser()

    @Test
    fun basicTest() {
        val input = tk.tokenize("1+2")
        Assert.assertEquals(3, pr.parse(input))
    }

    @Test
    fun precedenceTest() {
        val input = tk.tokenize("1+2*3")
        Assert.assertEquals(7, pr.parse(input))
    }

    @Test
    fun parenthesisTest() {
        val input = tk.tokenize("(1+2)*3")
        Assert.assertEquals(9, pr.parse(input))
    }

    @Test
    fun doubleParenthesisTest() {
        val input = tk.tokenize("(3+2-(1+2))*3")
        Assert.assertEquals(6, pr.parse(input))
    }

    @Test
    fun unaryTest() {
        val input = tk.tokenize("-1+3")
        Assert.assertEquals(2, pr.parse(input))
    }

    @Test
    fun multiUnary() {
        val input = tk.tokenize("--1--3")
        Assert.assertEquals(4, pr.parse(input))
    }

    @Test
    fun complexExpression() {
        val input = tk.tokenize("10 - (-3 * (4 - 7))")
        Assert.assertEquals(1, pr.parse(input))
    }


}