// In SudokuGenerator.kt
package com.example.sudoku

import kotlin.random.Random

class GameGenerator {
private var board: Array<IntArray> = Array(9) { IntArray(9) }

    fun generate(): Array<IntArray> {
        // Clear the board to start fresh
        board = Array(9) { IntArray(9) }
        // Start the recursive solving process from the top-left cell (0, 0)
        solve(0, 0)
        return board
    }

    private fun solve(row: Int, col: Int): Boolean {
        // --- Base Case: If we've moved past the last cell, we're done!
        if (row == 9) {
            // Game Over - SUCCESS
            return true
        }

        // Determine the next cell to solve
        var nextRow = row
        var nextCol = col + 1
        if (nextCol == 9) {
            nextRow++
            nextCol = 0
        }

        // Attempt 1-9 in the current cell
        // If current cell is already filled, move to next cell
        if (board[row][col] != 0) {
            return solve(nextRow, nextCol)
        }

        // Shuffle numbers 1-9
        val numbersToTry = (1..9).shuffled(Random)

        for (num in numbersToTry) {

            // Validate number placement
            if (isValid(row, col, num)) {
                // Place number if valid
                board[row][col] = num

                // Recursively try to solve the rest of the board from the next cell
                if (solve(nextRow, nextCol)) {
                    return true // If the rest of the board was solved, we're done
                }

                // Dead end
                // Reset the cell and try the next number in our loop.
                board[row][col] = 0
            }
        }

        // Attempted all numbers - return false to trigger backtracking
        return false
    }

    private fun isValid(row: Int, col: Int, num: Int): Boolean {
        // 1. Check row
        for (c in 0..8) {
            if (board[row][c] == num) {
                return false
            }
        }
        // 2. Check column
        for (r in 0..8) {
            if (board[r][col] == num) {
                return false
            }
        }
        // 3. Check box
        val boxStartRow = row - row % 3
        val boxStartCol = col - col % 3
        for (r in 0..2) {
            for (c in 0..2) {
                if (board[boxStartRow + r][boxStartCol + c] == num) {
                    return false
                }
            }
        }

        // Placement is valid
        return true
    }
}
