import javax.swing.JTable

class Parser(private val table: JTable) {

    fun parseCell(selectedRow: Int, selectedCol: Int) {
        val cur = table.model.getValueAt(selectedRow, selectedCol)
        println("Row: $selectedRow Col: $selectedCol and Val: $cur")
    }

    fun formulaParser(formula: Any) {


    }

}