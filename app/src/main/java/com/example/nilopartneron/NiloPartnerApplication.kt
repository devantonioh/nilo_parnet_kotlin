package com.example.nilopartneron

import android.app.Application
import com.example.nilopartneron.fcm.VolleyHelper

class NiloPartnerApplication : Application() {
    companion object{
        lateinit var volleyHelper: VolleyHelper
    }

    override fun onCreate() {
        super.onCreate()
        volleyHelper = VolleyHelper.getInstance(this)
    }
}