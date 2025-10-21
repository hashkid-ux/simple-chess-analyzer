package com.chessanalyzer.utils

import android.content.Context
import android.content.SharedPreferences
import com.chessanalyzer.data.model.AnalysisSpeed

class PreferenceManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    fun setAnalysisSpeed(speed: AnalysisSpeed) {
        prefs.edit().putString(KEY_ANALYSIS_SPEED, speed.name).apply()
    }
    
    fun getAnalysisSpeed(): AnalysisSpeed {
        val speedName = prefs.getString(KEY_ANALYSIS_SPEED, AnalysisSpeed.BLITZ.name)
        return try {
            AnalysisSpeed.valueOf(speedName ?: AnalysisSpeed.BLITZ.name)
        } catch (e: Exception) {
            AnalysisSpeed.BLITZ
        }
    }
    
    fun setAutoStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }
    
    fun getAutoStart(): Boolean {
        return prefs.getBoolean(KEY_AUTO_START, false)
    }
    
    fun setShowEvaluation(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_EVAL, show).apply()
    }
    
    fun getShowEvaluation(): Boolean {
        return prefs.getBoolean(KEY_SHOW_EVAL, true)
    }
    
    companion object {
        private const val PREFS_NAME = "chess_analyzer_prefs"
        private const val KEY_ANALYSIS_SPEED = "analysis_speed"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_SHOW_EVAL = "show_evaluation"
    }
}
