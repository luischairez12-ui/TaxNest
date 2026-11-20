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

/** Native one-or-more-page PDF; tax figures are documentary totals, never a computed refund. */
object TaxNestReport {
    fun writePdf(store: TaxNestStore, stream: java.io.OutputStream) {
        val pdf=android.graphics.pdf.PdfDocument()
        val ink=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=android.graphics.Color.rgb(28,39,71);textSize=12f}
        val pale=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=android.graphics.Color.rgb(85,99,144);textSize=10f}
        var pageNo=0
        var page:android.graphics.pdf.PdfDocument.Page?=null
        var y=0f
        fun nextPage(){
            page?.let { pdf.finishPage(it) }
            pageNo++
            page=pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNo).create())
            val c=page!!.canvas
            c.drawColor(android.graphics.Color.WHITE)
            ink.textSize=23f;ink.isFakeBoldText=true
            c.drawText("TaxNest | Annual Record Summary",42f,55f,ink)
            ink.textSize=12f;ink.isFakeBoldText=false
            pale.textSize=10f
            c.drawText("${store.taxYear} • ${store.profile} • ${store.filingStatus} • Page $pageNo",42f,77f,pale)
            y=108f
        }
        fun line(text:String,large:Boolean=false) {
            if(page==null||y>786f)nextPage()
            ink.textSize=if(large)15f else 11f
            ink.isFakeBoldText=large
            val max=if(large)62 else 93
            val c=page!!.canvas
            c.drawText(text.replace('\n',' ').take(max),42f,y,ink)
            y+=if(large)25f else 19f
            ink.isFakeBoldText=false
        }
        fun section(title:String){y+=9f;line(title,true)}
        try {
            nextPage()
            line("Generated ${today()}. Local records only; does not file or prepare taxes.")
            section("Recorded cash flow")
            val annual=store.entries.filter{it.date.startsWith(store.taxYear.toString())}
            val income=annual.filter{it.kind=="income"}.sumOf{it.amountCents}
            val expenses=annual.filter{it.kind=="expense"}.sumOf{it.amountCents}
            line("Recorded income: ${dollars(income)}")
            line("Recorded spending: ${dollars(expenses)}")
            line("Transactions: ${annual.size}  |  receipts with photos: ${annual.count{!it.receiptUri.isNullOrBlank()}}")
            section("Spending by category — not automatically deductible")
            SmartInsights.taxYearSummary(store.entries,store.taxYear).toSortedMap().forEach{(category,total)->line("$category: ${dollars(total)}")}
            section("Overtime and work-related records")
            val weekly=store.overtime.filter{it.weekOf.startsWith(store.taxYear.toString())}
            line("Overtime weeks logged: ${weekly.size}")
            line("Estimated potentially qualifying FLSA overtime premium: ${dollars(weekly.sumOf{it.estimatedPremiumCents()})}")
            line("Compare against employer payroll records and applicable federal limits.")
            val trips=store.mileage.filter{it.date.startsWith(store.taxYear.toString())}
            line("Business-purpose miles recorded: ${"%.1f".format(java.util.Locale.US,trips.sumOf{it.miles})}")
            line("Miles marked reimbursed: ${"%.1f".format(java.util.Locale.US,trips.filter{it.reimbursed}.sumOf{it.miles})}")
            section("Recurring subscriptions")
            store.subscriptions.take(25).forEach{line("${it.name}: ${dollars(it.amountCents)} ${it.frequency}; next ${it.nextDueDate}")}
            section("Documents / tax preparation checklist")
            line("[ ] W-2 forms, 1099s and any year-end earnings records")
            line("[ ] Eligible overtime premium reported by employer / payroll")
            line("[ ] Mileage logs, truck allowance and reimbursement policy")
            line("[ ] Itemized business receipts (especially for 1099 work)")
            line("[ ] Relevant education, retirement, HSA or other tax documents")
            section("Important: no automatic deduction or tax savings claim")
            line("Employee expenses and normal commuting are generally not federal W-2 write-offs.")
            line("Personal spending is not deductible just because a receipt is saved.")
            line("Confirm classification, dates, tax-year rules and limits with a qualified preparer.")
            page?.let{pdf.finishPage(it)}
            pdf.writeTo(stream)
        } finally {pdf.close()}
    }
}
