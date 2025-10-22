// File: app/src/main/java/com/chessanalyzer/databinding/OverlayControlBinding.kt
package com.chessanalyzer.databinding

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import com.chessanalyzer.R

class OverlayControlBinding private constructor(
    val root: View
) {
    val btnStop: ImageButton = root.findViewById(R.id.btnStop)
    val tvBestMove: TextView = root.findViewById(R.id.tvBestMove)
    val tvEvaluation: TextView = root.findViewById(R.id.tvEvaluation)
    val tvTime: TextView = root.findViewById(R.id.tvTime)
    val speedSpinner: Spinner = root.findViewById(R.id.speedSpinner)
    val btnPause: Button = root.findViewById(R.id.btnPause)

    companion object {
        fun inflate(inflater: LayoutInflater): OverlayControlBinding {
            val view = inflater.inflate(R.layout.overlay_control, null, false)
            return OverlayControlBinding(view)
        }
    }
}
