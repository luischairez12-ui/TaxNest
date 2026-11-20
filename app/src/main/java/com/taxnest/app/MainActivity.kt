package com.taxnest.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.core.view.WindowCompat
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.taxnest.app.gmail.GmailReceiptClient
import com.taxnest.app.gmail.GmailReceiptCandidate
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.taxnest.app.data.TaxNestExport
import com.taxnest.app.data.TaxNestReport
import com.taxnest.app.data.TaxNestImport
import com.taxnest.app.receipts.EmailReceiptParser
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
    private var gmailBusy by mutableStateOf(false)
    private var gmailConnected by mutableStateOf(false)
    private var gmailStatus by mutableStateOf("Not connected. Google Cloud OAuth must be configured for this build.")
    private var gmailCandidates by mutableStateOf<List<GmailReceiptCandidate>>(emptyList())

    private val gmailConsent = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            gmailBusy=false
            gmailStatus="Gmail access wasn't granted. Your inbox was not read."
            return@registerForActivityResult
        }
        try {
            val authorization=Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data)
            fetchGmail(authorization.accessToken)
        } catch (e:Exception) {
            gmailBusy=false
            gmailStatus="Gmail consent did not complete: ${e.localizedMessage ?: "Check the Google OAuth configuration"}"
        }
    }

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

    private val openCsv = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { source ->
            try {
                val result=contentResolver.openInputStream(source)?.bufferedReader()?.use{ TaxNestImport.restore(store,it) }
                    ?: error("Couldn't read CSV file")
                appError="Imported ${result.added} records; skipped ${result.skipped} duplicates or unsupported rows. Receipt photos are separate files."
            } catch(e:Exception) { appError="CSV import failed: ${e.message}" }
        }
    }
    private val createPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { destination ->
            try {
                contentResolver.openOutputStream(destination)?.use { TaxNestReport.writePdf(store,it) }
                    ?: error("Unable to open PDF destination")
            } catch (e: Exception) { appError = "Could not export PDF: ${e.message}" }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TaxNestStore(applicationContext)
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=true
        window.statusBarColor=android.graphics.Color.rgb(250,252,255)
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
                        store.addEntry(date,merchant,cents,category,"expense",uri.ifBlank{null},rawText,if(uri.isBlank())"shared-text" else "receipt")
                        receiptDraft = null
                    },
                    onPickImage = { pickImage.launch("image/*") },
                    onImportText = { text -> receiptDraft=EmailReceiptParser.parse(text) },
                    onExportPdf = { createPdf.launch("TaxNest_${store.taxYear}_AnnualSummary.pdf") },
                    onImportCsv = { openCsv.launch(arrayOf("text/*","text/comma-separated-values","application/csv","application/octet-stream")) },
                    onTakePicture = { launchCamera() },
                    onExportCsv = { createCsv.launch("TaxNest_TaxRecords_${java.time.LocalDate.now()}.csv") },
                    gmailBusy = gmailBusy,
                    gmailConnected = gmailConnected,
                    gmailStatus = gmailStatus,
                    gmailCandidates = gmailCandidates,
                    onConnectGmail = { requestGmailAccess() },
                    onSyncGmail = { requestGmailAccess() },
                    onDisconnectGmail = { disconnectGmail() },
                    onReviewGmail = { candidate -> receiptDraft=EmailReceiptParser.parse(candidate.reviewText) }

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
        if (source?.action != Intent.ACTION_SEND) return
        if (source.type?.startsWith("text/") == true) {
            source.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let {
                receiptDraft = EmailReceiptParser.parse(it)
            }
            return
        }
        if (source.type?.startsWith("image/") != true) return
        @Suppress("DEPRECATION")
        val image = if (Build.VERSION.SDK_INT >= 33)
            source.getParcelableExtra(Intent.EXTRA_STREAM,Uri::class.java)
        else source.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        image?.let(::importReceipt)
    }

    /** Request Gmail read-only scope only after the user taps Connect / Sync.
     *  Does not persist tokens or automatically read any email in the background. */
    private fun requestGmailAccess() {
        if(gmailBusy)return
        gmailBusy=true
        gmailStatus="Requesting Google's read-only Gmail permission…"
        try {
            val request=AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(GmailReceiptClient.READ_ONLY_SCOPE)))
                .build()
            Identity.getAuthorizationClient(this).authorize(request)
                .addOnSuccessListener { result ->
                    if(result.hasResolution()) {
                        val pending=result.pendingIntent
                        if(pending==null) {
                            gmailBusy=false;gmailStatus="Google didn't provide an authorization screen."
                        } else gmailConsent.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    } else fetchGmail(result.accessToken)
                }
                .addOnFailureListener { e ->
                    gmailBusy=false
                    gmailConnected=false
                    gmailStatus="Gmail permission unavailable: ${e.localizedMessage ?: "Check Google Cloud setup and signing SHA-1"}"
                }
        } catch(e:Exception) {
            gmailBusy=false;gmailStatus="Gmail setup error: ${e.localizedMessage}"
        }
    }

    private fun fetchGmail(token:String?) {
        if(token.isNullOrBlank()) {gmailBusy=false;gmailStatus="Google did not return an access token.";return}
        gmailConnected=true
        gmailStatus="Looking for recent receipt emails…"
        lifecycleScope.launch {
            try {
                val found=GmailReceiptClient.findReceipts(token)
                gmailCandidates=found
                gmailStatus="Found ${found.size} possible receipts. Nothing has been added to TaxNest automatically."
            } catch(e:Exception) {
                gmailStatus="Gmail scan failed: ${e.localizedMessage ?: "Check Gmail API and OAuth setup"}"
            } finally {gmailBusy=false}
        }
    }

    private fun disconnectGmail() {
        gmailCandidates=emptyList()
        gmailConnected=false
        gmailBusy=false
        gmailStatus="Disconnecting Gmail…"
        runCatching {
            val request=RevokeAccessRequest.builder()
                .setScopes(listOf(Scope(GmailReceiptClient.READ_ONLY_SCOPE)))
                .build()
            Identity.getAuthorizationClient(this).revokeAccess(request)
                .addOnSuccessListener { gmailStatus="Gmail access revoked. No email remains in the review list." }
                .addOnFailureListener { e -> gmailStatus="Review list cleared. Revoke Gmail permission in your Google Account if needed: ${e.localizedMessage}" }
        }.onFailure { e -> gmailStatus="Review list cleared. Revoke TaxNest access in your Google Account: ${e.localizedMessage}" }
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
