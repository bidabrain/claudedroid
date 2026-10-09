package com.clawdroid.app.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Camera capture and image downscaling for chat attachments. */
internal object ImageAttachments {
    // Claude rejects images over 5 MB and downsamples anything past ~1568px on the long edge.
    private const val MAX_EDGE = 1568
    private const val JPEG_QUALITY = 85

    private fun dir(context: Context) = File(context.cacheDir, "attachments").also { it.mkdirs() }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Empty file + content Uri for the system camera to write into. */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val file = File(dir(context), "photo_${System.currentTimeMillis()}.jpg")
        return file to uriFor(context, file)
    }

    /**
     * Re-encodes [source] as an upright JPEG no larger than [MAX_EDGE] on its long side.
     * Returns the new file's content Uri, or null if it can't be decoded as an image.
     */
    fun downscale(context: Context, source: Uri): Pair<Uri, String>? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val decoded = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotation = resolver.openInputStream(source)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = minOf(1f, MAX_EDGE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val output = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val file = File(dir(context), "image_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (output !== decoded) output.recycle()
        decoded.recycle()
        return uriFor(context, file) to file.name
    }
}
