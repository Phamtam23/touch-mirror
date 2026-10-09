package com.example.touchmirror

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        TouchMirrorService.instance?.hideOverlay()
    }

    companion object {
        const val ACTION_STOP = "com.example.touchmirror.STOP"
    }
}
