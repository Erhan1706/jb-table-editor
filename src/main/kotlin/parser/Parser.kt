package parser

import java.lang.StringBuilder
import javax.swing.JTable

class Parser(private val table: JTable) {

    fun parseCell(selectedRow: Int, selectedCol: Int) {
        var cur = table.model.getValueAt(selectedRow, selectedCol)
        cur = transformUnaryOperators(cur.toString())
        if (cur.isEmpty()) return
        cur = infixToPrefix(cur.reversed())
        prefixParser(cur, 0)
    }

    fun isOperand(c: Char): Boolean {
        return setOf<Char>('+', '-', '*', '/', '~').contains(c)
    }

    fun isNumber(c: Char): Boolean {
       return ('0'..'9').toSet().contains(c)
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


    fun infixToPrefix(formula: String): String {
        val res = StringBuilder("")
        // The ~ character is used to represent the unitary - sign
        val operandsPrio: Map<Char, Int> = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2, '~' to 3)
        var stack: ArrayDeque<Char> = ArrayDeque()

        for (c in formula) {
            var cur = c
            if (cur.isWhitespace()) continue
            else if (isNumber(cur)) {
                res.append("$cur ")
            }
            else if (cur == ')') {
                stack.addLast(')')
            } else if (cur == '(') {
                while (stack.last() != ')') {
                    res.append("${stack.removeLast()} ")
                }
                stack.removeLast()
            } else if (isOperand(cur)) {
                if (stack.isEmpty()) {
                    // If the first element is a - operator then it's unary
                    stack.addLast(cur)
                    continue
                }
                val last = stack.last()
                if ((operandsPrio[last] ?: 0) >= (operandsPrio[cur] ?: 0)) {
                    res.append("${stack.removeLast()} ")
                    stack.addLast(cur)
                } else stack.addLast(cur)
            }
        }
        while (stack.isNotEmpty()) {
            res.append("${stack.removeLast()} ")
        }

        println(res.reversed().toString())
        return res.reversed().toString()
    }

    fun prefixParser(prefix: String, index: Int): Int {
        var cur: Char = prefix[index]
        while (cur.isWhitespace()) {
            cur = prefix[index + 1]
        }
        if (isNumber(cur)) {
            return cur.digitToInt()
        } else if (isOperand(cur)) {
            when (cur) {
                '+' -> return prefixParser("",0) + prefixParser("",0)
            }
        }
        return 0
    }

}