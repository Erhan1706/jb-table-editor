package ui

import parser.Parser
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class Table(private val rows: Int, private val columns: Int) : JTable() {

    private val parser: Parser

    init {
        val data = initializeData()
        val columnNames = initializeColumnNames()
        model = object: DefaultTableModel(data, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                // Make the first column non-editable
                return column != 0
            }
        }
        setSelectionMode(0)
        overrideEnterKeyBind()

        parser = Parser(this)

        val centerRenderer = DefaultTableCellRenderer()
        centerRenderer.horizontalAlignment = DefaultTableCellRenderer.CENTER
        getColumnModel().getColumn(0).cellRenderer = centerRenderer
    }

    private fun initializeColumnNames(): Array<Char> {
        val columnNames = Array<Char> (columns) {'\u0000'}
        // TODO: excel 'AA' when overflow
        for (i in 1 until columnNames.size) {
            columnNames[i] = ('A'.code + i - 1).toChar()
        }
        return columnNames
    }

    private fun initializeData(): Array<Array<Any>> {
        val data = Array(rows) { Array<Any> (columns) {""}}
        for (i in 0 until rows) {
            data[i][0] = i + 1
        }
        return data
    }

    private fun overrideEnterKeyBind(){
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "Enter")
        actionMap.put("Enter", object : AbstractAction() {
            override fun actionPerformed(ae: ActionEvent) {
                parser.parseCell(selectedRow, selectedColumn)
            }
        })
    }

}