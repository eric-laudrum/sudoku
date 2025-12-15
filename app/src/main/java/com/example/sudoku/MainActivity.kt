package com.example.sudoku

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Variables
    private lateinit var gridLayout: GridLayout
    private var cellViews: Array<Array<TextView>>? = null
    private var solutionBoard: Array<IntArray>? = null

    // On Create
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

                    // Remove listener to stop from running again
                    gridLayout.post { gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this) }
                }
            }
        })
    }

    // Functions
    private fun createBoard() {
        // Generate solution
        solutionBoard = GameGenerator().generate()

        val boardSize = 9
        gridLayout.columnCount = boardSize

        gridLayout.rowCount = boardSize
        gridLayout.removeAllViews()

        val cells = Array(boardSize) { row ->
            Array(boardSize) { col ->
                val cell = TextView(this).apply {
                    layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(row, 1f),
                        GridLayout.spec(col, 1f)
                    ).apply {
                        width = 0
                        height = 0
                    }

                    gravity = Gravity.CENTER
                    textSize = 20f
                    setTextColor(Color.BLACK)

                    val number = solutionBoard!![row][col]
                    text = if (number == 0) "" else number.toString()

                    // Create cell borders
                    val border = GradientDrawable()
                    border.setColor(Color.WHITE)

                    // Set line thicknesses
                    val thick = 6
                    val thin = 2

                    // Set border thickness for each side
                    val top = if (row % 3 == 0) thick else thin
                    val bottom = if (row == 8) thick else 0
                    val left = if (col % 3 == 0) thick else thin
                    val right = if (col == 8) thick else 0

                    // Create a LayerDrawable to combine strokes correctly
                    val inset = GradientDrawable()
                    inset.setColor(Color.WHITE)

                    val layers = arrayOf(border, inset)
                    val layerList = android.graphics.drawable.LayerDrawable(layers)

                    // Set the insets to reveal the borders underneath
                    layerList.setLayerInset(1, left, top, right, bottom)

                    // Set all borders to thick lines
                    border.setStroke(thick, Color.BLACK)

                    // Apply the final layered drawable as the background
                    this.background = layerList
                }
                gridLayout.addView(cell)
                cell
            }
        }
        cellViews = cells
    }
}