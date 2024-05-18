package ui

import parser.Parser
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
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
        setupKeyBind()

        parser = Parser(this)

        val centerRenderer = DefaultTableCellRenderer()
        centerRenderer.horizontalAlignment = DefaultTableCellRenderer.CENTER
        getColumnModel().getColumn(0).cellRenderer = centerRenderer
    }

    /** Initializes the column names of the table, starting from A to Z */
    private fun initializeColumnNames(): Array<Char> {
        val columnNames = Array<Char> (columns) {' '}
        for (i in 1 until columnNames.size) {
            columnNames[i] = ('A'.code + i - 1).toChar()
        }
        return columnNames
    }

    /**
     * Initializes the data matrix of the table. It also sets all the row numbers from 1 to n, with n being the number
     * of column the table has.
     */
    private fun initializeData(): Array<Array<Any>> {
        val data = Array(rows) { Array<Any> (columns) {""}}
        for (i in 0 until rows) {
            data[i][0] = i + 1
        }
        return data
    }

    /**
     * Sets up the key bind for the table. When the user presses the 'C' key, the cell that is currently
     * selected will be parsed.
     */
    private fun setupKeyBind(){
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), null);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("C"), "parse")
        actionMap.put("parse", object : AbstractAction() {
            override fun actionPerformed(ae: ActionEvent) {
                parser.parseCell(selectedRow, selectedColumn)
            }
        })
    }

}