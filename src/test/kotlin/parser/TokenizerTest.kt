package parser;

import org.junit.Assert.assertEquals
import org.junit.Test;
import tokenizer.*

public class TokenizerTest {

    @Test
    fun basicTest() {
        val tk = Tokenizer()
        val exp = listOf<Token>(NumberToken(1), OperandToken('+'), NumberToken(2))
        assertEquals(exp , tk.tokenize("1+2"))
    }

    @Test
    fun basicTestWithSpaces() {
        val tk = Tokenizer()
        val exp = listOf<Token>(NumberToken(1), OperandToken('+'), NumberToken(2))
        assertEquals(exp , tk.tokenize("    1   +   2    "))
    }

    @Test
    fun multiOperands() {
        val tk = Tokenizer()
        val exp = listOf<Token>(NumberToken(1), OperandToken('*'), NumberToken(23), OperandToken('+'),
        NumberToken(64), OperandToken('/'), NumberToken(1245))
        assertEquals(exp , tk.tokenize( "1*23+64/1245"))
    }

    @Test
    fun basicFunction() {
        val tk = Tokenizer()
        val exp = listOf(FunctionToken("pow"), OpenBracketToken, NumberToken(1), CommaToken, NumberToken(2), CloseBracketToken)
        assertEquals(exp , tk.tokenize("pow(1,2)"))
    }

    @Test
    fun nestedFunction() {
        val tk = Tokenizer()
        val exp = listOf(FunctionToken("pow"), OpenBracketToken, FunctionToken("pow"), OpenBracketToken, NumberToken(13),
        OperandToken('-'), NumberToken(1), CommaToken, NumberToken(1), CloseBracketToken, CommaToken, NumberToken(2), CloseBracketToken)
        assertEquals(exp , tk.tokenize("pow(pow(13-1,1),2)"))
    }


}
