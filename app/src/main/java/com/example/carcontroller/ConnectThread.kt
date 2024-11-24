package com.example.carcontroller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue

@SuppressLint("MissingPermission")
internal class ConnectThread(
    device: BluetoothDevice,
    private val queue: LinkedBlockingQueue<CarMove>,
    private val onConnected: () -> Unit,
    private val onConnectionFailed: (err: Exception) -> Unit,
    private val onDisconnected: () -> Unit) : Thread() {

    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(device.uuids.first().uuid)
    }

    override fun run() {
        mmSocket?.let { socket ->
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            try {
                socket.connect()
                onConnected()
            } catch(err: Exception) {
                onConnectionFailed(err)
                return
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            while (socket.isConnected) {
                try {
                    val carMove = queue.take()
                    val carMoveBytes = "C${carMove.leftPWM},${carMove.rightPWM};K".toByteArray()
                    socket.outputStream.write(carMoveBytes)
                } catch (err: Exception) {
                    // do nothing
                }
            }
        }
        onDisconnected()
    }

    // Closes the client socket and causes the thread to finish.
    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            Log.e("ConnectThread", "Could not close the client socket", e)
        }
    }
}