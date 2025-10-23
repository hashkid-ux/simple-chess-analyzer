// File: app/src/main/java/com/chessanalyzer/ChessAnalyzerApp.kt
package com.chessanalyzer

import android.app.Application
import com.chessanalyzer.data.repository.ChessEngineRepository
import com.chessanalyzer.utils.PreferenceManager

class ChessAnalyzerApp : Application() {
    
    lateinit var preferenceManager: PreferenceManager
        private set
    
    lateinit var engineRepository: ChessEngineRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        engineRepository = ChessEngineRepository()
    }
}
