package tests

import tokenizer.Tokenizer
import ui.Table

class CellCalculator(private val table: Table) {

    private val tokenizer: Tokenizer = Tokenizer()
    private val parser: Parser = Parser()

    fun evaluateCell(selectedRow: Int, selectedCol: Int): Int {
        var formula = table.model.getValueAt(selectedRow, selectedCol).toString()
        formula = calculateReferences(formula)
        val tokens = tokenizer.tokenize(formula)
        val result = parser.parse(tokens)
        table.model.setValueAt(result, selectedRow, selectedCol)

        return result
    }

    /**
     * For all cell references in the formula, replace them with their corresponding values from the table. If the
     * reference cell contains a formula that needs to be calculated, this will call parseCell for each reference to
     * ensure that the final value is returned.
     */
    private fun calculateReferences(formula: String): String  {
        // Define the regular expression pattern to match 1 or more uppercase letter followed by one or more digits
        val pattern = Regex("([A-Z]+)(\\d+)")
        val result = pattern.replace(formula) { coord ->
            val (row, col) = mapCoordinatesToTableCell(coord.value)
            evaluateCell(row, col).toString()
        }
        return result
    }

    /**
     * Maps a string representation of a cell coordinate (e.g. "B2") to the row and column indices of the table
     * Note that the row index is 0-based while the column index is 1-based, thus A1 starts at (0, 1) and not (0,0).
     */
    private fun mapCoordinatesToTableCell(coord: String): Pair<Int, Int> {
        return if (coord[1].isDigit()) {
            val row = coord.substring(1).toInt() - 1
            val col = coord[0] - 'A' + 1
            Pair(row, col)
        } else {
            val row = coord.substring(2).toInt() - 1
            val col = convertColumnToIndex(coord.substring(0, 2))
            Pair(row, col)
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

}