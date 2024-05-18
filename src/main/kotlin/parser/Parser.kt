package parser

import java.lang.StringBuilder
import javax.swing.JTable
import kotlin.math.pow

class Parser(private val table: JTable) {

    fun parseCell(selectedRow: Int, selectedCol: Int): Double {
        var formula = calculateReferences(table.model.getValueAt(selectedRow, selectedCol).toString())
        formula = removeSpaces(formula)
        formula = transformUnaryMinus(formula)
        if (formula.isEmpty()) return 0.0
        val tokenizedFormula = tokenizeFormula(formula)
        val cur = infixToPrefixTokenize(tokenizedFormula.reversed())
        val (ast, _) = generateAST(cur, 0)
        val result = parseAST(ast)
        table.model.setValueAt(result, selectedRow, selectedCol)
        // println(parseAST(ast))
        return result
    }

    private fun removeSpaces(formula: String): String {
        return formula.filter { !it.isWhitespace() }
    }

    private fun isOperand(c: Char): Boolean {
        return setOf<Char>('+', '-', '*', '/', '~').contains(c)
    }

    private fun isOperandString(s: String): Boolean {
        return setOf<String>("+", "-", "*", "/", "~").contains(s)
    }

    private fun isNumber(c: Char): Boolean {
       return ('0'..'9').toSet().contains(c) || c == '.' // for decimal support
    }

    private fun isNumberString(s: String): Boolean {
        if (s.isEmpty()) return false
        for (char in s) {
            if (!isNumber(char)) return false
        }
        return true
    }

    private fun isNamedFunction(s: String): Boolean {
        return setOf<String>("pow", "sqrt").contains(s)
    }

    fun mapCoordinatesToTableCell(coord: String): Pair<Int, Int> {
        val row = coord.substring(1).toInt() - 1
        val col = coord[0] - 'A' + 1
        return Pair(row, col)
    }

    fun calculateReferences(formula: String): String  {
        // Define the regular expression pattern to match an uppercase letter followed by one or more digits
        val pattern = Regex("([A-Z])(\\d+)")
        val result = pattern.replace(formula) { coord ->
            val (row, col) = mapCoordinatesToTableCell(coord.value)
            parseCell(row, col).toString()
        }
        return result
    }

    fun transformUnaryMinus(formula: String): String {
        var lastElement: Char = ' '
        var res: StringBuilder = StringBuilder()
        for (c in formula) {
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
        return res.toString()
    }

    fun tokenizeFormula(formula: String): String {
        val res = StringBuilder()
        var i = 0
        while (i < formula.length) {
            var cur: Char = formula[i]
            if (!cur.isLetter()) {
                res.append(cur)
                i++
            }
            val str = StringBuilder()
            while (cur.isLetter()) {
                str.append(cur)
                cur = formula[++i]
            }
            if (isNamedFunction(str.toString()) && cur == '(') {
                i++
                res.append('(')
                while (formula[i] != ',') {
                    res.append(formula[i])
                    i++
                }
                res.append(')')
                res.append(str)
                res.append('(')
                i++
                while (formula[i] != ')') {
                    res.append(formula[i])
                    i++
                }
                res.append(')')
                i++
            }
        }
        return res.toString()
    }

    fun infixToPrefixTokenize(formula: String): List<String> {
        val res = ArrayList<String>()
        // The ~ character is used to represent the unitary - sign
        val operandsPrio: Map<String, Int> = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "~" to 4, "pow" to 3)
        val stack: ArrayDeque<String> = ArrayDeque()

        var i = 0
        while (i < formula.length) {
            val cur = formula[i]
            when {
                cur.isWhitespace() -> {
                    i++
                }
                isNumber(cur) -> {
                    val number = StringBuilder()
                    while (i < formula.length && isNumber(formula[i])) {
                        number.insert(0, formula[i])
                        i++
                    }
                    res.add(number.toString())
                    continue
                }
                cur == ')' -> {
                    stack.addLast(")")
                }
                cur == '(' -> {
                    while (stack.last() != ")") {
                        res.add(stack.removeLast())
                    }
                    stack.removeLast()
                }
                isOperand(cur) -> {
                    if (stack.isEmpty() || stack.last() == "(") {
                        stack.addLast(cur.toString())
                    } else {
                        val last = stack.last()
                        if ((operandsPrio[last] ?: 0) >= (operandsPrio[cur.toString()] ?: 0)) {
                            res.add(stack.removeLast())
                        }
                        stack.addLast(cur.toString())
                    }
                }
                else -> {
                    val str = StringBuilder()
                    while (i < formula.length && formula[i].isLetter()) {
                        str.append(formula[i])
                        i++
                    }
                    val strName = str.reversed().toString()
                    if (isNamedFunction(strName)) {
                        val last = stack.lastOrNull()
                        if ((operandsPrio[last] ?: 0) >= (operandsPrio[strName] ?: 0)) {
                            res.add(stack.removeLast())
                        }
                        stack.addLast(strName)
                    }
                    continue
                }
            }
            i++
        }
        while (stack.isNotEmpty()) {
            res.add(stack.removeLast().toString())
        }

        println(res.reversed())
        return res.reversed()
    }

    fun generateAST(prefix: List<String>, index: Int): Pair<Expression, Int> {
        val cur: String = prefix[index]
        if (isNumberString(cur)) {
            return Pair(NumberExpr(cur.toDouble()), index + 1)
        } else if (isOperandString(cur)) {
            when (cur) {
                "+" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    return Pair(BinOp('+', left, right), finalIndex)
                }
                "-" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    return Pair(BinOp('-', left, right), finalIndex)
                }
                "*" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    return Pair(BinOp('*', left, right), finalIndex)
                }
                "/" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    return Pair(BinOp('/', left, right), finalIndex)
                }
                "~" -> {
                    val (operand, nextIndex) = generateAST(prefix, index + 1)
                    return Pair(UnOp('-', operand), nextIndex)
                }
                else -> throw IllegalArgumentException("Unknown operator")
            }
        } else if (isNamedFunction(cur)) {
            when (cur) {
                "pow" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    return Pair(BinOp('^', left, right), finalIndex)
                }
                else -> throw IllegalArgumentException("Unknown function")
            }
        }
        throw IllegalArgumentException("Unknown expression")
    }

    fun parseAST(expr: Expression): Double {
        return when (expr) {
            is NumberExpr -> expr.num
            is BinOp -> when (expr.op) {
                '+' -> parseAST(expr.l) + parseAST(expr.r)
                '-' -> parseAST(expr.l) - parseAST(expr.r)
                '*' -> parseAST(expr.l) * parseAST(expr.r)
                '/' -> parseAST(expr.l) / parseAST(expr.r)
                '^' -> parseAST(expr.l).pow(parseAST(expr.r))
                else -> throw IllegalArgumentException("Unknown binary operator")
            }
            is UnOp -> when (expr.op) {
                '-' -> parseAST(expr.expr) * -1
                else -> throw IllegalArgumentException("Unknown unary operator")
            }
        }
    }

}