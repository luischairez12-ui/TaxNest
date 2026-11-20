package com.taxnest.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Small offline-first app state. SQLite retains everything across restarts. */
class TaxNestStore(context: Context) {
    private val database = TaxNestDb(context.applicationContext)
    private val preferences = context.getSharedPreferences("taxnest-settings",Context.MODE_PRIVATE)
    var entries: List<MoneyEntry> by mutableStateOf(emptyList()); private set
    var subscriptions: List<Subscription> by mutableStateOf(emptyList()); private set
    var overtime: List<OvertimeWeek> by mutableStateOf(emptyList()); private set
    var mileage: List<MileageTrip> by mutableStateOf(emptyList()); private set
    var profile: String by mutableStateOf(preferences.getString("profile","W-2") ?: "W-2"); private set
    var taxYear: Int by mutableStateOf(preferences.getInt("taxYear", java.time.LocalDate.now().year)); private set
    var filingStatus: String by mutableStateOf(preferences.getString("filingStatus","Single") ?: "Single"); private set
    var dependents: Int by mutableStateOf(preferences.getInt("dependents",0)); private set
    var creditScore: Int by mutableStateOf(preferences.getInt("creditScore",0)); private set
    var cardBalanceCents: Long by mutableStateOf(preferences.getLong("cardBalanceCents",0L)); private set
    var cardLimitCents: Long by mutableStateOf(preferences.getLong("cardLimitCents",0L)); private set
    init { reload() }
    fun reload() {
        entries=database.entries(); subscriptions=database.subscriptions(); overtime=database.overtime(); mileage=database.mileage()
    }
    fun setProfile(value: String) { profile=value; preferences.edit().putString("profile",value).apply() }
    fun setTaxYear(value:Int) { taxYear=value.coerceIn(2025,2028); preferences.edit().putInt("taxYear",taxYear).apply() }
    fun setFilingStatus(value: String) { filingStatus=value; preferences.edit().putString("filingStatus",value).apply() }
    fun setDependents(value: Int) { dependents=value.coerceIn(0,30);preferences.edit().putInt("dependents",dependents).apply() }
    fun updateCredit(score:Int,balance:Long,limit:Long) {
        creditScore=score;cardBalanceCents=balance;cardLimitCents=limit
        preferences.edit().putInt("creditScore",score).putLong("cardBalanceCents",balance).putLong("cardLimitCents",limit).apply()
    }
    fun addEntry(date:String,merchant:String,amount:Long,category:String,kind:String,receiptUri:String?=null,ocrText:String?=null,source:String="manual") {
        database.addEntry(date,merchant,amount,category,kind,receiptUri,ocrText,source); reload()
    }
    fun deleteEntry(id:Long) { database.deleteEntry(id); reload() }
    fun addSubscription(name:String,amount:Long,frequency:String,nextDue:String) { database.addSubscription(name,amount,frequency,nextDue);reload() }
    fun deleteSubscription(id:Long){database.deleteSubscription(id);reload()}
    fun addOvertime(date:String,rate:Long,regular:Double,ot:Double,eligible:Boolean){database.addOvertime(date,rate,regular,ot,eligible);reload()}
    fun deleteOvertime(id:Long){database.deleteOvertime(id);reload()}
    fun addMileage(date:String,miles:Double,purpose:String,reimbursed:Boolean){database.addMileage(date,miles,purpose,reimbursed);reload()}
    fun deleteMileage(id:Long){database.deleteMileage(id);reload()}
    fun monthlyExpenses():Long=entries.filter{it.kind=="expense" && it.date.startsWith(monthStart())}.sumOf{it.amountCents}
    fun monthlyIncome():Long=entries.filter{it.kind=="income" && it.date.startsWith(monthStart())}.sumOf{it.amountCents}
    fun estimatedOtPremium():Long=overtime.filter{it.weekOf.take(4)==taxYear.toString()}.sumOf{it.estimatedPremiumCents()}
}
