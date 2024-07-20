package tests

import tokenizer.*
import kotlin.math.max
import kotlin.math.pow

class Parser {
    private var operatorStack: ArrayDeque<Token> = ArrayDeque()
    private var outputStack: ArrayDeque<Token> = ArrayDeque()

    fun parse(input: List<Token>): Int {
        operatorStack.clear()
        outputStack.clear()
        shuntingYard(input)
        val numStack = ArrayDeque<NumberToken>()
        evalRPN(numStack)

        return numStack.removeLast().num
    }

    private fun evalOperator(numStack: ArrayDeque<NumberToken>, op: Char) {
        val r = numStack.removeLast()
        val l = numStack.removeLast()
        val res: NumberToken = when (op) {
            '+' -> NumberToken(l.num + r.num)
            '-' -> NumberToken(l.num - r.num)
            '/' -> NumberToken(l.num / r.num)
            '*' -> NumberToken(l.num * r.num)
            else -> throw IllegalArgumentException("Unknown operator: $op")
        }
        numStack.add(res)
    }

    private fun evalFunction(numStack: ArrayDeque<NumberToken>, f: FunctionToken) {
        val r = numStack.removeLast()
        val l = numStack.removeLast()
        val res: NumberToken = when (f.func) {
            "pow"-> NumberToken(l.num.toDouble().pow(r.num.toDouble()).toInt())
            "max"-> NumberToken(max(l.num, r.num))
            else -> throw IllegalArgumentException("Unknown function: ${f.func}")
        }
        numStack.add(res)
    }

    private fun evalRPN(numStack: ArrayDeque<NumberToken>) {
        for (token in outputStack) {
            when (token) {
                is NumberToken -> numStack.add(token)
                OperatorToken.PLUS -> evalOperator(numStack, '+')
                OperatorToken.MINUS -> evalOperator(numStack, '-')
                OperatorToken.MULT -> evalOperator(numStack, '*')
                OperatorToken.DIVIDE -> evalOperator(numStack, '/')
                OperatorToken.UNARY -> {
                    val numToken = numStack.removeLast()
                    numStack.add(NumberToken(-numToken.num))
                }
                is FunctionToken -> evalFunction(numStack, token)
            }
        }
        if (numStack.size != 1) throw IllegalArgumentException("Expected result stack to only have 1 element")
    }

    private fun shuntingYard(input: List<Token>) {
        var prevToken: Token? = null
        for (token in input) {
            when (token) {
                is NumberToken -> outputStack.add(token)
                is OperatorToken -> parseOperator(token, prevToken)
                is OpenBracketToken -> operatorStack.add(token)
                is CloseBracketToken -> bracketParse(false)
                is CommaToken -> bracketParse(true)
                is FunctionToken -> operatorStack.add(token)
            }
            prevToken = token
        }

        while (operatorStack.isNotEmpty()) {
            outputStack.add(operatorStack.removeLast())
        }
    }

    private fun getPrecedence(op: OperatorToken): Int {
        return mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2, 'u' to 4)[op.symbol]!!
    }

    private fun parseOperator(token: OperatorToken, prevToken: Token?) {
        if (prevToken == null || prevToken is OpenBracketToken || prevToken is OperatorToken) {
            if (token == OperatorToken.MINUS) {
                operatorStack.add(OperatorToken('u'))
                return
            }
            else if (token == OperatorToken.PLUS) return
        }
        else if (operatorStack.isEmpty()) {
            operatorStack.add(token)
            return
        }
        var last: Token? = operatorStack.last()
        while (last is OperatorToken && getPrecedence(last) >= getPrecedence(token)) {
            outputStack.add(operatorStack.removeLast())
            last = if (operatorStack.isNotEmpty()) operatorStack.last() else null
        }
        operatorStack.add(token)
    }

    private fun bracketParse(comma: Boolean) {
        if (operatorStack.isEmpty()) throw IllegalArgumentException("Expected '(' before ')'")
        var last = operatorStack.last()
        while (last !is OpenBracketToken) {
            outputStack.add(operatorStack.removeLast())
            if (operatorStack.isEmpty()) throw IllegalArgumentException("Mismatched parentheses")
            last = operatorStack.last()
        }
        if (comma) return // In case of comma, cannot remove the left parenthesis thus leave early
        operatorStack.removeLast() // pop the left bracket

        if (operatorStack.isNotEmpty() && operatorStack.last() is FunctionToken)
            outputStack.add(operatorStack.removeLast())
    }

}