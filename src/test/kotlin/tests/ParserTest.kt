package tests

import org.junit.Assert
import org.junit.Test
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

    @Test
    fun functionTest() {
        val input = tk.tokenize("pow((2+1),3)")
        Assert.assertEquals(27, pr.parse(input))
    }

    @Test
    fun nestedFunctionTest() {
        val input = tk.tokenize("max(pow(6-4,4),3)")
        Assert.assertEquals(16, pr.parse(input))
    }

    @Test
    fun associativityTest() {
        var input = tk.tokenize("8-5-1")
        Assert.assertEquals(2, pr.parse(input))
        input = tk.tokenize("16/4/2")
        Assert.assertEquals(2, pr.parse(input))

    }


}