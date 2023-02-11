package net.veldor.flibusta_test.model.converter

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.adobe.dp.fb2.convert.Converter
import net.veldor.flibusta_test.App
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class Fb2ToEpubConverter {
    fun getEpubFile(documentFile: DocumentFile): File {
        val tempFb2File = File.createTempFile("forConvertation", ".fb2")
        tempFb2File.deleteOnExit()
        val inputStream = App.instance.contentResolver.openInputStream(documentFile.uri)
        val outputStream: OutputStream = FileOutputStream(tempFb2File)

        val buffer = ByteArray(1024)
        var length: Int
        if (inputStream != null) {
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            inputStream.close()
        }
        outputStream.close()
        Log.d("surprise", "Fb2ToEpubConverter: 28 fb2 file length is ${tempFb2File.length()}")
        val tempEpubFile = File(App.instance.getExternalFilesDir("media"), documentFile.name!!.replace("fb2", "epub"))
        tempEpubFile.deleteOnExit()
        Converter.main(arrayOf(tempFb2File.absolutePath, tempEpubFile.absolutePath))
        Log.d("surprise", "Fb2ToEpubConverter: 32 epub length is ${tempEpubFile.length()}")
        return tempEpubFile
    }

    fun getEpubFile(file: File): File {
        val tempFb2File = File.createTempFile("forConvertation", ".fb2")
        tempFb2File.deleteOnExit()
        val inputStream = FileInputStream(file)
        val outputStream: OutputStream = FileOutputStream(tempFb2File)
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }

        inputStream.close()
        outputStream.close()
        val tempEpubFile = File(App.instance.getExternalFilesDir("media"), file.name.replace("fb2", "epub"))
        tempEpubFile.deleteOnExit()
        Converter.main(arrayOf(tempEpubFile.absolutePath, tempEpubFile.absolutePath))
        return tempEpubFile
    }
}