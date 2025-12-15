// In SudokuGenerator.kt
package com.example.sudoku

import kotlin.random.Random

class GameGenerator {
    private lateinit var solution: Array<IntArray>

    fun generatePuzzle(): Pair<Array<IntArray>, Array<IntArray>>{
        // Initialize board
        solution = Array(9){IntArray(9)}

        solve(0, 0)

        // Create puzzle
        val puzzle = createPuzzle()
        return Pair(puzzle, solution)
    }

    private fun createPuzzle(): Array<IntArray>{
        // Create a copy of the solution
        var puzzle = solution.map { it.clone() }.toTypedArray()

        for(number in 1..9){
            // Find all locations of number
            val locations = (0..8).flatMap{ r->
                (0..8).mapNotNull {c ->
                    if(puzzle[r][c] == number) r to c else null
                }
            }.shuffled().toMutableList()


            val numbersToRemove = if(locations.size > 1){
                Random.nextInt(1, locations.size)
            } else{
                0 // Leave at least 1 number
            }

            // Remove numbers from the board
            repeat(numbersToRemove){
                val loc = locations.removeAt(0)
                puzzle[loc.first][loc.second] = 0 // "Poke a hole" in the board
            }
        }
        return puzzle
    }

    private fun solve(row: Int, col: Int): Boolean {
        if (row == 9) {
            return true
        }

        // Determine the next cell to solve
        var nextRow = row
        var nextCol = col + 1
        if (nextCol == 9) {
            nextRow++
            nextCol = 0
        }

        if (solution[row][col] != 0) return solve(nextRow, nextCol)

        val numbersToTry = (1..9).shuffled(Random)

        for (num in numbersToTry) {
            if (isValid(row, col, num)) {
                solution[row][col] = num
                if (solve(nextRow, nextCol)) return true
                solution[row][col] = 0 // Backtrack
            }
        }
        return false
    }

    private fun isValid(row: Int, col: Int, num: Int): Boolean {
        // 1. Check row
        for (c in 0..8) {
            if (solution[row][c] == num) {
                return false
            }
        }
        // 2. Check column
        for (r in 0..8) {
            if (solution[r][col] == num) {
                return false
            }
        }
        // 3. Check box
        val boxStartRow = row - row % 3
        val boxStartCol = col - col % 3
        for (r in 0..2) {
            for (c in 0..2) {
                if (solution[boxStartRow + r][boxStartCol + c] == num) {
                    return false
                }
            }
        }

        // Placement is valid
        return true
    }
}
