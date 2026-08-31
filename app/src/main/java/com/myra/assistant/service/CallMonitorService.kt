package com.myra.assistant.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.TelephonyManager

class CallMonitorService : Service() {

    private lateinit var callReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        callReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
                        val mainIntent = Intent(context, Class.forName("com.myra.assistant.MainActivity"))
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        mainIntent.putExtra("INCOMING_CALL", true)
                        mainIntent.putExtra("CALLER_NUMBER", number)
                        context?.startActivity(mainIntent)
                    } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                        val mainIntent = Intent(context, Class.forName("com.myra.assistant.MainActivity"))
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        mainIntent.putExtra("CALL_ENDED", true)
                        context?.startActivity(mainIntent)
                    }
                }
            }
        }
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(callReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(callReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
