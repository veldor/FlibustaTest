package net.veldor.flibusta_test.model.utils

import android.content.Intent
import net.veldor.flibusta_test.App

object TransportUtils {
    @kotlin.jvm.JvmStatic
    fun intentCanBeHandled(intent: Intent): Boolean {
        val packageManager = App.instance.packageManager
        return intent.resolveActivity(packageManager) != null
    }
}