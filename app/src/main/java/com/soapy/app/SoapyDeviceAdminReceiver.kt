package com.soapy.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Makes Soapy harder to uninstall: while this is active, Android makes the user
 * deactivate it first, and shows the question below in that dialog.
 */
class SoapyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.uninstall_question)
}
