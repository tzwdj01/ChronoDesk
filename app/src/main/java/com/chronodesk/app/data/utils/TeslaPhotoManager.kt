package com.chronodesk.app.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.InputStream

object TeslaPhotoManager {

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, "tesla_custom_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getPhotoFile(context: Context, dsUid: String, carId: Int): File {
        val cleanDsUid = dsUid.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(getStorageDir(context), "car_${cleanDsUid}_${carId}.jpg")
    }

    fun hasCustomPhoto(context: Context, dsUid: String, carId: Int): Boolean {
        if (dsUid.isBlank() || carId <= 0) return false
        val file = getPhotoFile(context, dsUid, carId)
        return file.exists() && file.length() > 0
    }

    fun savePhoto(context: Context, dsUid: String, carId: Int, inputStream: InputStream): Boolean {
        return try {
            val file = getPhotoFile(context, dsUid, carId)
            file.outputStream().use { out ->
                inputStream.copyTo(out)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deletePhoto(context: Context, dsUid: String, carId: Int): Boolean {
        return try {
            val file = getPhotoFile(context, dsUid, carId)
            if (file.exists()) file.delete() else true
        } catch (_: Exception) {
            false
        }
    }

    fun loadScaledBitmap(context: Context, dsUid: String, carId: Int, maxDimension: Int = 1024): ImageBitmap? {
        if (dsUid.isBlank() || carId <= 0) return null
        val file = getPhotoFile(context, dsUid, carId)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            bmp?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
