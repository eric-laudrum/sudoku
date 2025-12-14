package com.example.sudoku

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private var cellViews: Array<Array<TextView>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Status bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Generate Grid
        gridLayout = findViewById(R.id.grid)

        val observer = gridLayout.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (gridLayout.width > 0 && gridLayout.height > 0 && cellViews == null) {

                    createBoard()

                    // Remove listener so it doesn't run again
                    gridLayout.post { gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this) }
                }
            }
        })
    }

    private fun createBoard() {
        val boardSize = 9
        gridLayout.columnCount = boardSize
        gridLayout.rowCount = boardSize

        gridLayout.removeAllViews()

        val cells = Array(boardSize) { row ->
            Array(boardSize) { col ->
                TextView(this).apply {
                    layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(row, 1f),
                        GridLayout.spec(col, 1f)
                    ).apply {
                        width = 0
                        height = 0
                    }

                    gravity = Gravity.CENTER
                    textSize = 14f
                    setBackgroundResource(R.drawable.cell_border)
                    setTextColor(Color.DKGRAY)
                    text = "$row,$col"
                }
            }
        }
        cellViews = cells

        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                gridLayout.addView(cells[row][col])
            }
        }
    }
}
