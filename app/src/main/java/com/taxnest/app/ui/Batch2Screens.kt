package com.taxnest.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxnest.app.data.*
import com.taxnest.app.gmail.GmailReceiptCandidate

@Composable
fun SmartInboxScreen(
    store:TaxNestStore,
    onImportText:(String)->Unit,
    gmailBusy:Boolean,
    gmailConnected:Boolean,
    gmailStatus:String,
    gmailCandidates:List<GmailReceiptCandidate>,
    onConnectGmail:()->Unit,
    onSyncGmail:()->Unit,
    onDisconnectGmail:()->Unit,
    onReviewGmail:(GmailReceiptCandidate)->Unit
) {
    var draft by remember { mutableStateOf("") }
    val detected=SmartInsights.recurringCandidates(store.entries,store.subscriptions)
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Smart Inbox","Import emailed purchases — with your permission.") }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Rounded.MarkEmailRead,null,tint=Blue,modifier=Modifier.size(29.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Gmail receipt connection",fontWeight=FontWeight.Bold,color=Ink,fontSize=15.sp)
                    Text(if(gmailConnected)"Authorized for this session" else "Optional · Google permission required",fontSize=11.sp,color=Muted)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Read-only Gmail access, only when you tap Sync. Possible purchase emails stay in this review list until you choose one. No mail is deleted or sent.",fontSize=12.sp,color=Muted)
            Spacer(Modifier.height(10.dp))
            Text(gmailStatus,fontSize=12.sp,color=if(gmailConnected)Blue else Muted)
            Spacer(Modifier.height(10.dp))
            Button(onClick=if(gmailConnected)onSyncGmail else onConnectGmail,
                enabled=!gmailBusy,modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Blue)) {
                if(gmailBusy) CircularProgressIndicator(Modifier.size(16.dp),color=Color.White,strokeWidth=2.dp)
                else Icon(Icons.Rounded.MarkEmailRead,null,modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if(gmailBusy)"Checking Gmail…" else if(gmailConnected)"Sync recent receipts" else "Connect Gmail (read-only)")
            }
            if(gmailConnected) TextButton(onClick=onDisconnectGmail,enabled=!gmailBusy) { Text("Disconnect and revoke access") }
            Text("Google Cloud's Gmail API and an Android OAuth client matching this APK's signing certificate must be configured first. Google may block unverified apps; this feature is not linked by default.",fontSize=11.sp,color=Muted)
        } }
        if(gmailCandidates.isNotEmpty()) {
            item { SectionHeading("Gmail purchases to review (${gmailCandidates.size})") }
            items(gmailCandidates) { candidate ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(candidate.merchant,color=Ink,fontWeight=FontWeight.Bold,fontSize=14.sp)
                    Text(candidate.date+" · "+candidate.subject,color=Muted,fontSize=11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(candidate.preview,color=Muted,fontSize=12.sp,maxLines=2)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick={onReviewGmail(candidate)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(13.dp)) {
                        Icon(Icons.Rounded.RateReview,null,modifier=Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp));Text("Review and save purchase")
                    }
                }
            }
        }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Text("Paste an emailed receipt",fontWeight=FontWeight.Bold,color=Ink)
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(value=draft,onValueChange={draft=it.take(16000)},modifier=Modifier.fillMaxWidth().heightIn(min=160.dp),
                minLines=5,maxLines=12,placeholder={Text("From: retailer\nOrder total: $42.50\nDate: 2026-11-20...")},
                shape=RoundedCornerShape(16.dp))
            Spacer(Modifier.height(8.dp))
            Button(onClick={onImportText(draft);draft=""},enabled=draft.isNotBlank(),modifier=Modifier.fillMaxWidth(),
                colors=ButtonDefaults.buttonColors(containerColor=Blue),shape=RoundedCornerShape(15.dp)) {
                Icon(Icons.Rounded.ReceiptLong,null);Spacer(Modifier.width(6.dp));Text("Review imported purchase")
            }
        } }
        item { SectionHeading("Recurring-charge discoveries") }
        if(detected.isEmpty()) item {
            EmptyNotice("Not enough history yet","Record at least two similar charges on a regular schedule, and TaxNest will suggest a subscription.",Icons.Rounded.Autorenew)
        } else items(detected) { candidate ->
            GlassCard(Modifier.fillMaxWidth()) {
                Text(candidate.merchant,fontWeight=FontWeight.Bold,color=Ink)
                Text("${dollars(candidate.amountCents)} ${candidate.cadence.lowercase()} · ${candidate.occurrences} matching charges",fontSize=12.sp,color=Muted)
                Text("Possible next due: ${candidate.nextDue}",fontSize=12.sp,color=Muted)
                OutlinedButton(onClick={store.addSubscription(candidate.merchant,candidate.amountCents,candidate.cadence,candidate.nextDue)},
                    modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(13.dp)) { Text("Add to subscriptions") }
            }
        }
        item { Text("Pasted receipts and approved Gmail purchases follow the same review process. Gmail access tokens are not stored by TaxNest; live mail scanning needs your authorization and internet access.",fontSize=11.sp,color=Muted) }
    }
}

@Composable
fun AnnualReportScreen(store:TaxNestStore,onExportPdf:()->Unit,onExportCsv:()->Unit) {
    val year=store.taxYear.toString()
    val entries=store.entries.filter{it.date.startsWith(year)}
    val income=entries.filter{it.kind=="income"}.sumOf{it.amountCents}
    val spending=entries.filter{it.kind=="expense"}.sumOf{it.amountCents}
    val categories=SmartInsights.taxYearSummary(store.entries,store.taxYear).entries.sortedByDescending{it.value}
    val yearMileage=store.mileage.filter{it.date.startsWith(year)}
    val premium=store.estimatedOtPremium()
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Year-End Report","Review your ${store.taxYear} records before tax time.") }
        item { GlassCard(Modifier.fillMaxWidth()) {
            MiniLabel("Your recorded activity · not linked to bank")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) { Text("Income",color=Muted,fontSize=12.sp);Text(dollars(income),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink) }
                Column(Modifier.weight(1f)) { Text("Spending",color=Muted,fontSize=12.sp);Text(dollars(spending),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink) }
            }
            Spacer(Modifier.height(8.dp))
            Text("${entries.size} transactions · ${entries.count{!it.receiptUri.isNullOrBlank()}} receipt photos",fontSize=12.sp,color=Muted)
        } }
        item { SectionHeading("Spending by category") }
        if(categories.isEmpty()) item { EmptyNotice("No spending this tax year","Add expenses or receipts to see totals here.",Icons.Rounded.PieChart) }
        else items(categories) { (name,total) ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(name,color=Ink,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                    Text(dollars(total),color=Ink,fontWeight=FontWeight.Bold)
                }
            }
        }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Text("Other records to review",fontWeight=FontWeight.Bold,color=Ink)
            Spacer(Modifier.height(8.dp))
            Text("Estimated FLSA overtime premium: ${dollars(premium)}",fontSize=13.sp,color=Ink)
            Text("Logged business-purpose miles: ${"%.1f".format(java.util.Locale.US,yearMileage.sumOf{it.miles})}",fontSize=13.sp,color=Ink)
            Text("Active subscription records: ${store.subscriptions.size}",fontSize=13.sp,color=Ink)
            Spacer(Modifier.height(8.dp))
            Text("These are documented amounts, not deductions or tax refunds. Eligibility varies with your work type, tax year, income, and reimbursements.",fontSize=12.sp,color=Muted)
        } }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Text("Export for your tax preparer",fontWeight=FontWeight.Bold,color=Ink)
            Text("The PDF summarizes this tax year. CSV contains all your records. Receipt pictures are separate in Pictures/TaxNest/Receipts.",fontSize=12.sp,color=Muted)
            Spacer(Modifier.height(8.dp))
            Button(onClick=onExportPdf,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Blue)) {
                Icon(Icons.Rounded.PictureAsPdf,null);Spacer(Modifier.width(8.dp));Text("Export annual PDF")
            }
            OutlinedButton(onClick=onExportCsv,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp)) {
                Icon(Icons.Rounded.FileDownload,null);Spacer(Modifier.width(8.dp));Text("Export full CSV")
            }
        } }
    }
}
