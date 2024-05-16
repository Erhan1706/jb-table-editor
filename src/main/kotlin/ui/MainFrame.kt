package ui

import Parser
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import javax.swing.table.DefaultTableModel

class MainFrame: JFrame() {


    init {
        title = "Table Editor"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        setSize(600, 400)
        setLocationRelativeTo(null)


        val table = Table(10, 9)
        add(table, BorderLayout.CENTER)
        add(JScrollPane(table))

        isVisible = true
    }

}