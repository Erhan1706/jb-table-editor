package parser

import parser.expressions.BinOp
import parser.expressions.Expression
import parser.expressions.NumberExpr
import parser.expressions.UnOp
import ui.Table
import java.lang.StringBuilder
import kotlin.math.*

class ParserOld(private val table: Table) {

    /**
     * Parses the formula of a cell and updates it so that it contains the final result. This will also recursively
     * parse and update any cells that are referenced in the formula.
     */
    fun parseCell(selectedRow: Int, selectedCol: Int): Double {
        try {
            // Start by identifying any references to other cells and replacing them with their values
            var formula: String = calculateReferences(table.model.getValueAt(selectedRow, selectedCol).toString())
            if (formula.isEmpty()) return 0.0

            formula = removeSpaces(formula) // Remove all whitespaces
            formula = handleUnaryMinus(formula) // Substitute unary minus with ~ character
            // Preprocess named functions to a format that can be parsed
            val processedFormula = preprocessNamedFunctions(formula, StringBuilder(), 0, formula.length)
            val tokens: List<String> = infixToPrefixTokenize(processedFormula.reversed()) // Convert infix to prefix notation and tokenize
            val (ast,_) = generateAST(tokens, 0) // Generate the AST from the tokens
            val result = parseAST(ast) // Parse the AST and calculate the result
            if  (result % 1.0 == 0.0) { // If the result doesn't have any decimal places, show it as an int
                table.model.setValueAt(result.toInt(), selectedRow, selectedCol)
            } else {
                table.model.setValueAt(String.format("%.2f", result), selectedRow, selectedCol)
            }
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
        return setOf('+', '-', '*', '/', '~', '%').contains(c)
    }

    /** Checks if string is an operand supported by the parser*/
    private fun isOperandString(s: String): Boolean {
        return setOf("+", "-", "*", "/", "~", "%").contains(s)
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
        return setOf("pow", "sqrt", "e", "fact", "max", "min").contains(s)
    }

    /** Returns the number of arguments a named function takes */
    private fun getNumArgsNamedFunctions(s: String): Int {
        return when (s) {
            "pow" -> 2
            "sqrt" -> 1
            "e" -> 1
            "fact" -> 1
            "max" -> 2
            "min" -> 2
            else -> 0
        }
    }

    /**
     * Maps a string representation of a cell coordinate (e.g. "B2") to the row and column indices of the table
     * Note that the row index is 0-based while the column index is 1-based, thus A1 starts at (0, 1) and not (0,0).
     */
    fun mapCoordinatesToTableCell(coord: String): Pair<Int, Int> {
        if (coord[1].isDigit()) {
            val row = coord.substring(1).toInt() - 1
            val col = coord[0] - 'A' + 1
            return Pair(row, col)
        } else {
            val row = coord.substring(2).toInt() - 1
            val col = convertColumnToIndex(coord.substring(0, 2))
            return Pair(row, col)
        }
    }

    /** Converts an Excel-style column label (e.g., "AA") to a one-based column index */
    private fun convertColumnToIndex(column: String): Int {
        var index = 0
        for (i in column.indices) {
            index = index * 26 + (column[i] - 'A' + 1)
        }
        return index
    }

    /**
     * For all cell references in the formula, replace them with their corresponding values from the table. If the
     * reference cell contains a formula that needs to be calculated, this will call parseCell for each reference to
     * ensure that the final value is returned.
     */
    fun calculateReferences(formula: String): String  {
        // Define the regular expression pattern to match 1 or more uppercase letter followed by one or more digits
        val pattern = Regex("([A-Z]+)(\\d+)")
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
        val res = StringBuilder()
        for (c in formula) {
            if (res.isEmpty() && c== '+') continue // skip unary +, since they don't change anything
            // Check if first character is a minus sign
            else if (res.isEmpty() && c == '-') {
                res.append('~')
                lastElement = '~'
            // Check if minus sign is preceded by an operand or an opening parenthesis
            } else if (isOperand(c) && (isOperand(lastElement) || lastElement == '(' )){
                if (c == '+') continue // skip unary +, since they don't change anything
                else if (c == '-') {
                    res.append('~')
                    lastElement = '~'
                } else throw IllegalArgumentException("Two illegal operators in a row")
            } else {
                res.append(c)
                lastElement = c
            }
        }
        return res.toString()
    }

    /**
     * Preprocesses named functions in the formula string, such that they can be interpreted by the parser. The
     * named functions are reformatted so that they can resemble binary operators. For example,
     * "pow(2,3)" is transformed to "(2)pow(3)". For more examples see the test cases.
     */
    fun preprocessNamedFunctions(formula: String, res: StringBuilder, curIndex: Int,  endIndex: Int): String {
        var i = curIndex
        while (i < endIndex) {
            var cur: Char = formula[i]
            // Skip non-letter characters
            if (!cur.isLetter()) {
                res.append(cur)
                i++
            } else {
                val str = StringBuilder()
                while (cur.isLetter()) {
                    str.append(cur)
                    cur = formula[++i]
                }
                // Check if the string is a named function and is followed by an opening parenthesis
                if (isNamedFunction(str.toString()) && cur == '(') {
                    if (getNumArgsNamedFunctions(str.toString()) == 2) {
                        res.append(formula[i++])
                        while (i < formula.length && formula[i] != ',') {
                            res.append(formula[i])
                            i++
                        }
                        res.append(')')
                        i = rewriteNamedFunction(res, formula, i, str.toString())
                    } else if (getNumArgsNamedFunctions(str.toString()) == 1) {
                        i = rewriteNamedFunction(res, formula, i, str.toString())
                    }
                } else throw IllegalArgumentException("Invalid format of named function")
            }
        }
        return res.toString()
    }

    /** Helper function that rewrites named function to the correct format supported by the parser */
    private fun rewriteNamedFunction(res: StringBuilder, formula: String, i: Int, functionName: String): Int {
        var index = i
        res.append(functionName)
        res.append('(')
        index++
        while (index < formula.length && formula[index] != ')') {
            res.append(formula[index])
            index++
        }
        res.append(')')
        index++
        return index
    }

    /**
     * Converts the formula string from infix to prefix notation. The algorithm is based on the shunting-yard algorithm.
     * Then it tokenizes the prefix expression and returns a list of tokens. The input formula should be reversed
     * before calling this function.
     */
    fun infixToPrefixTokenize(formula: String): List<String> {
        val res = ArrayList<String>()
        // The ~ character is used to represent the unitary - sign. Higher number -> higher priority
        val operatorsPrio: Map<String, Int> = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "%" to 2,"~" to 4,
            "pow" to 3, "max" to 3, "min" to 3, "sqrt" to 4, "e" to 4, "fact" to 4)
        val stack: ArrayDeque<String> = ArrayDeque()

        var i = 0
        while (i < formula.length) {
            val cur = formula[i]
            when {
                cur.isWhitespace() -> i++
                isNumber(cur) -> {
                    val number = StringBuilder()
                    // Iterate for multi-digit numbers
                    while (i < formula.length && isNumber(formula[i])) {
                        number.insert(0, formula[i]) // Insert at the beginning since number is reversed
                        i++
                    }
                    res.add(number.toString()) // Add the number to the result directly
                    continue
                }
                // Parentheses appear reversed because the input string is processed in reverse order
                cur == ')' -> {
                    stack.addLast(")")
                }
                cur == '(' -> {
                    // Pop operators from the stack to the result until a closing parenthesis is found
                    while (stack.last() != ")") {
                        res.add(stack.removeLast())
                    }
                    if (stack.isNotEmpty()) stack.removeLast()
                }
                isOperand(cur) -> {
                    // If the stack is empty or the last element is a closing parenthesis, add the operand to the stack
                    if (stack.isEmpty() || stack.last() == ")") {
                        stack.addLast(cur.toString())
                    } else {
                        // Pop operators with higher precedence from the stack to the result
                        while (stack.isNotEmpty() &&
                            (operatorsPrio[stack.last()] ?: 0) > (operatorsPrio[cur.toString()] ?: 0)) {
                            res.add(stack.removeLast())
                        }
                        stack.addLast(cur.toString())
                    }
                }
                // If it's none of the above then it's a string
                else -> {
                    val str = StringBuilder()
                    // Iterate for multi-letter strings
                    while (i < formula.length && formula[i].isLetter()) {
                        str.append(formula[i])
                        i++
                    }
                    val strName = str.reversed().toString()
                    // Check if the string is one of the supported named functions
                    if (isNamedFunction(strName)) {
                        val last = stack.lastOrNull()
                        if ((operatorsPrio[last] ?: 0) >= (operatorsPrio[strName] ?: 0)) {
                            res.add(stack.removeLast())
                        }
                        stack.addLast(strName)
                    } else throw IllegalArgumentException("Invalid character in formula")
                    continue
                }
            }
            i++
        }
        // Remove the remaining elements in the stack
        while (stack.isNotEmpty()) {
            res.add(stack.removeLast())
        }
        println(res.reversed())
        return res.reversed()
    }

    /**
     * Generates the Abstract Syntax Tree (AST) from the prefix expression. The AST represents the hierarchical
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
            return when (cur) {
                "pow" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    Pair(BinOp('^', left, right), finalIndex)
                }
                "sqrt" -> {
                    val (operand, nextIndex) = generateAST(prefix, index + 1)
                    Pair(UnOp('√', operand), nextIndex)
                }
                "e" -> {
                    val (operand, nextIndex) = generateAST(prefix, index + 1)
                    Pair(UnOp('e', operand), nextIndex)
                }
                "fact" -> {
                    val (operand, nextIndex) = generateAST(prefix, index + 1)
                    Pair(UnOp('!', operand), nextIndex)
                }
                "max" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    Pair(BinOp('m', left, right), finalIndex)
                }
                "min" -> {
                    val (left, nextIndex) = generateAST(prefix, index + 1)
                    val (right, finalIndex) = generateAST(prefix, nextIndex)
                    Pair(BinOp('n', left, right), finalIndex)
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
                'm' -> max(parseAST(expr.l), parseAST(expr.r))
                'n' -> min(parseAST(expr.l), parseAST(expr.r))
                else -> throw IllegalArgumentException("Unknown binary operator")
            }
            is UnOp -> when (expr.op) {
                '-' -> parseAST(expr.expr) * -1
                '√' -> sqrt(parseAST(expr.expr))
                'e' -> E.pow(parseAST(expr.expr))
                '!' -> factorial(parseAST(expr.expr))
                else -> throw IllegalArgumentException("Unknown unary operator")
            }
            else -> throw IllegalArgumentException("Unknown expression")
        }
    }

    private fun factorial(n: Double): Double {
        if (n < 0) throw IllegalArgumentException("Factorial is not defined for negative numbers")
        return if (n == 0.0 || n == 1.0) 1.0 else n * factorial(n - 1)
    }

}