package net.veldor.tor_client.model.utils

import android.annotation.SuppressLint
import java.io.File

internal object ChmodCommand {
    @SuppressLint("SetWorldReadable")
    fun dirChmod(path: String, executableDir: Boolean) {
        val dir = File(path)
        check(dir.isDirectory) { "dirChmod dir not exist or not dir $path" }
        check(
            !(!dir.setReadable(true, false)
                    || !dir.setWritable(true)
                    || !dir.setExecutable(true, false))
        ) { "DirChmod chmod dir fault $path" }
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                dirChmod(file.absolutePath, executableDir)
            } else if (file.isFile) {
                if (executableDir) {
                    executableFileChmod(file.absolutePath)
                } else {
                    regularFileChmod(file.absolutePath)
                }
            }
        }
    }

    @SuppressLint("SetWorldReadable")
    private fun executableFileChmod(path: String) {
        val executable = File(path)
        check(executable.isFile) { "executableFileChmod file not exist or not file $path" }
        check(
            !(!executable.setReadable(true, false)
                    || !executable.setWritable(true)
                    || !executable.setExecutable(true, false))
        ) { "executableFileChmod chmod file fault $path" }
    }

    @SuppressLint("SetWorldReadable")
    private fun regularFileChmod(path: String) {
        val file = File(path)
        check(file.isFile) { "regularFileChmod file not exist or not file $path" }
        check(
            !(!file.setReadable(true, false)
                    || !file.setWritable(true))
        ) { "regularFileChmod chmod file fault $path" }
    }
}