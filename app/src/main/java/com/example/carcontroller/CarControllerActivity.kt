package com.example.carcontroller

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import java.util.concurrent.LinkedBlockingQueue

class CarControllerActivity : ComponentActivity() {
    private lateinit var bltThread: ConnectThread
    private lateinit var queue: LinkedBlockingQueue<CarMove>
    private lateinit var leftSpeedSeekBar: SeekBar
    private lateinit var rightSpeedSeekBar: SeekBar
    private lateinit var progressBar: ProgressBar
    private lateinit var controllerUI: LinearLayout
    private lateinit var turnLeftBtn: Button
    private lateinit var turnRightBtn: Button
    private lateinit var moveForwardBtn: Button
    private lateinit var moveBackwardBtn: Button

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the navigation and bottom buttons
        // Hide the navigation and bottom buttons
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions


        setContentView(R.layout.car_controller)
        leftSpeedSeekBar = findViewById(R.id.leftSpeed)
        rightSpeedSeekBar = findViewById(R.id.rightSpeed)
        progressBar = findViewById(R.id.progressBar)
        controllerUI = findViewById(R.id.controllerUI)
        turnLeftBtn = findViewById(R.id.turnLeftBtn)
        turnRightBtn = findViewById(R.id.turnRightBtn)
        moveForwardBtn = findViewById(R.id.moveForwardBtn)
        moveBackwardBtn = findViewById(R.id.moveBackwardBtn)

        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            controllerUI.visibility = View.GONE
        }

        leftSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2) {
                    sendMoveCmd(p1, rightSpeedSeekBar.progress)
                    Log.w("CarControl", "Current progress $p1 ${rightSpeedSeekBar.progress}")
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                if(p0 != null) {
                    p0.progress = 50
                    sendMoveCmd(p0.progress, rightSpeedSeekBar.progress)
                }
            }
        })

        rightSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2) {
                    sendMoveCmd(leftSpeedSeekBar.progress, p1)
                    Log.w("CarControl", "Current progress ${leftSpeedSeekBar.progress} $p1")
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                if(p0 != null) {
                    p0.progress = 50
                    sendMoveCmd(leftSpeedSeekBar.progress, p0.progress)
                }
            }
        })

        turnLeftBtn.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendMoveCmd(51, 49)
                }

                MotionEvent.ACTION_UP -> {
                    sendMoveCmd(leftSpeedSeekBar.progress, rightSpeedSeekBar.progress)
                }
            }
            false
        }

        turnRightBtn.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendMoveCmd(49, 51)
                }

                MotionEvent.ACTION_UP -> {
                    sendMoveCmd(leftSpeedSeekBar.progress, rightSpeedSeekBar.progress)
                }
            }
            false
        }

        moveForwardBtn.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendMoveCmd(49, 49)
                }

                MotionEvent.ACTION_UP -> {
                    sendMoveCmd(leftSpeedSeekBar.progress, rightSpeedSeekBar.progress)
                }
            }
            false
        }

        moveBackwardBtn.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendMoveCmd(51, 51)
                }

                MotionEvent.ACTION_UP -> {
                    sendMoveCmd(leftSpeedSeekBar.progress, rightSpeedSeekBar.progress)
                }
            }
            false
        }

        queue = LinkedBlockingQueue()
        val bundle: Bundle ?= intent.extras
        val device: BluetoothDevice ?= bundle!!.getParcelable("device")
        if(device != null) {
            Log.w("CarControllerActivity", "device is set!")
            Log.w("CarControllerActivity", "Trying to connect...")
            bltThread = ConnectThread(
                device,
                queue,
                {onConnected()},
                {err -> onConnectionFailed(err)},
                {onDisconnected()}
            )
            bltThread.start()
        } else {
            Log.w("CarControllerActivity", "device is null")
        }
    }

    private fun onFailed() {
        setResult(Activity.RESULT_CANCELED)
    }

    private fun onSucceeded() {
        setResult(Activity.RESULT_OK)
    }

    override fun onDestroy() {
        super.onDestroy()
        bltThread.cancel()
        onFailed()
    }

    private fun onConnected() {
        Log.w("CarControllerActivity", "Connected! :)")
        runOnUiThread {
            progressBar.visibility = View.GONE
            controllerUI.visibility = View.VISIBLE
        }
    }

    private fun onConnectionFailed(err: Exception) {
        Log.e("CarControllerActivity", "Connection failed! :(")
        onFailed()
        finish()
    }

    private fun onDisconnected() {
        onSucceeded()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        onSucceeded()
        onBackPressedDispatcher.onBackPressed()
    }

    private fun sendMoveCmd(left:Int, right:Int) {
        val cmd = CarMove(
            steeringToPWM(normSeekBar(left)),
            steeringToPWM(normSeekBar(right))
        )
        queue.remainingCapacity()
        queue.put(cmd)
    }

    private fun normSeekBar(value: Int): Float {
        return (50 - value.toFloat()) / 50
    }

    private fun steeringToPWM(steering: Float): Int {
        return (sign(steering) * 100 + steering * 155).toInt()
    }

    private fun sign(value: Float): Int {
        if(value == 0.0f) return 0
        return if (value > 0)  1 else -1
    }
}