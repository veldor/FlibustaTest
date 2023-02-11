/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/
package net.veldor.tor_client.model.control

import android.content.Context
import android.os.Process
import android.util.Log
import net.veldor.tor_client.model.managers.StorageManager
import net.veldor.tor_client.model.managers.ZipFileManager
import net.veldor.tor_client.model.tor_utils.FileUtilities
import net.veldor.tor_client.model.tor_utils.OsData
import net.veldor.tor_client.model.tor_utils.WriteObserver
import java.io.File

class AndroidOnionProxyContext(private val context: Context) {
    val workingDirectory = File(context.applicationInfo.nativeLibraryDir)
    val filesDirectory = File("${context.applicationInfo.dataDir}/app_data/tor/")
    val torExecutableFile = File("${context.applicationInfo.nativeLibraryDir}/libtor.so")
    val torConfigFile = File("${context.applicationInfo.dataDir}/app_data/tor/tor.conf")
    val cookieFile = File("${context.applicationInfo.dataDir}/tor_data/control_auth_cookie")
    val processId: String
        get(){ return Process.myPid().toString() }


    fun clearInstallFiles() {
        //install files only if it not exists
        if(torConfigFile.exists()){
            Log.d("surprise", "AndroidOnionProxyContext: 36 file exitsts, skip installation")
            return
        }
        Log.d("surprise", "AndroidOnionProxyContext: 41 install clear files")
        // install clear TOR files
        val zipFileManager = ZipFileManager()
        zipFileManager.extractZipFromInputStream(
            context.assets.open("tor.mp3"),
            context.applicationInfo.dataDir
        )
        // create log files dir
        StorageManager().createLogsDir(context.applicationInfo.dataDir)
        StorageManager().editConfigurationFile(context)
    }

    fun generateWriteObserver(file: File?): WriteObserver {
        return AndroidWriteObserver(file!!)
    }

    /**
     * Sets environment variables and working directory needed for Tor
     *
     * @param processBuilder we will call start on this to run Tor
     */
    fun setEnvironmentArgsAndWorkingDirectoryForStart(processBuilder: ProcessBuilder) {
        processBuilder.directory(workingDirectory)
        val environment = processBuilder.environment()
        environment["HOME"] = workingDirectory.absolutePath
        when (OsData.osType) {
            OsData.OsType.LINUX_32, OsData.OsType.LINUX_64 ->                 // We have to provide the LD_LIBRARY_PATH because when looking for dynamic libraries
                // Linux apparently will not look in the current directory by default. By setting this
                // environment variable we fix that.
                environment["LD_LIBRARY_PATH"] = workingDirectory.absolutePath
            else -> {}
        }
    }


    @Throws(InterruptedException::class)
    fun deleteAllFilesButHiddenServices() {
        // It can take a little bit for the Tor OP to detect the connection is dead and kill itself
        Thread.sleep(1000, 0)
        filesDirectory.listFiles()?.forEach {file ->
            if (file.isDirectory) {
                if (file.name.compareTo(HIDDENSERVICE_DIRECTORY_NAME) != 0) {
                    FileUtilities.recursiveFileDelete(file)
                }
            } else {
                if (!file.delete()) {
                    throw RuntimeException("Could not delete file " + file.absolutePath)
                }
            }
        }
    }

    companion object{
        private const val HIDDENSERVICE_DIRECTORY_NAME = "hiddenservice"
    }
}