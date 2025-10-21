package com.chessanalyzer.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chessanalyzer.ChessAnalyzerApp
import com.chessanalyzer.databinding.ActivityMainBinding
import com.chessanalyzer.service.BoardDetectionService
import com.chessanalyzer.service.OverlayService
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val app get() = application as ChessAnalyzerApp
    
    private var mediaProjectionResultCode: Int = 0
    private var mediaProjectionData: Intent? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        binding.apply {
            // Speed selection
            speedGroup.setOnCheckedChangeListener { _, checkedId ->
                val speed = when (checkedId) {
                    binding.rbBullet.id -> com.chessanalyzer.data.model.AnalysisSpeed.BULLET
                    binding.rbBlitz.id -> com.chessanalyzer.data.model.AnalysisSpeed.BLITZ
                    binding.rbRapid.id -> com.chessanalyzer.data.model.AnalysisSpeed.RAPID
                    binding.rbClassical.id -> com.chessanalyzer.data.model.AnalysisSpeed.CLASSICAL
                    else -> com.chessanalyzer.data.model.AnalysisSpeed.BLITZ
                }
                app.preferenceManager.setAnalysisSpeed(speed)
                updateSpeedInfo(speed)
            }
            
            // Start button
            btnStart.setOnClickListener {
                if (checkPermissions()) {
                    startAnalyzer()
                } else {
                    requestPermissions()
                }
            }
            
            // Settings switches
            switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
                app.preferenceManager.setAutoStart(isChecked)
            }
            
            switchShowEval.setOnCheckedChangeListener { _, isChecked ->
                app.preferenceManager.setShowEvaluation(isChecked)
            }
        }
    }
    
    private fun loadSettings() {
        val speed = app.preferenceManager.getAnalysisSpeed()
        binding.apply {
            when (speed) {
                com.chessanalyzer.data.model.AnalysisSpeed.BULLET -> rbBullet.isChecked = true
                com.chessanalyzer.data.model.AnalysisSpeed.BLITZ -> rbBlitz.isChecked = true
                com.chessanalyzer.data.model.AnalysisSpeed.RAPID -> rbRapid.isChecked = true
                com.chessanalyzer.data.model.AnalysisSpeed.CLASSICAL -> rbClassical.isChecked = true
            }
            
            switchAutoStart.isChecked = app.preferenceManager.getAutoStart()
            switchShowEval.isChecked = app.preferenceManager.getShowEvaluation()
            
            updateSpeedInfo(speed)
        }
    }
    
    private fun updateSpeedInfo(speed: com.chessanalyzer.data.model.AnalysisSpeed) {
        binding.tvSpeedInfo.text = buildString {
            append("Max Depth: ${speed.maxDepth}\n")
            append("Time Limit: ${speed.timeLimit / 1000}s")
        }
    }
    
    private fun checkPermissions(): Boolean {
        val overlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        
        return overlayPermission && mediaProjectionData != null
    }
    
    private fun requestPermissions() {
        // Request overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            return
        }
        
        // Request screen capture permission
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_SCREEN_CAPTURE
        )
    }
    
    private fun startAnalyzer() {
        // Start overlay service with media projection data
        val overlayIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra(OverlayService.EXTRA_RESULT_CODE, mediaProjectionResultCode)
            putExtra(OverlayService.EXTRA_DATA, mediaProjectionData)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayIntent)
        } else {
            startService(overlayIntent)
        }
        
        Toast.makeText(this, "Chess Analyzer Started", Toast.LENGTH_SHORT).show()
        
        // Minimize app
        moveTaskToBack(true)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    requestPermissions()
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_SCREEN_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mediaProjectionResultCode = resultCode
                    mediaProjectionData = data
                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Screen capture permission required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_SCREEN_CAPTURE = 1002
    }
}
