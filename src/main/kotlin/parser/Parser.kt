package parser

import ui.Table
import java.lang.StringBuilder
import kotlin.math.pow

class Parser(private val table: Table) {

    /**
     * Parses the formula of a cell and updates it so that it contains the final result. This will also recursively
     * parse and update any cells that are referenced in the formula.
     */
    fun parseCell(selectedRow: Int, selectedCol: Int): Double {
        // Start by identifying any references to other cells and replacing them with their values
        try {
            var formula: String = calculateReferences(table.model.getValueAt(selectedRow, selectedCol).toString())
            if (formula.isEmpty()) return 0.0

            formula = removeSpaces(formula) // Remove all whitespaces
            formula = handleUnaryMinus(formula) // Substitute unary minus with ~ character
            formula = preprocessNamedFunctions(formula)
            val tokens: List<String> = infixToPrefixTokenize(formula.reversed())
            val (ast,_) = generateAST(tokens, 0)
            val result = parseAST(ast)
            table.model.setValueAt(result, selectedRow, selectedCol)
            return result
        } catch (e: Exception) {
            table.model.setValueAt("", selectedRow, selectedCol)
            table.label.text = "Error: Invalid formula"
            return -1.0
        }
    }

    /** Removes all whitespaces from the formula string */
    private fun removeSpaces(formula: String): String {
        return formula.filter { !it.isWhitespace() }
    }

    /** Checks if char is an operand supported by the parser*/
    private fun isOperand(c: Char): Boolean {
        return setOf<Char>('+', '-', '*', '/', '~', '%').contains(c)
    }

    /** Checks if string is an operand supported by the parser*/
    private fun isOperandString(s: String): Boolean {
        return setOf<String>("+", "-", "*", "/", "~", "%").contains(s)
    }

    /** Checks if char is a number */
    private fun isNumber(c: Char): Boolean {
       return ('0'..'9').toSet().contains(c) || c == '.' // . included for decimal support
    }

    /** Checks if string is a multi-digit number */
    private fun isNumberString(s: String): Boolean {
        if (s.isEmpty()) return false
        for (char in s) {
            if (!isNumber(char)) return false
        }
        return true
    }

    /** Checks if string is one of the supported named function */
    private fun isNamedFunction(s: String): Boolean {
        return setOf<String>("pow", "sqrt").contains(s)
    }

    /**
     * Maps a string representation of a cell coordinate (e.g. "B2") to the row and column indices of the table
     * Note that the row index is 0-based while the column index is 1-based, thus A1 starts at (0, 1) and not (0,0).
     */
    fun mapCoordinatesToTableCell(coord: String): Pair<Int, Int> {
        val row = coord.substring(1).toInt() - 1
        val col = coord[0] - 'A' + 1
        return Pair(row, col)
    }

    /**
     * For all cell references in the formula, replace them with their corresponding values from the table. This
     * will call parseCell for each reference to ensure that the final value is calculated and returned.
     */
    fun calculateReferences(formula: String): String  {
        // Define the regular expression pattern to match an uppercase letter followed by one or more digits
        val pattern = Regex("([A-Z])(\\d+)")
        val result = pattern.replace(formula) { coord ->
            val (row, col) = mapCoordinatesToTableCell(coord.value)
            parseCell(row, col).toString()
        }
        return result
    }

    /**
     * Transforms the unary minus sign into a ~ character, such that the parser can differentiate between
     * the binary and unary minus. 2 rules are applied to detect a unary minus sign:
     * 1. If the first character is a minus sign, it is a unary minus
     * 2. If the minus sign is preceded by an operand or an opening parenthesis, it is a unary minus
     */
    fun handleUnaryMinus(formula: String): String {
        var lastElement: Char = ' '
        var res: StringBuilder = StringBuilder()
        for (c in formula) {
            // Check if first character is a minus sign
            if (res.isEmpty() && c == '-') {
                res.append('~')
                lastElement = '~'
            // Check if minus sign is preceded by an operand or an opening parenthesis
            } else if (isOperand(c) && (isOperand(lastElement) || lastElement == '(' )){
                if (c == '+') continue // skip unary +, since they don't change anything
                else if (c == '-') {
                    res.append('~')
                    lastElement = '~'
                }
            } else {
                res.append(c)
                lastElement = c
            }
        }
        return res.toString()
    }

    /**
     * Preprocesses named functions in the formula string, such that they can be interpreted by the parser. The
     * named functions are reformatted so that they can  resemble binary operators. For example,
     * "pow(2,3)" is transformed to "(2)pow(3)". For more examples see the test cases.
     */
    fun preprocessNamedFunctions(formula: String): String {
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

    /**
     * Converts the formula string from infix to prefix notation. The algorithm is based on the shunting-yard algorithm.
     * Then it tokenizes the prefix expression and returns a list of tokens. The input formula should be reversed
     * before calling this function.
     */
    fun infixToPrefixTokenize(formula: String): List<String> {
        val res = ArrayList<String>()
        // The ~ character is used to represent the unitary - sign. Higher number -> higher priority
        val operandsPrio: Map<String, Int> = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "%" to 2,"~" to 4, "pow" to 3)
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
                // Parentheses appear reversed because the input string is processed in reverse order
                cur == ')' -> {
                    stack.addLast(")")
                }
                cur == '(' -> {
                    while (stack.last() != ")") {
                        res.add(stack.removeLast())
                    }
                    if (stack.isNotEmpty()) stack.removeLast()
                }
                isOperand(cur) -> {
                    if (stack.isEmpty() || stack.last() == "(") {
                        stack.addLast(cur.toString())
                    } else {
                        while (stack.isNotEmpty() &&
                            (operandsPrio[stack.last()] ?: 0) > (operandsPrio[cur.toString()] ?: 0)) {
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
            res.add(stack.removeLast())
        }

        println(res.reversed())
        return res.reversed()
    }

    /**
     * Generates the  Abstract Syntax Tree (AST) AST from the prefix expression. The AST represents the hierarchical
     * structure of the expression.
     * @return the root of the AST and the index of the next element in the prefix expression. The index is used to
     * correctly parse the next element in recursive calls, in case there are nested expressions.
     */
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
                "%" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    return Pair(BinOp('%', left, right), finalIndex)
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

    /**
     * Parses the given AST and evaluates the expression represented by it, returns the final result as a Double.
     */
    fun parseAST(expr: Expression): Double {
        return when (expr) {
            is NumberExpr -> expr.num
            is BinOp -> when (expr.op) {
                '+' -> parseAST(expr.l) + parseAST(expr.r)
                '-' -> parseAST(expr.l) - parseAST(expr.r)
                '*' -> parseAST(expr.l) * parseAST(expr.r)
                '/' -> parseAST(expr.l) / parseAST(expr.r)
                '%' -> parseAST(expr.l) % parseAST(expr.r)
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