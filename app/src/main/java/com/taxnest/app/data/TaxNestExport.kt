package com.taxnest.app.data

/** Flat year-end CSV backup for all record types. Photos are separate in Pictures. */
object TaxNestExport {
    private fun escape(value:String):String = "\"${value.replace("\"","\"\"")}\""
    fun csv(store:TaxNestStore):String = buildString {
        appendLine("record_type,date,description,amount_usd,category,status,detail,receipt_uri")
        fun appendRecord(type:String,date:String,description:String,amount:String,category:String,status:String,detail:String,uri:String="") {
            appendLine(listOf(type,date,description,amount,category,status,detail,uri).joinToString(",",transform=::escape))
        }
        store.entries.forEach { e ->
            appendRecord("transaction",e.date,e.merchant,"%.2f".format(java.util.Locale.US,e.amountCents/100.0),e.category,e.kind,e.source,e.receiptUri?:"")
        }
        store.subscriptions.forEach { s ->
            appendRecord("subscription",s.nextDueDate,s.name,"%.2f".format(java.util.Locale.US,s.amountCents/100.0),"Recurring",s.frequency,"Next due ${s.nextDueDate}")
        }
        store.mileage.forEach { trip ->
            appendRecord("mileage",trip.date,trip.purpose,"","Truck",if(trip.reimbursed)"reimbursed" else "not reimbursed","${trip.miles} business miles")
        }
        store.overtime.forEach { week ->
            appendRecord("overtime",week.weekOf,"Weekly OT premium estimate","%.2f".format(java.util.Locale.US,week.estimatedPremiumCents()/100.0),"Overtime",if(week.flsaEligible)"potentially FLSA eligible" else "excluded","Rate ${dollars(week.hourlyRateCents)}; ${week.regularHours} regular hours; ${week.overtimeHours} OT hours")
        }
    }
}
