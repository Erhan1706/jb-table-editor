package ui

import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane

class MainFrame: JFrame() {


    init {
        title = "Table Editor"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        setSize(600, 330)
        setLocationRelativeTo(null)


        val table = Table(15, 9) // Change here the amount of columns and rows of the table
        add(table, BorderLayout.CENTER)
        add(JScrollPane(table))
        table.label.horizontalAlignment = JLabel.CENTER
        table.label.foreground = Color.RED
        add(table.label, BorderLayout.SOUTH)

        isVisible = true
    }

}