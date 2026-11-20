package com.taxnest.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.taxnest.app.data.parseMoney
import com.taxnest.app.data.today
import com.taxnest.app.receipts.ReceiptDraft
import java.time.LocalDate

@Composable
private fun EntryDialog(title:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=onClose) {
        Surface(shape=RoundedCornerShape(27.dp),color=Color(0xFFF8FBFF),shadowElevation=10.dp,modifier=Modifier.fillMaxWidth().heightIn(max=670.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(title,fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
                    IconButton(onClick=onClose){Icon(Icons.Rounded.Close,"Close",tint=Muted)}
                }
                Spacer(Modifier.height(11.dp))
                content()
            }
        }
    }
}
@Composable
private fun Field(label:String,value:String,onValue:(String)->Unit,hint:String="") {
    OutlinedTextField(value=value,onValueChange=onValue,label={Text(label)},placeholder={Text(hint)},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(14.dp))
    Spacer(Modifier.height(8.dp))
}
@Composable
private fun SaveButton(text:String="Save",onClick:()->Unit) {
    Spacer(Modifier.height(7.dp))
    Button(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=49.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Blue)) { Text(text) }
}
@Composable
private fun ValidationError(message:String?) {
    if(message!=null) Text(message,color=MaterialTheme.colorScheme.error,fontSize=12.sp,modifier=Modifier.padding(vertical=6.dp))
}
private fun validDate(value:String):Boolean=runCatching{LocalDate.parse(value)}.isSuccess

@Composable
fun MoneyEditor(onClose:()->Unit,onSave:(String,String,Long,String,String)->Unit) {
    var date by remember { mutableStateOf(today()) }
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shopping") }
    var kind by remember { mutableStateOf("expense") }
    var error by remember { mutableStateOf<String?>(null) }
    EntryDialog("Add transaction",onClose) {
        Text("Keep track of income or purchases, even without a receipt.",fontSize=12.sp,color=Muted)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("expense","income").forEach { option ->
                FilterChip(selected=kind==option,onClick={kind=option},label={Text(option.replaceFirstChar{it.uppercase()})},modifier=Modifier.weight(1f))
            }
        }
        Field(if(kind=="income")"Source" else "Merchant / store",merchant,{merchant=it},"e.g. Home Depot")
        Field("Amount (USD)",amount,{amount=it},"e.g. 42.50")
        Field("Date (YYYY-MM-DD)",date,{date=it})
        Field("Category",category,{category=it},"Food, Truck, Work...")
        ValidationError(error)
        SaveButton { val cents=parseMoney(amount)
            if(merchant.isBlank() || cents==null || cents<=0 || !validDate(date) || category.isBlank()) error="Please enter a name, positive amount, category and valid date."
            else onSave(date,merchant.trim(),cents,category.trim(),kind)
        }
    }
}

@Composable
fun ReceiptReviewDialog(draft:ReceiptDraft,existing:List<com.taxnest.app.data.MoneyEntry>,onClose:()->Unit,onSave:(String,String,Long,String,String,String)->Unit) {
    var merchant by remember(draft){mutableStateOf(draft.merchant)}
    var amount by remember(draft){mutableStateOf(draft.amount)}
    var date by remember(draft){mutableStateOf(draft.date)}
    var category by remember(draft){mutableStateOf(draft.category)}
    var error by remember { mutableStateOf<String?>(null) }
    var duplicateConfirmed by remember(draft) { mutableStateOf(false) }
    val matches = com.taxnest.app.data.SmartInsights.duplicateMatches(existing,date,merchant,parseMoney(amount) ?: 0)
    EntryDialog(if(draft.photoUri.isBlank())"Review emailed purchase" else "Review your receipt",onClose) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(draft.photoUri.isNotBlank()) ReceiptThumbnail(draft.photoUri)
            else Icon(Icons.Rounded.MarkEmailRead,null,tint=Blue,modifier=Modifier.size(42.dp))
            Column {
                Text(if(draft.photoUri.isNotBlank())"Original photo saved" else "Imported from shared or pasted text",color=Ink,fontWeight=FontWeight.Bold)
                Text(if(draft.photoUri.isNotBlank())"Pictures/TaxNest/Receipts" else "No image attached to this record",color=Muted,fontSize=11.sp)
                Text("Review suggested details before saving",color=Muted,fontSize=11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Field("Merchant",merchant,{merchant=it})
        Field("Total (USD)",amount,{amount=it},"Enter total if OCR missed it")
        Field("Date (YYYY-MM-DD)",date,{date=it})
        Field("Category",category,{category=it})
        if(matches.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Possible duplicate found!",color=MaterialTheme.colorScheme.error,fontSize=13.sp,fontWeight=FontWeight.Bold)
            matches.take(2).forEach { match ->
                Text("${match.entry.merchant} · ${match.entry.date} · ${com.taxnest.app.data.dollars(match.entry.amountCents)}",color=Muted,fontSize=12.sp)
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=duplicateConfirmed,onCheckedChange={duplicateConfirmed=it})
                Text("This is a different purchase — save anyway",fontSize=12.sp,color=Ink)
            }
        }
        Text("Saving a purchase doesn't mean it is tax deductible. Work expenses need review.",color=Muted,fontSize=12.sp)
        ValidationError(error)
        SaveButton("Save receipt & expense") { val cents=parseMoney(amount)
            if(merchant.isBlank()||cents==null||cents<=0||!validDate(date)||category.isBlank()) error="Review the merchant, total, date and category before saving."
            else if(matches.isNotEmpty()&&!duplicateConfirmed) error="This looks like an existing transaction. Check the box if it is a different purchase."
            else onSave(date,merchant.trim(),cents,category.trim(),draft.photoUri,draft.rawText)
        }
    }
}

@Composable
fun SubscriptionEditor(onClose:()->Unit,onSave:(String,Long,String,String)->Unit) {
    var name by remember{mutableStateOf("")}
    var amount by remember{mutableStateOf("")}
    var frequency by remember{mutableStateOf("Monthly")}
    var nextDate by remember{mutableStateOf(today())}
    var error by remember{mutableStateOf<String?>(null)}
    EntryDialog("New subscription",onClose) {
        Field("Subscription name",name,{name=it},"Netflix, YouTube Premium...")
        Field("Charge amount (USD)",amount,{amount=it})
        Text("Billing frequency",fontWeight=FontWeight.SemiBold,color=Ink)
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("Weekly","Monthly","Yearly").forEach { option ->
                FilterChip(selected=frequency==option,onClick={frequency=option},label={Text(option,fontSize=11.sp)})
            }
        }
        Field("Next due (YYYY-MM-DD)",nextDate,{nextDate=it})
        ValidationError(error)
        SaveButton { val cents=parseMoney(amount)
            if(name.isBlank()||cents==null||cents<=0||!validDate(nextDate)) error="Enter a name, positive charge and valid due date."
            else onSave(name.trim(),cents,frequency,nextDate)
        }
    }
}

@Composable
fun OvertimeEditor(onClose:()->Unit,onSave:(String,Long,Double,Double,Boolean)->Unit) {
    var week by remember{mutableStateOf(today())}
    var rate by remember{mutableStateOf("")}
    var regular by remember{mutableStateOf("40")}
    var overtime by remember{mutableStateOf("")}
    var eligible by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf<String?>(null)}
    EntryDialog("Log overtime",onClose) {
        Text("Enter the actual rate and hours from your paycheck. Do not count the same week twice.",color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(10.dp))
        Field("Week ending or starting (YYYY-MM-DD)",week,{week=it})
        Field("Regular hourly rate (USD)",rate,{rate=it},"e.g. 38")
        Field("Regular hours",regular,{regular=it},"e.g. 40")
        Field("Overtime hours",overtime,{overtime=it},"e.g. 30")
        Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=eligible,onCheckedChange={eligible=it})
            Column {Text("Potentially FLSA-required overtime",fontWeight=FontWeight.SemiBold,color=Ink,fontSize=13.sp)
                Text("Uncheck if overtime is only a voluntary employer premium",color=Muted,fontSize=11.sp)}
        }
        Text("Estimates only; 2026 W-2 box 12 code TT and statutory conditions control the deduction.",color=Muted,fontSize=11.sp)
        ValidationError(error)
        SaveButton { val cents=parseMoney(rate);val reg=regular.toDoubleOrNull();val ot=overtime.toDoubleOrNull()
            if(!validDate(week)||cents==null||cents<=0||reg==null||reg<0||reg>168||ot==null||ot<0||ot>168||reg+ot>168) error="Please enter a valid week, rate and weekly hours."
            else onSave(week,cents,reg,ot,eligible)
        }
    }
}

@Composable
fun MileageEditor(onClose:()->Unit,onSave:(String,Double,String,Boolean)->Unit) {
    var date by remember{mutableStateOf(today())}
    var miles by remember{mutableStateOf("")}
    var purpose by remember{mutableStateOf("")}
    var reimbursed by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    EntryDialog("Log truck mileage",onClose) {
        Text("Keep the date, distance and business purpose. For best records also note your start/end odometer readings outside this first version.",fontSize=12.sp,color=Muted)
        Spacer(Modifier.height(8.dp))
        Field("Date (YYYY-MM-DD)",date,{date=it})
        Field("Miles driven",miles,{miles=it},"e.g. 82.5")
        Field("Trip purpose / job",purpose,{purpose=it},"Job site, equipment pickup...")
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(reimbursed,onCheckedChange={reimbursed=it});Text("Already reimbursed by employer",fontSize=13.sp,color=Ink)}
        ValidationError(error)
        SaveButton { val distance=miles.toDoubleOrNull()
            if(!validDate(date)||distance==null||distance<=0||distance>10000||purpose.isBlank()) error="Enter a date, valid distance and purpose."
            else onSave(date,distance,purpose.trim(),reimbursed)
        }
    }
}

@Composable
fun CreditEditor(store:com.taxnest.app.data.TaxNestStore,onClose:()->Unit,onSave:(Int,Long,Long)->Unit) {
    var score by remember{mutableStateOf(if(store.creditScore>0)store.creditScore.toString() else "")}
    var balance by remember{mutableStateOf(if(store.cardBalanceCents>0)"%.2f".format(java.util.Locale.US,store.cardBalanceCents/100.0) else "")}
    var limit by remember{mutableStateOf(if(store.cardLimitCents>0)"%.2f".format(java.util.Locale.US,store.cardLimitCents/100.0) else "")}
    var error by remember{mutableStateOf<String?>(null)}
    EntryDialog("Update credit snapshot",onClose) {
        Text("Manually track your score and credit card utilization. TaxNest doesn't pull a credit report.",fontSize=12.sp,color=Muted)
        Spacer(Modifier.height(8.dp))
        Field("FICO score",score,{score=it},"e.g. 537")
        Field("Current card balance (USD)",balance,{balance=it})
        Field("Card credit limit (USD)",limit,{limit=it})
        ValidationError(error)
        SaveButton { val s=score.toIntOrNull();val b=parseMoney(balance);val l=parseMoney(limit)
            if(s==null || s !in 300..850 || b==null || b<0 || l==null || l<=0) error="Enter a FICO score from 300–850 and valid card amounts."
            else onSave(s,b,l)
        }
    }
}
