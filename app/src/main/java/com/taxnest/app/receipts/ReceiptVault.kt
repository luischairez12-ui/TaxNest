package com.taxnest.app.receipts

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate
import java.util.Locale

/** Original image bytes are preserved in the visible Pictures/TaxNest/Receipts/yyyy/MM folder. */
object ReceiptVault {
    fun importImage(context: Context, from: Uri): Uri {
        val mime = context.contentResolver.getType(from) ?: "image/jpeg"
        val ext = when (mime.lowercase(Locale.US)) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val now = LocalDate.now()
        val name = "Receipt_${now}_${System.currentTimeMillis()}.$ext"
        val record = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,name)
            put(MediaStore.Images.Media.MIME_TYPE,mime)
            put(MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/TaxNest/Receipts/${now.year}/${"%02d".format(now.monthValue)}")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val out = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,record)
            ?: error("Unable to create receipt image")
        try {
            context.contentResolver.openInputStream(from).use { input ->
                requireNotNull(input) { "Receipt image could not be opened" }
                context.contentResolver.openOutputStream(out).use { target ->
                    requireNotNull(target) { "Receipt folder could not be written" }
                    input.copyTo(target)
                }
            }
            context.contentResolver.update(out, ContentValues().apply{put(MediaStore.Images.Media.IS_PENDING,0)},null,null)
            return out
        } catch (e: Exception) { context.contentResolver.delete(out,null,null); throw e }
    }

    fun readText(context: Context, saved: Uri, onComplete: (String)->Unit, onError:(String)->Unit) {
        try {
            val img=InputImage.fromFilePath(context,saved)
            val scanner=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            scanner.process(img)
                .addOnSuccessListener { onComplete(it.text) }
                .addOnFailureListener { onError(it.message ?: "Text scanning failed") }
                .addOnCompleteListener { scanner.close() }
        } catch (e: Exception) { onError(e.message ?: "Unable to scan image") }
    }
}
