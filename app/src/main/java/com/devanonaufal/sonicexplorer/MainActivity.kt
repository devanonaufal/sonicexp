package com.devanonaufal.sonicexplorer

import android.Manifest
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_AUDIO_PERMISSION_CODE = 200

    private lateinit var textLocation: TextView
    private lateinit var textDecibel: TextView
    private lateinit var imageFrequency: ImageView
    private lateinit var textFrequencyDescription: TextView
    private lateinit var textTime: TextView
    private lateinit var buttonShowLocation: Button
    private lateinit var buttonToggleSoundMeter: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi View
        textLocation = findViewById(R.id.textLocation)
        textDecibel = findViewById(R.id.textDecibel)
        imageFrequency = findViewById(R.id.imageFrequency)
        textFrequencyDescription = findViewById(R.id.textFrequencyDescription)
        textTime = findViewById(R.id.textTime)
        buttonShowLocation = findViewById(R.id.buttonShowLocation)
        buttonToggleSoundMeter = findViewById(R.id.buttonToggleSoundMeter)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Tombol untuk Menampilkan Lokasi GPS
        buttonShowLocation.setOnClickListener {
            showLocation()
        }

        // Tombol untuk Menghidupkan/Mematikan Sound Meter
        buttonToggleSoundMeter.setOnClickListener {
            if (isRecording) {
                stopSoundMeter()
            } else {
                startSoundMeter()
            }
        }

        // Menjalankan jam real-time
        startRealTimeClock()

        // Tambahkan listener untuk salin koordinat lokasi
        textLocation.setOnLongClickListener {
            val coordinates = textLocation.text.toString().replace("Location: ", "")
            copyTextToClipboard(coordinates)
            true
        }
    }

    private fun startRealTimeClock() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        handler.post(object : Runnable {
            override fun run() {
                val currentTime = timeFormat.format(Date())
                textTime.text = "Time: $currentTime"
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun showLocation() {
        if (checkPermissions()) {
            startLocationUpdates()
        } else {
            requestPermissions()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                textLocation.text = "Location: ${location.latitude}, ${location.longitude}"
            }
        }
    }

    private fun startSoundMeter() {
        if (checkPermissions()) {
            isRecording = true
            buttonToggleSoundMeter.text = "Stop Sound Meter"
            startDecibelMeasurement()
        } else {
            requestPermissions()
        }
    }

    private fun stopSoundMeter() {
        isRecording = false
        buttonToggleSoundMeter.text = "Start Sound Meter"
        stopDecibelMeasurement()
        textDecibel.text = "Decibel: 0 dB" // Reset decibel display to 0
    }

    private fun startDecibelMeasurement() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION_CODE)
            return
        }

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                val outputFile = "${cacheDir.absolutePath}/temp_audio.3gp"
                setOutputFile(outputFile)
                prepare()
                start()
            }

            handler.post(object : Runnable {
                override fun run() {
                    if (isRecording) {
                        try {
                            mediaRecorder?.maxAmplitude?.let { maxAmplitude ->
                                if (maxAmplitude > 0) {
                                    val decibel = 20 * Math.log10(maxAmplitude.toDouble() * 2)
                                    textDecibel.text = "Decibel: ${decibel.toInt()} dB"
                                    animateImageView(decibel.toFloat())
                                } else {
                                    textDecibel.text = "Decibel: N/A"
                                }
                            }
                            handler.postDelayed(this, 200)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error in Decibel Measurement: ${e.message}")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting MediaRecorder: ${e.message}")
        }
    }



    private fun animateImageView(decibel: Float) {
        // Mengonversi desibel menjadi pergeseran vertikal
        val translationY = -decibel * 2  // Atur skala sesuai preferensi Anda
        val animator = ObjectAnimator.ofFloat(imageFrequency, "translationY", translationY)
        animator.duration = 200 // Sesuaikan durasi animasi agar selaras dengan interval pembaruan
        animator.interpolator = LinearInterpolator()
        animator.start()
    }

    private fun stopDecibelMeasurement() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping MediaRecorder: ${e.message}")
        }
    }

    private fun checkPermissions(): Boolean {
        val audioPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val locationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return audioPermission == PackageManager.PERMISSION_GRANTED && locationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION),
            101
        )
    }

    private fun copyTextToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Coordinates", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                if (isRecording) {
                    startSoundMeter()
                } else {
                    startLocationUpdates()
                }
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
