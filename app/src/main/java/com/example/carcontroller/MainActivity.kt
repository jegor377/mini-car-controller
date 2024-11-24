package com.example.carcontroller

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.example.carcontroller.ui.theme.CarControllerTheme
import com.google.android.material.snackbar.Snackbar


class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var listView: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            Log.w("MainActivity", "DANE")
        }
    }
    private var carControllerActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode != Activity.RESULT_OK) {
            val toast = Toast.makeText(this, "Nie udało się połączyć! :(", Toast.LENGTH_SHORT)
            toast.show()
            Log.e("MainActivity", "Nie udało się połączyć:(")
        }
    }
    private lateinit var coordinator: CoordinatorLayout
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.w("MainActivity", "SIEMA");
        setContentView(R.layout.activity_main)
        listView = findViewById(R.id.bluetoothDevicesList)
        coordinator = findViewById(R.id.coordinatorLayout)

        // Check if Bluetooth permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            // Bluetooth permissions already granted
            // Perform your desired Bluetooth operation here
            Log.w("MainActivity", "BLT permissions granted")
            scanBluetoothDevices()
        } else {
            // Request Bluetooth permissions
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), BLUETOOTH_PERMISSION_REQUEST_CODE)
        }


        findViewById<Button>(R.id.scanBluetoothButton)
            .setOnClickListener {
                scanBluetoothDevices()
            }
    }

    private fun scanBluetoothDevices() {
        Log.w("MainActivity", "Scanning for bluetooth devices")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.w("MainActivity", "bluetooth adapter is null")
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultLauncher.launch(enableBtIntent)
            Log.w("MainActivity", "BLT IS NOT EN")
        } else {
            Log.w("MainActivity", "BLT is EN")
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTH_PERMISSION_REQUEST_CODE)
        }
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val devicesNames = arrayListOf<String>()
        val devices = arrayListOf<BluetoothDevice>()

        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            devicesNames.add(deviceName)
            devices.add(device)
            Log.w("MainActivity", "Urządzenie $deviceName")
        }

        deviceListAdapter = ArrayAdapter<String>(this, R.layout.bluetooth_device, devicesNames)
        listView.adapter = deviceListAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Log.w("MainActivity", "Kliknięto: $position ${devices[position].address}")
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
            val carControllerActivity = Intent(this, CarControllerActivity::class.java)
            val controllerBundle = bundleOf()
            controllerBundle.putParcelable("device", devices[position])
            carControllerActivity.putExtras(controllerBundle)
            carControllerActivityResult.launch(carControllerActivity)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CarControllerTheme {
        Greeting("Android")
    }
}