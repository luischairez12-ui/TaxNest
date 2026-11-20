package com.taxnest.app.data

import java.io.Reader
import java.time.LocalDate

/** Imports TaxNest's own CSV backup. Does not read bank, Gmail or third-party files. */
object TaxNestImport {
    data class Result(val added:Int,val skipped:Int)

    private fun rows(reader:Reader):List<List<String>> {
        val text=reader.readText().removePrefix("\uFEFF")
        require(text.length<10_000_000) { "CSV is too large for this backup importer" }
        val records=mutableListOf<List<String>>()
        val cells=mutableListOf<String>();val token=StringBuilder()
        var quoted=false;var i=0
        while(i<text.length) {
            when(val c=text[i]) {
                '"' -> if(quoted && i+1<text.length && text[i+1]=='"') {token.append('"');i++} else quoted=!quoted
                ',' -> if(quoted) token.append(c) else {cells.add(token.toString());token.clear()}
                '\n' -> if(quoted) token.append(c) else {cells.add(token.toString().trimEnd('\r'));token.clear();if(cells.any{it.isNotBlank()})records.add(cells.toList());cells.clear()}
                else -> token.append(c)
            };i++
        }
        require(!quoted) { "Incomplete quoted CSV cell" }
        if(token.isNotEmpty()||cells.isNotEmpty()) {cells.add(token.toString());if(cells.any{it.isNotBlank()})records.add(cells.toList())}
        return records
    }

    fun restore(store:TaxNestStore,reader:Reader):Result {
        val data=rows(reader)
        require(data.firstOrNull()==listOf("record_type","date","description","amount_usd","category","status","detail","receipt_uri")) {
            "This is not a TaxNest CSV export"
        }
        var added=0;var skipped=0
        data.drop(1).forEach{ c ->
            if(c.size!=8){skipped++;return@forEach}
            val type=c[0];val date=c[1];val name=c[2];val amount=c[3]
            val category=c[4];val status=c[5];val detail=c[6];val uri=c[7]
            if(runCatching{LocalDate.parse(date)}.isFailure || name.isBlank()){skipped++;return@forEach}
            when(type) {
                "transaction" -> {
                    val cents=parseMoney(amount)
                    if(cents==null || cents<=0 || status !in listOf("income","expense")) {skipped++;return@forEach}
                    val exists=store.entries.any{it.date==date&&it.merchant==name&&it.amountCents==cents&&it.kind==status}
                    if(exists)skipped++ else {
                        store.addEntry(date,name,cents,category,status,uri.ifBlank{null},null,"restored-csv")
                        added++
                    }
                }
                "subscription" -> {
                    val cents=parseMoney(amount)
                    if(cents==null||cents<=0||status !in listOf("Weekly","Monthly","Yearly")){skipped++;return@forEach}
                    if(store.subscriptions.any{it.name==name&&it.amountCents==cents&&it.nextDueDate==date})skipped++
                    else {store.addSubscription(name,cents,status,date);added++}
                }
                "mileage" -> {
                    val miles=Regex("([0-9]+(?:\\.[0-9]+)?) business miles").find(detail)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
                    if(miles==null||miles<=0){skipped++;return@forEach}
                    if(store.mileage.any{it.date==date&&it.purpose==name&&it.miles==miles})skipped++
                    else {store.addMileage(date,miles,name,status=="reimbursed");added++}
                }
                "overtime" -> {
                    val rate=Regex("Rate \\$([0-9,.]+)").find(detail)?.groupValues?.getOrNull(1)?.let{parseMoney(it)}
                    val hrs=Regex("([0-9.]+) regular hours; ([0-9.]+) OT hours").find(detail)
                    val reg=hrs?.groupValues?.getOrNull(1)?.toDoubleOrNull()
                    val ot=hrs?.groupValues?.getOrNull(2)?.toDoubleOrNull()
                    if(rate==null||reg==null||ot==null){skipped++;return@forEach}
                    if(store.overtime.any{it.weekOf==date&&it.hourlyRateCents==rate&&it.regularHours==reg&&it.overtimeHours==ot})skipped++
                    else {store.addOvertime(date,rate,reg,ot,status=="potentially FLSA eligible");added++}
                }
                else -> skipped++
            }
        }
        return Result(added,skipped)
    }
}
