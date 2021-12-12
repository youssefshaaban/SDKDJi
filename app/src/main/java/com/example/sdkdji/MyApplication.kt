package com.example.sdkdji

import android.app.Application
import android.content.Context
import com.secneo.sdk.Helper

class MyApplication:Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Helper.install(this)
    }
}