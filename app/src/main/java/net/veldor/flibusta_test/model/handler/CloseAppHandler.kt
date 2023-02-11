package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.content.Intent
import android.os.Process


object CloseAppHandler {
    fun closeApp(context: Context){
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        context.startActivity(intent)
        val pid = Process.myPid()
        Process.killProcess(pid)
    }
}