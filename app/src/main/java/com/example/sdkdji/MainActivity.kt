package com.example.sdkdji

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.base.BaseProduct.ComponentKey
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import dji.thirdparty.afinal.core.AsyncTask
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.name
    val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
    private val mProduct: BaseProduct? = null
    private var mHandler: Handler? = null

    private val REQUIRED_PERMISSION_LIST = arrayOf(
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE
    )
    private val missingPermission: ArrayList<String> = ArrayList()
    private val isRegistrationInProgress: AtomicBoolean = AtomicBoolean(false)
    private val REQUEST_PERMISSION_CODE = 12345
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }
        mHandler =  Handler(Looper.getMainLooper())

    }

    private fun checkAndRequestPermissions() {
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    eachPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!")
            ActivityCompat.requestPermissions(
                this,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    private fun startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(Runnable {
                showToast("registering, pls wait...")
                DJISDKManager.getInstance()
                    .registerApp(this@MainActivity.applicationContext, object : SDKManagerCallback {
                        override fun onRegister(djiError: DJIError) {
                            if (djiError === DJISDKError.REGISTRATION_SUCCESS) {
                                showToast("Register Success")
                                DJISDKManager.getInstance().startConnectionToProduct()
                            } else {
                                showToast("Register sdk fails, please check the bundle id and network connection!")
                            }
                            Log.v(TAG, djiError.description)
                        }

                        override fun onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect")
                            showToast("Product Disconnected")
                            notifyStatusChange()
                        }

                        override fun onProductConnect(baseProduct: BaseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct))
                            showToast("Product Connected")
                            notifyStatusChange()
                        }

                        override fun onComponentChange(
                            componentKey: ComponentKey, oldComponent: BaseComponent,
                            newComponent: BaseComponent
                        ) {
                            if (newComponent != null) {
                                newComponent.setComponentListener { isConnected ->
                                    Log.d(TAG, "onComponentConnectivityChanged: $isConnected")
                                    notifyStatusChange()
                                }
                            }
                            Log.d(
                                TAG, String.format(
                                    "onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                    componentKey,
                                    oldComponent,
                                    newComponent
                                )
                            )
                        }

                        override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}
                        override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
                    })
            })
        }
    }

    private fun notifyStatusChange() {
        mHandler?.removeCallbacks(updateRunnable);
        mHandler?.postDelayed(updateRunnable, 500);
    }

    private val updateRunnable = Runnable {
        val intent = Intent(FLAG_CONNECTION_CHANGE)
        sendBroadcast(intent)
    }
    private fun showToast(toastMsg: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post { Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show() }
    }
}