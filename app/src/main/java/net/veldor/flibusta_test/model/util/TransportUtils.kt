package net.veldor.flibusta_test.model.util

import android.content.Intent
import android.util.Log
import net.veldor.flibusta_test.App

object TransportUtils {
    @kotlin.jvm.JvmStatic
    fun intentCanBeHandled(intent: Intent): Boolean {
        val packageManager = App.instance.packageManager
        Log.d("surprise", "intentCanBeHandled 10:  ${intent.resolveActivity(packageManager)}")
        return intent.resolveActivity(packageManager) != null
    }
}