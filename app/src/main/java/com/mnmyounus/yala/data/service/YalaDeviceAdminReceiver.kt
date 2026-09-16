package com.mnmyounus.yala.data.service

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Device Admin keeps intruders from uninstalling or force-stopping YALA.
 * Disabling it is gated behind the active lock screen.
 */
class YalaDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Disabling device admin removes YALA's uninstall protection. Your locked apps stay locked, but an intruder could remove the app."

    companion object {
        fun component(context: Context) =
            ComponentName(context, YalaDeviceAdminReceiver::class.java)
    }
}
