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

@Composable
fun SmartInboxScreen(store:TaxNestStore,onImportText:(String)->Unit) {
    var draft by remember { mutableStateOf("") }
    val detected=SmartInsights.recurringCandidates(store.entries,store.subscriptions)
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Smart Inbox","Import emailed purchases — with your permission.") }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Rounded.MarkEmailUnread,null,tint=Blue)
                Spacer(Modifier.width(10.dp))
                Text("Gmail not connected",fontWeight=FontWeight.Bold,color=Ink)
            }
            Spacer(Modifier.height(8.dp))
            Text("You control what enters TaxNest. Copy the receipt text from an email, paste it below, then check the merchant, total and date before saving.",fontSize=12.sp,color=Muted)
            Spacer(Modifier.height(8.dp))
            Text("You can also use Android's Share menu to share plain receipt text to TaxNest when your email app supports it.",fontSize=12.sp,color=Muted)
        } }
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
        item { Text("TaxNest never reads your Gmail mailbox in this release. No password or OAuth token is collected.",fontSize=11.sp,color=Muted) }
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
