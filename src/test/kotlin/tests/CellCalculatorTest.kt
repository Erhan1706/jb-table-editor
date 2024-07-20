package tests

import org.junit.Assert
import org.junit.Test
import ui.Table

class CellCalculatorTest {

    private val table: Table = Table(10, 10)
    private val cellCalculator: CellCalculator = CellCalculator(table)

    @Test
    fun parseCellTestReferences() {
        table.setValueAt("1", 0, 1)
        table.setValueAt("3", 1, 2)
        table.setValueAt("A1+B2", 2, 3)
        Assert.assertEquals(4, cellCalculator.evaluateCell (2, 3))
    }

    @Test
    fun parseCellComplex() {
        table.setValueAt("24/8", 0, 1)
        table.setValueAt("8-5-1", 1, 2)
        table.setValueAt("pow(-2, A1 - 3) * (42 + B2)", 2, 3)
        Assert.assertEquals(44, cellCalculator.evaluateCell(2, 3))
    }



}