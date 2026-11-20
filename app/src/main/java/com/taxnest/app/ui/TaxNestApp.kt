package com.taxnest.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxnest.app.data.*
import com.taxnest.app.receipts.ReceiptDraft

enum class Page(val label:String) { Home("Home"), Receipts("Receipts"), Money("Money"), TaxBreaks("Tax Breaks"), More("More"), Subscriptions("Subscriptions"), Overtime("Overtime"), Mileage("Mileage"), Credit("Credit"), SmartInbox("Smart Inbox"), Reports("Reports") }
private enum class Editor { None, Money, Subscription, Overtime, Mileage, Credit }

@Composable
fun TaxNestApp(
    store: TaxNestStore,
    scanning:Boolean,
    scanned:ReceiptDraft?,
    error:String?,
    onErrorDismiss:()->Unit,
    onScannedDismiss:()->Unit,
    onScannedSave:(String,String,Long,String,String,String)->Unit,
    onPickImage:()->Unit,
    onTakePicture:()->Unit,
    onImportText:(String)->Unit,
    onExportPdf:()->Unit,
    onImportCsv:()->Unit,
    onExportCsv:()->Unit
) {
    var page by remember { mutableStateOf(Page.Home) }
    var editor by remember { mutableStateOf(Editor.None) }
    val primary = listOf(Page.Home,Page.Receipts,Page.Money,Page.TaxBreaks,Page.More)
    BackHandler(page !in primary || page != Page.Home) { page=Page.Home }
    Box(Modifier.fillMaxSize().mistBackground()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(page=page,onHome={page=Page.Home})
            Box(Modifier.weight(1f)) {
                when(page) {
                    Page.Home -> HomeScreen(store,{page=it},{editor=Editor.Money},{page=Page.Receipts;onTakePicture()})
                    Page.Receipts -> ReceiptsScreen(store,onTakePicture,onPickImage,{editor=Editor.Money},{page=Page.SmartInbox})
                    Page.Money -> MoneyScreen(store,{editor=Editor.Money},onExportCsv,{page=Page.Subscriptions})
                    Page.TaxBreaks -> TaxBreaksScreen(store,{page=it})
                    Page.More -> MoreScreen(store,{page=it},onExportCsv,{page=Page.SmartInbox},{page=Page.Reports},onImportCsv)
                    Page.Subscriptions -> SubscriptionsScreen(store,{editor=Editor.Subscription})
                    Page.Overtime -> OvertimeScreen(store,{editor=Editor.Overtime})
                    Page.Mileage -> MileageScreen(store,{editor=Editor.Mileage})
                    Page.Credit -> CreditScreen(store,{editor=Editor.Credit})
                    Page.SmartInbox -> SmartInboxScreen(store,onImportText)
                    Page.Reports -> AnnualReportScreen(store,onExportPdf,onExportCsv)
                }
            }
            BottomNav(page,primary) { page=it }
        }
        if(scanning) {
            Box(Modifier.fillMaxSize().background(Color(0x88FFFFFF)),contentAlignment=Alignment.Center) {
                GlassCard { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(25.dp),color=Blue,strokeWidth=3.dp)
                    Text("Saving & reading your receipt…",color=Ink)
                } }
            }
        }
    }
    if(error!=null) AlertDialog(onDismissRequest=onErrorDismiss,title={Text("TaxNest",color=Ink)},text={Text(error)},confirmButton={TextButton(onClick=onErrorDismiss){Text("OK")}})
    if(scanned!=null) ReceiptReviewDialog(scanned,store.entries,onScannedDismiss,onScannedSave)
    when(editor) {
        Editor.Money -> MoneyEditor({editor=Editor.None}) {date,merchant,amount,category,kind -> store.addEntry(date,merchant,amount,category,kind);editor=Editor.None}
        Editor.Subscription -> SubscriptionEditor({editor=Editor.None}){name,amount,frequency,nextDue->store.addSubscription(name,amount,frequency,nextDue);editor=Editor.None}
        Editor.Overtime -> OvertimeEditor({editor=Editor.None}){date,rate,regular,ot,eligible->store.addOvertime(date,rate,regular,ot,eligible);editor=Editor.None}
        Editor.Mileage -> MileageEditor({editor=Editor.None}){date,miles,purpose,reimbursed->store.addMileage(date,miles,purpose,reimbursed);editor=Editor.None}
        Editor.Credit -> CreditEditor(store,{editor=Editor.None}){score,balance,limit->store.updateCredit(score,balance,limit);editor=Editor.None}
        else -> Unit
    }
}

@Composable
private fun TopBar(page:Page,onHome:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(start=21.dp,end=20.dp,top=14.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color.White.copy(alpha=.88f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
            Icon(Icons.Rounded.Wallet,tint=Blue,contentDescription=null)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("TaxNest",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink)
            Text("Your money, organized",fontSize=11.sp,color=Muted)
        }
        Spacer(Modifier.weight(1f))
        if(page !in listOf(Page.Home,Page.Receipts,Page.Money,Page.TaxBreaks,Page.More)) {
            TextButton(onClick=onHome){Icon(Icons.Rounded.ArrowBack,contentDescription="Home");Spacer(Modifier.width(3.dp));Text("Back")}
        } else {
            Box(Modifier.background(Color.White.copy(alpha=.65f),CircleShape).padding(horizontal=12.dp,vertical=7.dp)) {
                Text("TaxNest",color=Muted,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BottomNav(page:Page,items:List<Page>,onClick:(Page)->Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha=.90f),RoundedCornerShape(topStart=24.dp,topEnd=24.dp)).padding(vertical=9.dp,horizontal=5.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
        items.forEach { item ->
            val selected=page==item || (page in listOf(Page.Subscriptions,Page.Overtime,Page.Mileage,Page.Credit,Page.SmartInbox,Page.Reports) && item==Page.More)
            val icon=when(item){Page.Home->Icons.Rounded.Home;Page.Receipts->Icons.Rounded.ReceiptLong;Page.Money->Icons.Rounded.PieChart;Page.TaxBreaks->Icons.Rounded.AutoAwesome;else->Icons.Rounded.GridView}
            Column(Modifier.weight(1f).clickable{onClick(item)}.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(icon,contentDescription=item.label,tint=if(selected)Blue else Muted,modifier=Modifier.size(21.dp))
                Spacer(Modifier.height(4.dp))
                Text(item.label,fontSize=10.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal,color=if(selected)Blue else Muted,maxLines=1)
            }
        }
    }
}

@Composable
fun PageHeading(title:String,subtitle:String) {
    Column(Modifier.padding(bottom=15.dp)) {
        Text(title,fontSize=27.sp,fontWeight=FontWeight.Bold,color=Ink)
        Text(subtitle,fontSize=13.sp,color=Muted)
    }
}

@Composable
fun SectionHeading(text:String,action:String?=null,onAction:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(top=7.dp,bottom=9.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(text,fontWeight=FontWeight.Bold,fontSize=16.sp,color=Ink,modifier=Modifier.weight(1f))
        if(action!=null && onAction!=null) Text(action,color=Blue,fontWeight=FontWeight.SemiBold,fontSize=12.sp,modifier=Modifier.clickable{onAction()})
    }
}

@Composable
fun PillButton(text:String,icon:ImageVector,modifier:Modifier=Modifier,click:()->Unit) {
    Button(onClick=click,modifier=modifier.heightIn(min=48.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Blue)) {
        Icon(icon,contentDescription=null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(text,maxLines=1)
    }
}

@Composable
fun MiniLabel(text:String) { Text(text.uppercase(),color=Muted,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=.9.sp) }

@Composable
fun EmptyNotice(title:String,detail:String,icon:ImageVector) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(45.dp).background(Color(0xFFE9EDFF),CircleShape),contentAlignment=Alignment.Center){Icon(icon,contentDescription=null,tint=Blue)}
            Column { Text(title,fontWeight=FontWeight.SemiBold,color=Ink);Text(detail,fontSize=12.sp,color=Muted) }
        }
    }
}

@Composable
fun ActionRow(icon:ImageVector,title:String,subtitle:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=11.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(41.dp).background(Color(0xFFF0F3FF),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Icon(icon,null,tint=Blue)}
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold,color=Ink,fontSize=14.sp);Text(subtitle,color=Muted,fontSize=12.sp,maxLines=2)}
        Icon(Icons.Rounded.ChevronRight,null,tint=Muted)
    }
}
