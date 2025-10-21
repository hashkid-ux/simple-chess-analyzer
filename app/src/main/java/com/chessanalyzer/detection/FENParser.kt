package com.chessanalyzer.detection

import com.chessanalyzer.data.model.PlayerColor

class FENParser {
    
    /**
     * Parse FEN string to get position details
     */
    fun parseFEN(fen: String): FENData {
        val parts = fen.split(" ")
        
        return FENData(
            position = parts.getOrNull(0) ?: "",
            activeColor = if (parts.getOrNull(1) == "b") PlayerColor.BLACK else PlayerColor.WHITE,
            castlingRights = parts.getOrNull(2) ?: "-",
            enPassant = parts.getOrNull(3) ?: "-",
            halfmoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0,
            fullmoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1
        )
    }
    
    /**
     * Convert square notation to board coordinates
     */
    fun squareToCoordinates(square: String): Pair<Int, Int> {
        if (square.length != 2) return Pair(-1, -1)
        
        val file = square[0] - 'a' // 0-7
        val rank = square[1] - '1' // 0-7
        
        return Pair(file, rank)
    }
    
    /**
     * Convert board coordinates to square notation
     */
    fun coordinatesToSquare(file: Int, rank: Int): String {
        if (file !in 0..7 || rank !in 0..7) return ""
        return "${('a' + file)}${('1' + rank)}"
    }
    
    /**
     * Parse UCI move (e.g., "e2e4") to from/to squares
     */
    fun parseUCIMove(uciMove: String): UCIMove? {
        if (uciMove.length < 4) return null
        
        val fromSquare = uciMove.substring(0, 2)
        val toSquare = uciMove.substring(2, 4)
        val promotion = if (uciMove.length > 4) uciMove[4] else null
        
        return UCIMove(fromSquare, toSquare, promotion)
    }
    
    data class FENData(
        val position: String,
        val activeColor: PlayerColor,
        val castlingRights: String,
        val enPassant: String,
        val halfmoveClock: Int,
        val fullmoveNumber: Int
    )
    
    data class UCIMove(
        val from: String,
        val to: String,
        val promotion: Char?
    )
}
