package net.veldor.flibusta_test.model.utils

import android.content.Context
import android.os.Environment
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import java.io.File
import java.math.BigDecimal

object CacheUtils {
    /**
     * Получить общий размер кеша
     *
     * @param context
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getTotalCacheSize(context: Context): String {
        var cacheSize = getFolderSize(context.cacheDir)
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            cacheSize += getFolderSize(context.externalCacheDir)
        }
        return getFormatSize(cacheSize.toDouble())
    }


    fun getCacheSize(context: Context): Long {
        var cacheSize = getFolderSize(context.cacheDir)
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            cacheSize += getFolderSize(context.externalCacheDir)
        }
        return cacheSize
    }

    /**
     * Очистить весь кеш
     *
     * @param context
     */
    fun clearAllCache(context: Context) {
        deleteDir(context.cacheDir)
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            deleteDir(context.externalCacheDir)
        }
    }

    /**
     * Удалить файлы
     *
     * @param dir
     * @return
     */
    private fun deleteDir(dir: File?): Boolean {
        if (null == dir) {
            return false
        }
        if (dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }

    // Получить файл
    //Context.getExternalFilesDir () -> SDCard / Android / data / имя пакета / files / каталог вашего приложения, обычно помещают некоторые долгосрочные сохраненные данные
    //Context.getExternalCacheDir () -> SDCard / Android / data / имя пакета вашего приложения / cache / каталог, обычно хранит данные временного кэша
    @Throws(Exception::class)
    fun getFolderSize(file: File?): Long {
        var size: Long = 0
        try {
            val fileList = file!!.listFiles()
            for (i in fileList.indices) {
                // Если есть папки ниже
                size += if (fileList[i].isDirectory) {
                    getFolderSize(fileList[i])
                } else {
                    fileList[i].length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return size
    }

    /**
     * Формат единицы
     *
     * @param size
     * @return
     */
    fun getFormatSize(size: Double): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) {
            //            return size + "Byte";
            return "0K"
        }

        val megaByte = kiloByte / 1024
        if (megaByte < 1) {
            val result1 = BigDecimal(kiloByte.toString())
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "KB"
        }

        val gigaByte = megaByte / 1024
        if (gigaByte < 1) {
            val result2 = BigDecimal(megaByte.toString())
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "MB"
        }

        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) {
            val result3 = BigDecimal(gigaByte.toString())
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "GB"
        }
        val result4 = BigDecimal(teraBytes)
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB"
    }

    fun requestClearCache() {
        if (DownloadHandler.instance.downloadInProgress.value == false && PreferencesHandler.instance.isClearCache) {
            val size = getCacheSize(App.instance)
            if(size > PreferencesHandler.instance.longMaxCacheSize){
                clearAllCache(App.instance)
            }
        }
    }

}