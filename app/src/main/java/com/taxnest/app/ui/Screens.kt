package com.taxnest.app.ui

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxnest.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.min

@Composable
fun HomeScreen(store: TaxNestStore, go:(Page)->Unit, addMoney:()->Unit, scan:()->Unit) {
    val greeting=when(LocalTime.now().hour){in 0..10->"Good morning";in 11..16->"Good afternoon";else->"Good evening"}
    LazyColumn(contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { MountainHeader(greeting) }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Rounded.CalendarMonth,null,tint=Blue);Spacer(Modifier.width(10.dp))
                Text("${store.taxYear} Tax Year",color=Ink,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text(store.profile,color=Lavender,fontWeight=FontWeight.SemiBold,fontSize=12.sp)
            }
        } }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(Brush.linearGradient(listOf(Color(0xFF4499B3),Color(0xFF566FC8),Color(0xFF9270DA)))).clickable{go(Page.TaxBreaks)}.padding(18.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Rounded.AutoAwesome,null,tint=Color.White,modifier=Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tax Break Finder",color=Color.White.copy(alpha=.95f),fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                        Text("Review possible tax savings",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                        Text("No deduction is assumed without a review",fontSize=11.sp,color=Color.White.copy(alpha=.86f))
                    }
                    Icon(Icons.Rounded.ChevronRight,null,tint=Color.White)
                }
            }
        }
        item {
            val tiles=listOf(
                Triple("Receipts",Icons.Rounded.ReceiptLong,Page.Receipts),
                Triple("Mileage",Icons.Rounded.DirectionsCar,Page.Mileage),
                Triple("Overtime",Icons.Rounded.Schedule,Page.Overtime),
                Triple("Subscriptions",Icons.Rounded.Subscriptions,Page.Subscriptions),
                Triple("Spending",Icons.Rounded.PieChart,Page.Money),
                Triple("Tax Breaks",Icons.Rounded.AutoAwesome,Page.TaxBreaks),
                Triple("Smart Inbox",Icons.Rounded.MarkEmailRead,Page.SmartInbox),
                Triple("Tax Reports",Icons.Rounded.Description,Page.Reports)
            )
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                tiles.chunked(3).forEach{row->
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach { (title,icon,route) ->
                            GlassCard(Modifier.weight(1f).clickable{go(route)}) {
                                Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Icon(icon,null,tint=Blue,modifier=Modifier.size(28.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(title,fontSize=11.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeading("Quick capture") }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                QuickAction(Icons.Rounded.CameraAlt,"Scan receipt","Save original photo",Modifier.weight(1f),scan)
                QuickAction(Icons.Rounded.AddCircleOutline,"Add expense","Log a purchase",Modifier.weight(1f),addMoney)
            }
        }
        item { SectionHeading("Your money this month","View spending") { go(Page.Money) } }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                MiniLabel("Recorded spending")
                Spacer(Modifier.height(5.dp))
                Text(dollars(store.monthlyExpenses()),fontSize=29.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text("Income: ${dollars(store.monthlyIncome())} · Net: ${dollars(store.monthlyIncome()-store.monthlyExpenses())}",fontSize=12.sp,color=Muted)
                Text("Manual entries only — not linked to your bank",fontSize=11.sp,color=Muted)
            }
        }
        item { SectionHeading("Recent activity") }
        if(store.entries.isEmpty()) item { EmptyNotice("Fresh start","Your purchases and income will appear here.",Icons.Rounded.ReceiptLong) }
        else items(store.entries.take(3).size) { index -> EntryRow(store.entries[index],onDelete=null) }
    }
}

@Composable
private fun HeroMetric(title:String,amount:String,icon:ImageVector,modifier:Modifier=Modifier) {
    Row(modifier.background(Color.White.copy(alpha=.18f),RoundedCornerShape(14.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=Color.White,modifier=Modifier.size(18.dp));Spacer(Modifier.width(5.dp))
        Column { Text(title,fontSize=10.sp,color=Color.White.copy(alpha=.85f));Text(amount,fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1) }
    }
}
@Composable
private fun QuickAction(icon:ImageVector,title:String,subtitle:String,modifier:Modifier=Modifier,onClick:()->Unit) {
    GlassCard(modifier.clickable{onClick()}) {
        Icon(icon,null,tint=Blue,modifier=Modifier.size(28.dp))
        Spacer(Modifier.height(12.dp))
        Text(title,color=Ink,fontSize=14.sp,fontWeight=FontWeight.Bold)
        Text(subtitle,color=Muted,fontSize=11.sp,maxLines=2)
    }
}

@Composable
fun ReceiptsScreen(store: TaxNestStore, takePicture:()->Unit, pickImage:()->Unit, addManual:()->Unit,openInbox:()->Unit) {
    val entries=store.entries.filter{it.receiptUri!=null}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Receipt Vault","Every original photo, safely organized.") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Rounded.Folder,tint=Blue,contentDescription=null)
                    Column { Text("Pictures / TaxNest / Receipts",fontWeight=FontWeight.Bold,color=Ink,fontSize=14.sp)
                        Text("Android Files and Photos can access these images",fontSize=11.sp,color=Muted) }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    PillButton("Camera",Icons.Rounded.CameraAlt,Modifier.weight(1f),takePicture)
                    OutlinedButton(onClick=pickImage,modifier=Modifier.weight(1f),shape=RoundedCornerShape(16.dp)) {
                        Icon(Icons.Rounded.PhotoLibrary,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Import")
                    }
                }
            }
        }
        item { Text("Scans extract text on-device, and you review every amount before it's added.",color=Muted,fontSize=12.sp) }
        item { SectionHeading("Saved receipts (${entries.size})") }
        item { GlassCard(Modifier.fillMaxWidth()) {
            ActionRow(Icons.Rounded.MarkEmailRead,"Import emailed receipts","Paste or share order confirmations into TaxNest",openInbox)
        } }
        if(entries.isEmpty()) item { EmptyNotice("No receipts yet","Tap Camera or Import to save your first receipt.",Icons.Rounded.ReceiptLong) }
        else items(entries.size) { i -> ReceiptRow(entries[i],onDelete={store.deleteEntry(entries[i].id)}) }
    }
}

@Composable
private fun ReceiptRow(entry:MoneyEntry,onDelete:()->Unit) {
    val context=LocalContext.current
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ReceiptThumbnail(entry.receiptUri!!)
            Column(Modifier.weight(1f)) {
                Text(entry.merchant,fontWeight=FontWeight.Bold,color=Ink,maxLines=1)
                Text("${entry.date} · ${entry.category}",fontSize=12.sp,color=Muted)
                Text(dollars(entry.amountCents),fontWeight=FontWeight.Bold,color=Blue)
            }
            IconButton(onClick={
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(entry.receiptUri),"image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (_:Exception) { /* original image still lives in Pictures folder */ }
            }) {Icon(Icons.Rounded.OpenInNew,"Open original",tint=Blue)}
            IconButton(onClick=onDelete){Icon(Icons.Rounded.DeleteOutline,"Delete ledger entry",tint=Muted)}
        }
    }
}

@Composable
fun ReceiptThumbnail(uriString:String) {
    val context=LocalContext.current
    var image by remember(uriString){mutableStateOf<ImageBitmap?>(null)}
    LaunchedEffect(uriString) {
        image=withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                    BitmapFactory.decodeStream(stream,null,BitmapFactory.Options().apply { inSampleSize=8 })?.asImageBitmap()
                }
            } catch (_:Exception){null}
        }
    }
    Box(Modifier.size(68.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2F4FB)),contentAlignment=Alignment.Center) {
        if(image!=null) Image(bitmap=image!!,contentDescription="Saved receipt photo",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
        else Icon(Icons.Rounded.ReceiptLong,null,tint=Blue)
    }
}

@Composable
fun MoneyScreen(store:TaxNestStore,addMoney:()->Unit,exportCsv:()->Unit,subscriptions:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Money Overview","Know where every dollar goes.") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                MiniLabel("This month")
                Spacer(Modifier.height(8.dp))
                Text(dollars(store.monthlyExpenses()),fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text("Recorded spending",color=Muted,fontSize=12.sp)
                Spacer(Modifier.height(12.dp))
                Row { Text("Income",Modifier.weight(1f),color=Muted);Text(dollars(store.monthlyIncome()),color=Mint,fontWeight=FontWeight.Bold) }
                Spacer(Modifier.height(4.dp))
                Row { Text("Net (tracked)",Modifier.weight(1f),color=Muted);Text(dollars(store.monthlyIncome()-store.monthlyExpenses()),color=Ink,fontWeight=FontWeight.Bold) }
            }
        }
        item { Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            PillButton("Add transaction",Icons.Rounded.Add,Modifier.weight(1f),addMoney)
            OutlinedButton(onClick=exportCsv,shape=RoundedCornerShape(16.dp)) {Icon(Icons.Rounded.FileDownload,"Export CSV")}
        } }
        item { SectionHeading("Spending by category") }
        val categories=store.entries.filter{it.kind=="expense"&&it.date.startsWith(monthStart())}.groupBy{it.category}.mapValues{it.value.sumOf{e->e.amountCents}}.toList().sortedByDescending{it.second}
        if(categories.isEmpty()) item { EmptyNotice("Nothing recorded yet","Add an expense or scan a receipt.",Icons.Rounded.PieChart) }
        else item {
            GlassCard(Modifier.fillMaxWidth()) { categories.forEach { (name,amount) ->
                val total=categories.sumOf{it.second}.coerceAtLeast(1)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name,color=Ink,fontSize=13.sp);Text(dollars(amount),color=Muted,fontSize=13.sp)}
                Spacer(Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFFEDF0FA),RoundedCornerShape(5.dp))){Box(Modifier.fillMaxWidth(amount.toFloat()/total).fillMaxHeight().background(Blue,RoundedCornerShape(5.dp)))}
                Spacer(Modifier.height(12.dp))
            } }
        }
        item { GlassCard(Modifier.fillMaxWidth()) { ActionRow(Icons.Rounded.Subscriptions,"Recurring subscriptions","Track bills and their next due dates",subscriptions) } }
        item { SectionHeading("All transactions (${store.entries.size})") }
        if(store.entries.isEmpty()) item { EmptyNotice("No transactions yet","Manual entries and saved receipts will show here.",Icons.Rounded.Wallet) }
        else items(store.entries.size) { i -> EntryRow(store.entries[i]){store.deleteEntry(store.entries[i].id)} }
    }
}

@Composable
private fun EntryRow(e:MoneyEntry,onDelete:(()->Unit)?) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFEEF1FF),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
                Icon(if(e.kind=="income")Icons.Rounded.SouthWest else Icons.Rounded.ReceiptLong,null,tint=Blue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.merchant,color=Ink,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("${e.date} • ${e.category} • ${e.source}",color=Muted,fontSize=11.sp,maxLines=1)
            }
            Text((if(e.kind=="income")"+" else "−")+dollars(e.amountCents),color=if(e.kind=="income")Mint else Ink,fontWeight=FontWeight.Bold)
            if(onDelete!=null) IconButton(onClick=onDelete,modifier=Modifier.size(35.dp)) {Icon(Icons.Rounded.Close,"Delete",Modifier.size(16.dp),tint=Muted)}
        }
    }
}

@Composable
fun SubscriptionsScreen(store:TaxNestStore,addSubscription:()->Unit) {
    fun monthly(cents:Long,frequency:String):Long = when(frequency) { "Weekly" -> (cents*52)/12; "Yearly"-> cents/12;else->cents }
    val monthlyTotal=store.subscriptions.sumOf{monthly(it.amountCents,it.frequency)}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {PageHeading("Subscriptions","Everything recurring, all in one place.")}
        item { GlassCard(Modifier.fillMaxWidth()) {
            MiniLabel("Estimated recurring monthly total")
            Spacer(Modifier.height(6.dp))
            Text(dollars(monthlyTotal),fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
            Text("Calculated from subscriptions you entered",color=Muted,fontSize=11.sp)
            Spacer(Modifier.height(12.dp))
            PillButton("Add subscription",Icons.Rounded.Add,Modifier.fillMaxWidth(),addSubscription)
        } }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
                Icon(Icons.Rounded.Email,null,tint=Lavender);Text("Email subscription detection",fontWeight=FontWeight.SemiBold,color=Ink)
            }
            Spacer(Modifier.height(7.dp))
            Text("Recurring suggestions come from your own recorded purchases. No email inbox or bank account is being accessed.",color=Muted,fontSize=12.sp)
        } }
        item { SectionHeading("Tracked subscriptions (${store.subscriptions.size})") }
        val recurring = SmartInsights.recurringCandidates(store.entries,store.subscriptions)
        if(recurring.isNotEmpty()) {
            item { SectionHeading("Possible subscriptions found (${recurring.size})") }
            items(recurring) { guess ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(guess.merchant,color=Ink,fontWeight=FontWeight.Bold)
                            Text("${guess.occurrences} similar charges · ${guess.cadence} · ${dollars(guess.amountCents)}",fontSize=12.sp,color=Muted)
                        }
                        TextButton(onClick={store.addSubscription(guess.merchant,guess.amountCents,guess.cadence,guess.nextDue)}) { Text("Add") }
                    }
                }
            }
        }
        if(store.subscriptions.isEmpty()) item{EmptyNotice("No subscriptions yet","Track your streaming, apps, memberships and more.",Icons.Rounded.Subscriptions)}
        else items(store.subscriptions.size){index->
            val sub=store.subscriptions[index]
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Autorenew,null,tint=Blue);Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)){
                        Text(sub.name,color=Ink,fontWeight=FontWeight.Bold)
                        Text("${sub.frequency} · next ${sub.nextDueDate}",fontSize=11.sp,color=Muted)
                    }
                    Text(dollars(sub.amountCents),color=Ink,fontWeight=FontWeight.Bold)
                    IconButton(onClick={store.deleteSubscription(sub.id)}){Icon(Icons.Rounded.DeleteOutline,"Remove",tint=Muted)}
                }
            }
        }
    }
}

@Composable
fun OvertimeScreen(store:TaxNestStore,addOvertime:()->Unit) {
    val premium=store.estimatedOtPremium()
    val cap=if(store.filingStatus=="Married filing jointly")2500000L else 1250000L
    val capped=min(cap,premium)
    val logged=store.overtime.filter{it.weekOf.startsWith(store.taxYear.toString())}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {PageHeading("Overtime Tracker","Keep a record of your qualifying premium.")}
        item { GlassCard(Modifier.fillMaxWidth()) {
            MiniLabel("Estimated ${store.taxYear} qualifying premium")
            Spacer(Modifier.height(9.dp))
            Text(dollars(premium),fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
            Text("Illustrative amount before caps, income phaseouts & eligibility review",fontSize=11.sp,color=Muted)
            Spacer(Modifier.height(13.dp))
            Text("${store.filingStatus} maximum before phaseouts: ${dollars(cap)}",fontSize=12.sp,color=Ink)
            LinearProgressIndicator(progress={capped/cap.toFloat()},modifier=Modifier.fillMaxWidth().padding(vertical=8.dp),color=Blue,trackColor=SoftLine)
            PillButton("Log an overtime week",Icons.Rounded.Add,Modifier.fillMaxWidth(),addOvertime)
        } }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {Icon(Icons.Rounded.Info,null,tint=Blue);Text("Important IRS distinction",fontWeight=FontWeight.Bold,color=Ink)}
            Spacer(Modifier.height(7.dp))
            Text("For standard FLSA time-and-a-half, generally only the extra half of the regular rate is potentially deductible, not the full overtime paycheck. For 2026, confirm the qualified amount reported by your employer (generally W-2 box 12, code TT). TaxNest is a recordkeeper, not a tax filing determination.",fontSize=12.sp,color=Muted)
        } }
        item { SectionHeading("Logged weeks (${logged.size})") }
        if(logged.isEmpty()) item {EmptyNotice("No overtime weeks yet","Add your weekly rate and overtime hours.",Icons.Rounded.Schedule)}
        else items(logged.size) {index->
            val week=logged[index]
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Week of ${week.weekOf}",fontWeight=FontWeight.Bold,color=Ink)
                        Text("${week.overtimeHours} OT hours · ${dollars(week.hourlyRateCents)}/hr",fontSize=12.sp,color=Muted)
                        Text(if(week.flsaEligible)"Estimated premium: ${dollars(week.estimatedPremiumCents())}" else "Not counted as FLSA-qualified",fontSize=12.sp,color=if(week.flsaEligible)Blue else Muted)
                    }
                    IconButton(onClick={store.deleteOvertime(week.id)}){Icon(Icons.Rounded.DeleteOutline,"Delete",tint=Muted)}
                }
            }
        }
    }
}

@Composable
fun MileageScreen(store:TaxNestStore,addMileage:()->Unit) {
    val miles=store.mileage.sumOf{it.miles}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {PageHeading("ZEUS Mileage","Keep the truck records in one place.")}
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Box(Modifier.size(54.dp).background(Color(0xFFE8EEFF),RoundedCornerShape(17.dp)),contentAlignment=Alignment.Center){Icon(Icons.Rounded.DirectionsCar,null,tint=Blue)}
                Column {MiniLabel("Miles logged");Text("${"%,.1f".format(miles)} mi",fontSize=29.sp,fontWeight=FontWeight.Bold,color=Ink)}
            }
            Spacer(Modifier.height(12.dp))
            PillButton("Log a trip",Icons.Rounded.Add,Modifier.fillMaxWidth(),addMileage)
        } }
        item {GlassCard(Modifier.fillMaxWidth()){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Info,null,tint=Blue);Text("How mileage affects taxes",color=Ink,fontWeight=FontWeight.Bold)}
            Spacer(Modifier.height(6.dp))
            Text(if(store.profile=="W-2") "W-2 employee: ordinary unreimbursed work mileage is generally not a federal employee business deduction. These logs can support your company's accountable reimbursement process; don't double count company-paid fuel or allowances."
            else "1099 contractor: business mileage may qualify, depending on actual business use, reimbursements, and IRS rules. Keep a contemporaneous log; ordinary commuting is generally excluded.",fontSize=12.sp,color=Muted)
        }}
        item { SectionHeading("Trips (${store.mileage.size})") }
        if(store.mileage.isEmpty()) item { EmptyNotice("Your log starts here","Record dates, mileage and business purpose.",Icons.Rounded.DirectionsCar) }
        else items(store.mileage.size) {index->
            val trip=store.mileage[index]
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(trip.purpose,fontWeight=FontWeight.Bold,color=Ink)
                        Text("${trip.date} • ${trip.miles} mi • ${if(trip.reimbursed)"Reimbursed" else "Not reimbursed"}",fontSize=12.sp,color=Muted)
                    }
                    IconButton(onClick={store.deleteMileage(trip.id)}){Icon(Icons.Rounded.DeleteOutline,"Delete",tint=Muted)}
                }
            }
        }
    }
}

private data class Opportunity(val icon:ImageVector,val title:String,val description:String,val action:String?=null,val route:Page?=null)

@Composable
fun TaxBreaksScreen(store:TaxNestStore,go:(Page)->Unit) {
    val opportunityList=buildList {
        add(Opportunity(Icons.Rounded.Schedule,"Qualified overtime compensation",
            "Potential 2026 deduction for eligible FLSA overtime premium. Track weeks, then reconcile with your W-2 box 12 code TT and IRS rules.","Open overtime tracker",Page.Overtime))
        if(store.profile=="W-2") add(Opportunity(Icons.Rounded.DirectionsCar,"Work truck reimbursement",
            "Ask payroll whether your truck allowance and job mileage qualify for tax-free accountable-plan reimbursement. Not a general federal employee mileage deduction.","Track miles",Page.Mileage))
        else {
            add(Opportunity(Icons.Rounded.DirectionsCar,"1099 business mileage",
                "Review eligible business miles versus commuting, reimbursements, and the standard versus actual expense method.","Track miles",Page.Mileage))
            add(Opportunity(Icons.Rounded.Build,"Business tools & supplies",
                "Review ordinary, necessary expenses supported by receipts; account for personal use and equipment depreciation.","Open receipts",Page.Receipts))
            add(Opportunity(Icons.Rounded.AccountBalance,"Self-employment tax & retirement",
                "Review eligible self-employed retirement contributions, health insurance rules, and the deductible portion of self-employment tax."))
        }
        add(Opportunity(Icons.Rounded.Savings,"Retirement savings",
            "Review traditional IRA or workplace retirement deductions and possible Saver's Credit; limits depend on income and other details."))
        add(Opportunity(Icons.Rounded.MedicalServices,"HSA contributions",
            "If you have eligible high-deductible health coverage, review possible deductible HSA contributions."))
        add(Opportunity(Icons.Rounded.School,"Education credits and student loan interest",
            "If you paid qualifying education costs or eligible student loan interest, review income and other limitations."))
        add(Opportunity(Icons.Rounded.EnergySavingsLeaf,"Home improvement and clean energy",
            "Some home energy credits vary by tax year, purchase date and ownership; verify current-year availability before assuming eligibility."))
    }
    val uriHandler=LocalUriHandler.current
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Tax Break Finder","Explore possibilities, never promises.") }
        item { GlassCard(Modifier.fillMaxWidth()) {
            MiniLabel("Tax profile")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("W-2","1099").forEach { mode ->
                    FilterChip(selected=store.profile==mode,onClick={store.updateProfile(mode)},label={Text(mode)},modifier=Modifier.weight(1f))
                }
            }
            Text("Suggestions adjust by work type; a tax professional should verify eligibility.",color=Muted,fontSize=11.sp)
        } }
        item { GlassCard(Modifier.fillMaxWidth()) {
            MiniLabel("Receipts that may need attention")
            val yearEntries=store.entries.filter{it.kind=="expense"&&it.date.startsWith(store.taxYear.toString())}
            val withPhotos=yearEntries.count{!it.receiptUri.isNullOrBlank()}
            val workRelated=yearEntries.filter{it.category.contains("Truck",true)||it.category.contains("Tools",true)||it.category.contains("Fuel",true)}
            Text("${yearEntries.size} recorded expenses · $withPhotos with photos",color=Ink,fontWeight=FontWeight.Bold)
            Text("${workRelated.size} vehicle/tool/fuel entries to classify for potential reimbursement or contractor business use.",color=Muted,fontSize=12.sp)
            Text("Categories alone never establish eligibility; personal use and W-2 rules still apply.",color=Muted,fontSize=11.sp)
        } }
        item { SectionHeading("Opportunities to review (${opportunityList.size})") }
        items(opportunityList.size) { index ->
            val opportunity=opportunityList[index]
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(42.dp).background(Color(0xFFF0EEFF),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(opportunity.icon,null,tint=Lavender)}
                    Column(Modifier.weight(1f)) {
                        Text(opportunity.title,fontWeight=FontWeight.Bold,color=Ink,fontSize=14.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(opportunity.description,color=Muted,fontSize=12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("NEEDS REVIEW",color=Lavender,fontWeight=FontWeight.Bold,fontSize=10.sp)
                        if(opportunity.route!=null && opportunity.action!=null) TextButton(onClick={go(opportunity.route)}){Text(opportunity.action,fontSize=12.sp)}
                    }
                }
            }
        }
        item { OutlinedButton(onClick={uriHandler.openUri("https://www.irs.gov/credits-deductions")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {
            Text("Read official IRS credits & deductions");Spacer(Modifier.width(8.dp));Icon(Icons.Rounded.OpenInNew,null,Modifier.size(16.dp))
        } }
    }
}

@Composable
fun MoreScreen(store:TaxNestStore,go:(Page)->Unit,exportCsv:()->Unit,openInbox:()->Unit,openReports:()->Unit,importCsv:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {PageHeading("Your TaxNest","A little organized goes a long way.")}
        item { GlassCard(Modifier.fillMaxWidth()) {
            MiniLabel("Your work profile")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                listOf("W-2","1099").forEach {mode -> FilterChip(selected=store.profile==mode,onClick={store.updateProfile(mode)},label={Text(mode)},modifier=Modifier.weight(1f)) }
            }
            Text("Change any time. Your existing entries remain saved.",fontSize=11.sp,color=Muted)
            Spacer(Modifier.height(10.dp))
            Text("Tax year",color=Ink,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                (2025..2028).forEach { year ->
                    FilterChip(selected=store.taxYear==year,onClick={store.updateTaxYear(year)},label={Text("$year",fontSize=10.sp)})
                }
            }
            Spacer(Modifier.height(9.dp))
            Text("Federal filing status",fontWeight=FontWeight.SemiBold,color=Ink,fontSize=13.sp)
            listOf("Single","Married filing jointly","Head of household").forEach{option->
                Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.clickable{store.updateFilingStatus(option)}){
                    RadioButton(selected=store.filingStatus==option,onClick={store.updateFilingStatus(option)},modifier=Modifier.size(34.dp))
                    Text(option,color=Ink,fontSize=12.sp)
                }
            }
            Text("Dependents (manual)",fontWeight=FontWeight.SemiBold,color=Ink,fontSize=13.sp)
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={store.updateDependents(store.dependents-1)}){Icon(Icons.Rounded.Remove,"Minus",tint=Blue)}
                Text("${store.dependents}",fontWeight=FontWeight.Bold,color=Ink)
                IconButton(onClick={store.updateDependents(store.dependents+1)}){Icon(Icons.Rounded.Add,"Plus",tint=Blue)}
            }
        } }
        item {GlassCard(Modifier.fillMaxWidth()) {
            ActionRow(Icons.Rounded.Subscriptions,"Subscriptions","Track recurring charges and auto-discover patterns"){go(Page.Subscriptions)}
            HorizontalDivider(color=SoftLine)
            ActionRow(Icons.Rounded.MarkEmailRead,"Smart Inbox","Paste or share email purchase text",openInbox)
            HorizontalDivider(color=SoftLine)
            ActionRow(Icons.Rounded.PictureAsPdf,"Annual PDF Report","Prepare a printable year-end summary",openReports)
            HorizontalDivider(color=SoftLine)
            ActionRow(Icons.Rounded.Schedule,"Overtime Tracker","Track potentially qualified premium"){go(Page.Overtime)}
            HorizontalDivider(color=SoftLine)
            ActionRow(Icons.Rounded.DirectionsCar,"Truck Mileage","Document work-related trips"){go(Page.Mileage)}
            HorizontalDivider(color=SoftLine)
            ActionRow(Icons.Rounded.CreditScore,"Credit Comeback","Monitor card utilization and FICO goals"){go(Page.Credit)}
            HorizontalDivider(color=SoftLine)
            ActionRow(Icons.Rounded.FileDownload,"Export all tax records CSV","Expenses, subscriptions, mileage and overtime",exportCsv)
            HorizontalDivider(color=SoftLine)
            ActionRow(Icons.Rounded.Restore,"Restore TaxNest CSV","Import an earlier TaxNest backup without duplicating records",importCsv)
        }}
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Email,null,tint=Lavender)
                Text("Gmail import",fontWeight=FontWeight.Bold,color=Ink)
                Spacer(Modifier.weight(1f))
                Text("NOT CONNECTED",fontSize=10.sp,color=Lavender,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Text("Gmail inbox access is not connected. You can voluntarily paste or share purchase emails through Smart Inbox; no mailbox-reading permission is requested.",fontSize=12.sp,color=Muted)
        } }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Rounded.PrivacyTip,null,tint=Blue);Text("Your data & privacy",fontWeight=FontWeight.Bold,color=Ink)}
            Spacer(Modifier.height(8.dp))
            Text("Your ledger is stored in this app's private local SQLite database. Original receipt pictures are stored in the visible Pictures/TaxNest/Receipts folder and are not encrypted. They may be accessible to apps with photo permissions. There is no cloud synchronization in this version. CSV exports are also unencrypted.",color=Muted,fontSize=12.sp)
            Spacer(Modifier.height(8.dp))
            Text("Batch 2 preview · Not a tax filing or financial advice service",fontSize=11.sp,color=Muted)
        } }
    }
}

@Composable
fun CreditScreen(store:TaxNestStore,edit:()->Unit) {
    val credit=store.creditScore
    val balance=store.cardBalanceCents
    val limit=store.cardLimitCents
    val utilization=if(limit>0)(balance.toDouble()/limit*100.0) else 0.0
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("Credit Comeback","Build healthy habits, one payment at a time.") }
        item { GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    MiniLabel("Manual FICO score")
                    Text(if(credit>0)"$credit" else "Not entered",fontSize=32.sp,fontWeight=FontWeight.Bold,color=Ink)
                    Text("Goal: 600+",color=Muted,fontSize=12.sp)
                }
                Icon(Icons.Rounded.CreditScore,null,tint=Blue,modifier=Modifier.size(38.dp))
            }
            Spacer(Modifier.height(10.dp))
            PillButton("Update credit snapshot",Icons.Rounded.Edit,Modifier.fillMaxWidth(),edit)
        } }
        item { GlassCard(Modifier.fillMaxWidth()) {
            MiniLabel("Credit card utilization")
            Text(if(limit>0)"${"%.1f".format(utilization)}%" else "Add your card limit",fontSize=26.sp,fontWeight=FontWeight.Bold,color=Ink)
            if(limit>0){
                LinearProgressIndicator(progress={(balance.toFloat()/limit).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp),color=if(utilization>30)Color(0xFFE6A164) else Mint,trackColor=SoftLine)
                Text("Balance ${dollars(balance)} / limit ${dollars(limit)}",fontSize=12.sp,color=Muted)
            }
            Spacer(Modifier.height(8.dp))
            Text("Keep your accounts current first. Lower balances may help when lenders update the bureaus; late payments can have lasting effects.",fontSize=12.sp,color=Muted)
        } }
        item {GlassCard(Modifier.fillMaxWidth()){
            Text("Your comeback checklist",fontWeight=FontWeight.Bold,color=Ink)
            Spacer(Modifier.height(7.dp))
            listOf("Check whether any account is past due and contact its lender","Make at least every minimum payment by the due date","Pay down your existing revolving balances without new charges","Build an emergency cash cushion to avoid new advances","Verify each account on your credit reports").forEachIndexed{index,line->
                Row(Modifier.padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.Top){
                    Icon(Icons.Rounded.CheckCircleOutline,null,tint=Mint,modifier=Modifier.size(18.dp));Text(line,fontSize=12.sp,color=Ink)
                }
            }
            Text("A score increase or deadline cannot be guaranteed.",fontSize=11.sp,color=Muted)
        }}
    }
}
