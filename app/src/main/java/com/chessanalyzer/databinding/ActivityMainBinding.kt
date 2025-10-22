// File: app/src/main/java/com/chessanalyzer/databinding/ActivityMainBinding.kt
package com.chessanalyzer.databinding

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.chessanalyzer.R

class ActivityMainBinding private constructor(
    val root: View
) {
    val speedGroup: RadioGroup = root.findViewById(R.id.speedGroup)
    val rbBullet: RadioButton = root.findViewById(R.id.rbBullet)
    val rbBlitz: RadioButton = root.findViewById(R.id.rbBlitz)
    val rbRapid: RadioButton = root.findViewById(R.id.rbRapid)
    val rbClassical: RadioButton = root.findViewById(R.id.rbClassical)
    val tvSpeedInfo: TextView = root.findViewById(R.id.tvSpeedInfo)
    val switchAutoStart: SwitchCompat = root.findViewById(R.id.switchAutoStart)
    val switchShowEval: SwitchCompat = root.findViewById(R.id.switchShowEval)
    val btnStart: Button = root.findViewById(R.id.btnStart)

    companion object {
        fun inflate(inflater: LayoutInflater): ActivityMainBinding {
            val view = inflater.inflate(R.layout.activity_main, null, false)
            return ActivityMainBinding(view)
        }
    }
}
