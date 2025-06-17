package com.ai.kit.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "receive: ${intent?.action}")

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "boot complete And start MainActivity")

                try {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("boot_start", true)
                    }

                    context?.startActivity(launchIntent)
                    Log.d(TAG, "start MainActivity success")
                } catch (e: Exception) {
                    Log.e(TAG, "start MainActivity fail: ${e.message}", e)
                }
            }
        }
    }
} 