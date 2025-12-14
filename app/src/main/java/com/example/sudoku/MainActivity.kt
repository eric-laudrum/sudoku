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

                    // Remove listener so it doesn't run again
                    gridLayout.post { gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this) }
                }
            }
        })
    }

    // Functions
    private fun createBoard() {
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
                    text = "$row,$col" // For debugging

                    // --- Create cell borders ---

                    val border = GradientDrawable()
                    border.setColor(Color.WHITE) // Cell background color

                    // Line thicknesses
                    val thick = 6 // Use a more pronounced thickness in pixels
                    val thin = 2  // A clearly thinner line

                    // Determine border thickness for each side
                    val top = if (row % 3 == 0) thick else thin
                    val bottom = if (row == 8) thick else 0 // Only top/left matter for inner lines
                    val left = if (col % 3 == 0) thick else thin
                    val right = if (col == 8) thick else 0

                    // Create a LayerDrawable to combine strokes correctly
                    // We draw a full "thick" border and then cover it with a white inset shape
                    val inset = GradientDrawable()
                    inset.setColor(Color.WHITE)

                    val layers = arrayOf(border, inset)
                    val layerList = android.graphics.drawable.LayerDrawable(layers)

                    // Set the insets to reveal the borders underneath
                    layerList.setLayerInset(1, left, top, right, bottom)

                    // Set the stroke on the base layer
                    border.setStroke(thick, Color.BLACK) // All borders start thick

                    // Apply the final layered drawable as the background
                    this.background = layerList
                }
                gridLayout.addView(cell)
                cell // Return cell for the array
            }
        }
        cellViews = cells
    }
}