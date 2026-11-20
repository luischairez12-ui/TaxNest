package com.taxnest.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.taxnest.app.data.TaxNestExport
import com.taxnest.app.data.TaxNestStore
import com.taxnest.app.receipts.ReceiptDraft
import com.taxnest.app.receipts.ReceiptParser
import com.taxnest.app.receipts.ReceiptVault
import com.taxnest.app.ui.TaxNestApp
import com.taxnest.app.ui.TaxNestTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var store: TaxNestStore
    private var cameraTempUri: Uri? = null
    private var receiptDraft by mutableStateOf<ReceiptDraft?>(null)
    private var scanBusy by mutableStateOf(false)
    private var appError by mutableStateOf<String?>(null)

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::importReceipt)
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraTempUri?.let(::importReceipt)
        cameraTempUri = null
    }
    private val createCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { destination ->
            try {
                contentResolver.openOutputStream(destination)?.bufferedWriter()?.use { writer ->
                    writer.write(TaxNestExport.csv(store))
                } ?: error("Unable to open export file")
            } catch (e: Exception) { appError = "Could not export CSV: ${e.message}" }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TaxNestStore(applicationContext)
        setContent {
            TaxNestTheme {
                TaxNestApp(
                    store = store,
                    scanning = scanBusy,
                    scanned = receiptDraft,
                    error = appError,
                    onErrorDismiss = { appError = null },
                    onScannedDismiss = { receiptDraft = null },
                    onScannedSave = { date, merchant, cents, category, uri, rawText ->
                        store.addEntry(date,merchant,cents,category,"expense",uri,rawText,"receipt")
                        receiptDraft = null
                    },
                    onPickImage = { pickImage.launch("image/*") },
                    onTakePicture = { launchCamera() },
                    onExportCsv = { createCsv.launch("TaxNest_TaxRecords_${java.time.LocalDate.now()}.csv") }
                )
            }
        }
        if(savedInstanceState==null) handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(source:Intent?) {
        if (source?.action != Intent.ACTION_SEND || source.type?.startsWith("image/") != true) return
        @Suppress("DEPRECATION")
        val image = if (Build.VERSION.SDK_INT >= 33)
            source.getParcelableExtra(Intent.EXTRA_STREAM,Uri::class.java)
        else source.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        image?.let(::importReceipt)
    }

    private fun launchCamera() {
        try {
            val dir=File(cacheDir,"camera").also{it.mkdirs()}
            val temp=File.createTempFile("TaxNest_",".jpg",dir)
            cameraTempUri=FileProvider.getUriForFile(this,"${packageName}.fileprovider",temp)
            takePicture.launch(requireNotNull(cameraTempUri))
        } catch (e: Exception) { appError="Could not open camera: ${e.message}" }
    }

    private fun importReceipt(uri:Uri) {
        if(scanBusy) return
        scanBusy=true
        lifecycleScope.launch {
            try {
                val saved=withContext(Dispatchers.IO){ ReceiptVault.importImage(this@MainActivity,uri) }
                ReceiptVault.readText(
                    this@MainActivity,saved,
                    onComplete={ text -> receiptDraft=ReceiptParser.fromOcr(text,saved.toString());scanBusy=false },
                    onError={ reason ->
                        // OCR failure must never discard the preserved original image.
                        receiptDraft=ReceiptDraft(photoUri=saved.toString(),merchant="",rawText="")
                        scanBusy=false
                        appError="Image saved. Automatic text extraction couldn't finish: $reason. Enter the details manually."
                    }
                )
            } catch (e: Exception) {
                appError="Receipt could not be saved: ${e.message}"
                scanBusy=false
            }
        }
    }
}
