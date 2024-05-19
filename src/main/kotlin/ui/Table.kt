package ui

import parser.Parser
import java.awt.Color
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer


class Table(private val rows: Int, private val columns: Int) : JTable() {

    private val parser: Parser
    val label: JLabel = JLabel()

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
        getTableHeader().reorderingAllowed = false

        parser = Parser(this)

        val centerRenderer = DefaultTableCellRenderer()
        centerRenderer.horizontalAlignment = DefaultTableCellRenderer.CENTER
        getColumnModel().getColumn(0).cellRenderer = centerRenderer
    }


    override fun prepareRenderer(renderer: TableCellRenderer,row: Int, column: Int): Component {
        val c = super.prepareRenderer(renderer, row, column)
        //  Alternate row color
        if (!isRowSelected(rows)) c.background = if (row % 2 == 0) background else Color(220,220,220)

        return c
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
                label.text = ""
                parser.parseCell(selectedRow, selectedColumn)
            }
        })
    }

}