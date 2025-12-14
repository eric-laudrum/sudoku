package com.example.sudoku

import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    // ---------- Variables ----------
    private lateinit var gridLayout: GridLayout
    private lateinit var cellViews: Array<Array<TextView>>

    // ---------- Create ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        gridLayout = findViewById(R.id.grid)

        gridLayout.post{
            createBoard()
        }
    }

    // ---------- Functions ----------
    private fun createBoard() {
        val boardSize = 9
        cellViews = Array(boardSize){ row ->
            Array(boardSize) {col ->
                TextView(this).apply{
                    layoutParams = GridLayout.LayoutParams().apply{
                        width = gridLayout.width / boardSize
                        height = gridLayout.height / boardSize
                        rowSpec = GridLayout.spec(row)
                        columnSpec = GridLayout.spec(col)
                    }

                    gravity = Gravity.CENTER
                    textSize = 20f
                    setBackgroundResource(R.drawable.cell_border)

                }
            }
        }


        // Add TextViews to Grid
        for(row in 0 until boardSize){
            for(col in 0 until boardSize){
                gridLayout.addView(cellViews[row][col])
            }
        }
    }
}


