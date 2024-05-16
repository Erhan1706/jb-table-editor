package ui

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

        layout = GridLayout(2,2, 20, 20)

        //model.setRowIdentifiers(rowHeaders)
        add(JButton("1"))
        add(JButton("2"))
        add(JButton("3"))
        add(JButton("4"))


        setSize(600, 400)
        setLocationRelativeTo(null)
        isVisible = true
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                //TableEditor().isVisible = true
            }
        }
    }
}