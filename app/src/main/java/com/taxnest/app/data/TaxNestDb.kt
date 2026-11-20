package com.taxnest.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TaxNestDb(context: Context) : SQLiteOpenHelper(context, "taxnest.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            entry_date TEXT NOT NULL,
            merchant TEXT NOT NULL,
            amount_cents INTEGER NOT NULL,
            category TEXT NOT NULL,
            kind TEXT NOT NULL,
            receipt_uri TEXT,
            ocr_text TEXT,
            source TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE subscriptions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            amount_cents INTEGER NOT NULL,
            frequency TEXT NOT NULL,
            next_due TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE overtime (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            week_of TEXT NOT NULL,
            hourly_rate_cents INTEGER NOT NULL,
            regular_hours REAL NOT NULL,
            overtime_hours REAL NOT NULL,
            flsa_eligible INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE mileage (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            trip_date TEXT NOT NULL,
            miles REAL NOT NULL,
            purpose TEXT NOT NULL,
            reimbursed INTEGER NOT NULL
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { /* add migrations for V2 */ }

    fun addEntry(date: String, merchant: String, cents: Long, category: String, kind: String,
                 receiptUri: String? = null, ocrText: String? = null, source: String = "manual"): Long {
        val v = ContentValues().apply {
            put("entry_date", date); put("merchant", merchant); put("amount_cents", cents)
            put("category", category); put("kind", kind); put("receipt_uri", receiptUri)
            put("ocr_text", ocrText); put("source", source)
        }
        return writableDatabase.insertOrThrow("entries", null, v)
    }
    fun entries(): List<MoneyEntry> = readableDatabase.rawQuery(
        "SELECT id,entry_date,merchant,amount_cents,category,kind,receipt_uri,ocr_text,source FROM entries ORDER BY entry_date DESC,id DESC", null
    ).use { c ->
        buildList { while (c.moveToNext()) add(MoneyEntry(c.long(0), c.string(1), c.string(2), c.long(3), c.string(4), c.string(5), c.nullString(6), c.nullString(7), c.string(8))) }
    }
    fun deleteEntry(id: Long) { writableDatabase.delete("entries", "id=?", arrayOf(id.toString())) }

    fun addSubscription(name: String, amount: Long, frequency: String, nextDue: String) {
        val v = ContentValues().apply { put("name",name); put("amount_cents",amount); put("frequency",frequency); put("next_due",nextDue) }
        writableDatabase.insertOrThrow("subscriptions",null,v)
    }
    fun subscriptions(): List<Subscription> = readableDatabase.rawQuery(
        "SELECT id,name,amount_cents,frequency,next_due FROM subscriptions ORDER BY next_due,name", null
    ).use { c -> buildList { while(c.moveToNext()) add(Subscription(c.long(0),c.string(1),c.long(2),c.string(3),c.string(4))) } }
    fun deleteSubscription(id: Long) { writableDatabase.delete("subscriptions", "id=?",arrayOf(id.toString())) }

    fun addOvertime(weekOf: String, hourlyCents: Long, regularHours: Double, overtimeHours: Double, flsaEligible: Boolean) {
        val v=ContentValues().apply { put("week_of",weekOf);put("hourly_rate_cents",hourlyCents);put("regular_hours",regularHours);put("overtime_hours",overtimeHours);put("flsa_eligible",if(flsaEligible)1 else 0) }
        writableDatabase.insertOrThrow("overtime",null,v)
    }
    fun overtime(): List<OvertimeWeek> = readableDatabase.rawQuery(
        "SELECT id,week_of,hourly_rate_cents,regular_hours,overtime_hours,flsa_eligible FROM overtime ORDER BY week_of DESC,id DESC",null
    ).use { c -> buildList { while(c.moveToNext()) add(OvertimeWeek(c.long(0),c.string(1),c.long(2),c.double(3),c.double(4),c.int(5)==1)) } }
    fun deleteOvertime(id: Long) { writableDatabase.delete("overtime","id=?",arrayOf(id.toString())) }

    fun addMileage(date: String, miles: Double, purpose: String, reimbursed: Boolean) {
        val v=ContentValues().apply { put("trip_date",date);put("miles",miles);put("purpose",purpose);put("reimbursed",if(reimbursed)1 else 0) }
        writableDatabase.insertOrThrow("mileage",null,v)
    }
    fun mileage(): List<MileageTrip> = readableDatabase.rawQuery(
        "SELECT id,trip_date,miles,purpose,reimbursed FROM mileage ORDER BY trip_date DESC,id DESC",null
    ).use { c -> buildList { while(c.moveToNext()) add(MileageTrip(c.long(0),c.string(1),c.double(2),c.string(3),c.int(4)==1)) } }
    fun deleteMileage(id: Long) { writableDatabase.delete("mileage","id=?",arrayOf(id.toString())) }

    private fun Cursor.string(index: Int): String = getString(index)
    private fun Cursor.nullString(index: Int): String? = if (isNull(index)) null else getString(index)
    private fun Cursor.long(index: Int): Long = getLong(index)
    private fun Cursor.double(index: Int): Double = getDouble(index)
    private fun Cursor.int(index: Int): Int = getInt(index)
}
