package parser

import java.lang.StringBuilder
import javax.swing.JTable

class Parser(private val table: JTable) {

    fun parseCell(selectedRow: Int, selectedCol: Int) {
        var cur = table.model.getValueAt(selectedRow, selectedCol)
        cur = transformUnaryOperators(cur.toString())
        if (cur.isEmpty()) return
        cur = infixToPrefix(cur.reversed())
        val (result, _) = syntaxTransform(cur, 0)

        table.model.setValueAt(prefixParser(result), selectedRow, selectedCol)
        println(prefixParser(result))
    }

    fun isOperand(c: Char): Boolean {
        return setOf<Char>('+', '-', '*', '/', '~').contains(c)
    }

    fun isOperandString(s: String): Boolean {
        return setOf<String>("+", "-", "*", "/", "~").contains(s)
    }

    fun isNumber(c: Char): Boolean {
       return ('0'..'9').toSet().contains(c) || c == '.' // for decimal support
    }

    fun isNumberString(s: String): Boolean {
        if (s.isEmpty()) return false
        for (char in s) {
            if (!isNumber(char)) return false
        }
        return true
    }

    fun transformUnaryOperators(formula: String): String {
        var lastElement: Char = ' '
        var res: StringBuilder = StringBuilder()
        val strippedFormula = formula.filter { !it.isWhitespace() }
        for (c in strippedFormula) {
            if (res.isEmpty() && c == '-') {
                res.append('~')
                lastElement = '~'
            } else if (isOperand(c) && (isOperand(lastElement) || lastElement == '(' )){
                if (c == '+') continue
                else if (c == '-') {
                    res.append('~')
                    lastElement = c
                }
            } else {
                res.append(c)
                lastElement = c
            }
        }
        //println(res.toString())
        return res.toString()
    }


    fun infixToPrefix(formula: String): List<String> {
        var res = ArrayList<String>()
        // The ~ character is used to represent the unitary - sign
        val operandsPrio: Map<Char, Int> = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2, '~' to 3)
        var stack: ArrayDeque<Char> = ArrayDeque()

        var i = 0
        while (i < formula.length) {
            var cur = formula[i]
            if (cur.isWhitespace()) {
                i++
                continue
            }
            else if (isNumber(cur)) {
                val number = StringBuilder()
                while (i < formula.length && isNumber(formula[i])) {
                    number.insert(0, formula[i])
                    i++
                }
                res.add(number.toString())
                continue
            }
            else if (cur == ')') {
                stack.addLast(')')
            } else if (cur == '(') {
                while (stack.last() != ')') {
                    res.add(stack.removeLast().toString())
                }
                stack.removeLast()
            } else if (isOperand(cur)) {
                if (stack.isEmpty()) {
                    // If the first element is a - operator then it's unary
                    stack.addLast(cur)
                    i++
                    continue
                }
                val last = stack.last()
                if ((operandsPrio[last] ?: 0) >= (operandsPrio[cur] ?: 0)) {
                    res.add(stack.removeLast().toString())
                    stack.addLast(cur)
                } else stack.addLast(cur)
            }
            i++
        }
        while (stack.isNotEmpty()) {
            res.add(stack.removeLast().toString())
        }

        println(res.reversed())
        return res.reversed()
    }

    fun syntaxTransform(prefix: List<String>, index: Int): Pair<Expression, Int> {
        var cur: String = prefix[index]
        if (isNumberString(cur)) {
            return Pair(NumberExpr(cur.toDouble()), index + 1)
        } else if (isOperandString(cur)) {
            when (cur) {
                "+" -> {
                    val (left, nextIndex) = syntaxTransform(prefix, index + 1)
                    val (right, finalIndex) = syntaxTransform(prefix, nextIndex)
                    return Pair(BinOp('+', left, right), finalIndex)
                }
                "-" -> {
                    val (left, nextIndex) = syntaxTransform(prefix, index + 1)
                    val (right, finalIndex) = syntaxTransform(prefix, nextIndex)
                    return Pair(BinOp('-', left, right), finalIndex)
                }
                "*" -> {
                    val (left, nextIndex) = syntaxTransform(prefix, index + 1)
                    val (right, finalIndex) = syntaxTransform(prefix, nextIndex)
                    return Pair(BinOp('*', left, right), finalIndex)
                }
                "/" -> {
                    val (left, nextIndex) = syntaxTransform(prefix, index + 1)
                    val (right, finalIndex) = syntaxTransform(prefix, nextIndex)
                    return Pair(BinOp('/', left, right), finalIndex)
                }
                "~" -> {
                    val (operand, nextIndex) = syntaxTransform(prefix, index + 1)
                    return Pair(UnOp('-', operand), nextIndex)
                }
                else -> throw IllegalArgumentException("Unknown operator")
            }
        }
        throw IllegalArgumentException("Unknown expression")
    }

    fun prefixParser(expr: Expression): Double {
        return when (expr) {
            is NumberExpr -> expr.num
            is BinOp -> when (expr.op) {
                '+' -> prefixParser(expr.l) + prefixParser(expr.r)
                '-' -> prefixParser(expr.l) - prefixParser(expr.r)
                '*' -> prefixParser(expr.l) * prefixParser(expr.r)
                '/' -> prefixParser(expr.l) / prefixParser(expr.r)
                else -> throw IllegalArgumentException("Unknown binary operator")
            }
            is UnOp -> when (expr.op) {
                '-' -> prefixParser(expr.expr) * -1
                else -> throw IllegalArgumentException("Unknown unary operator")
            }
            else -> throw IllegalArgumentException("Unknown expression")
        }

    }

}