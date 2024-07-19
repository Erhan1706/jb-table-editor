package tokenizer

interface Token

data class NumberToken(val num: Int) : Token
object OpenBracketToken: Token
object CloseBracketToken : Token
object CommaToken: Token
data class OperatorToken(val symbol: Char) : Token {
    companion object {
        val MINUS = OperatorToken('-')
        val PLUS = OperatorToken('+')
        val DIVIDE = OperatorToken('/')
        val MULT = OperatorToken('*')
        val UNARY = OperatorToken('u')
    }
}
data class CellReferenceToken(val ref: String) : Token
data class FunctionToken(val func: String) : Token
class EndToken : Token


class TokenIterator(private val str: String) : CharIterator() {

    private var i = 0

    override fun hasNext(): Boolean = i < str.length - 1

    override fun nextChar(): Char = str[++i]

    fun peek(): Char = str[i]

    fun peekNext(): Char = str[i + 1]

}

class Tokenizer {

    private lateinit var it: TokenIterator
    private var lastAdded: Boolean = false

    fun tokenize(input: String): List<Token> {
        it = TokenIterator(input)
        lastAdded = false
        val res = mutableListOf<Token>()
        while (it.hasNext()) {
            val token = generateToken()
            if (token is EndToken) return res
            else res.add(token)
            if (it.hasNext()) it.nextChar()
        }
        if (!lastAdded) res.add(generateToken()) // Add the final token since the hasNext call does not cover it

        return res
    }

    private fun generateToken(): Token {
        while (it.peek().isWhitespace() ) {
            it.nextChar()
            if (!it.hasNext()) return EndToken()
        }
        val c = it.peek()

        return when {
            c == '(' -> OpenBracketToken
            c == ')' -> CloseBracketToken
            c == ',' -> CommaToken
            c.isDigit() -> {
                NumberToken(parseSequence(c, Character::isDigit, String::toInt))
            }
            isOperand(c) -> OperatorToken(c)
            c.isLetter() -> {
                var str = ""
                str += c
                if (it.peekNext().isDigit()) {
                    CellReferenceToken(c.toString() + it.nextChar())
                } else {
                    FunctionToken(parseSequence(c, Character::isLetter, String::toString))
                }
            }
            else -> throw IllegalArgumentException("Unexpected character: $c")
        }
    }

    private fun <T> parseSequence(
        initialChar: Char,
        isValidChar: (Char) -> Boolean,
        convertResult: (String) -> T
    ): T {
        var sequence = ""
        sequence += initialChar
        while (it.hasNext() && isValidChar(it.peekNext())) {
            sequence += it.nextChar()
        }
        if (!it.hasNext()) lastAdded = true
        return convertResult(sequence)
    }

    private fun isOperand(symbol: Char): Boolean {
        return setOf('*', '+', '-', '/').contains(symbol)
    }

}

