package com.example.evoluznsewingmachine

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class DbHelper (private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Machine_Sewing.db"
        private const val DATABASE_VERSION = 4
        private const val TABLE_NAME = "TableSewingMachine"


        // Columns
        private const val COL_ID = "id"
        private const val COL_TIME = "productionTime"
        private const val COL_PUSH_BACK_COUNT = "pushBackCount"
        private const val COL_TEMPERATURE = "temperature"
        private const val COL_VIBRATION = "vibrationValue"
        private const val COL_OIL_LEVEL = "finalOilLevel"
        private const val COL_THREAD_PERCENT = "threadPercent"
        private const val COL_STITCH_COUNT = "stitchCount"
        private const val COL_DATE_AND_TIME = "dateAndTime"
        private const val COL_RESET = "reset"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIME INTEGER,
                $COL_PUSH_BACK_COUNT INTEGER,
                $COL_TEMPERATURE TEXT,
                $COL_VIBRATION TEXT,
                $COL_OIL_LEVEL TEXT,
                $COL_THREAD_PERCENT FLOAT,
                $COL_STITCH_COUNT INTEGER,
               $COL_DATE_AND_TIME TEXT DEFAULT (datetime('now', 'localtime')),
               $COL_RESET INTEGER DEFAULT 0
               
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        // 🔹 Enable WAL Mode for Fast Writes
//        db.execSQL("PRAGMA journal_mode=WAL;")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(
        time: Int, pushBackCount: Int, temperature: String,
        vibration: String, oilLevel: String, threadPercent: Float, stitchCount: Int
    ): Boolean {
        val db = writableDatabase
        var result: Long = -1

        try {
//            db.beginTransaction()  // Start transaction
            val values = ContentValues().apply {
                put(COL_TIME, time)
                put(COL_PUSH_BACK_COUNT, pushBackCount)
                put(COL_TEMPERATURE, temperature)
                put(COL_VIBRATION, vibration)
                put(COL_OIL_LEVEL, oilLevel)
                put(COL_THREAD_PERCENT, threadPercent)
                put(COL_STITCH_COUNT, stitchCount)

            }

            result = db.insert(TABLE_NAME, null, values)

//            if (result != -1L) {
//                db.setTransactionSuccessful()  // Mark transaction as successful
//            }

        } catch (e: Exception) {
            Log.e("DbHelper", "Error inserting data: ${e.message}")
        } finally {
//            db.endTransaction()  // End transaction properly
            db.close()
        }

        return result != -1L
    }

    fun getMachineData(): MachineData? {
        val db = readableDatabase
        var machineData: MachineData? = null
        var cursor: Cursor? = null

        val AllQueryInOneFun = """
        WITH LastReset AS (
            SELECT MAX($COL_DATE_AND_TIME) AS lastResetTime 
            FROM $TABLE_NAME 
            WHERE $COL_RESET = 1
        )
        SELECT
            COALESCE(SUM($COL_TIME), 0),
            COALESCE(SUM($COL_PUSH_BACK_COUNT), 0),
            COALESCE(SUM($COL_STITCH_COUNT), 0),

           -- Average Sensor Readings After Reset
           (SELECT ROUND(AVG($COL_TEMPERATURE), 2) FROM $TABLE_NAME WHERE $COL_DATE_AND_TIME > (SELECT lastResetTime FROM LastReset)),
           (SELECT ROUND(AVG($COL_VIBRATION), 2) FROM $TABLE_NAME WHERE $COL_DATE_AND_TIME > (SELECT lastResetTime FROM LastReset)),
           (SELECT ROUND(AVG($COL_OIL_LEVEL), 2) FROM $TABLE_NAME WHERE $COL_DATE_AND_TIME > (SELECT lastResetTime FROM LastReset)),

            -- Total Thread Length in cm
            (SELECT ROUND(SUM($COL_THREAD_PERCENT) * 2.54, 2) FROM $TABLE_NAME WHERE $COL_DATE_AND_TIME > (SELECT lastResetTime FROM LastReset)),

            -- Stitch Count Per cm
            (SELECT 
                CASE 
                    WHEN SUM($COL_THREAD_PERCENT) > 0 
                    THEN ROUND(CAST(SUM($COL_STITCH_COUNT) AS FLOAT) /  (SUM($COL_THREAD_PERCENT) ), 2) 
                    ELSE 0 
                END
            FROM $TABLE_NAME WHERE $COL_THREAD_PERCENT > 0 AND $COL_DATE_AND_TIME > (SELECT lastResetTime FROM LastReset))

        FROM $TABLE_NAME 
        WHERE $COL_DATE_AND_TIME > (SELECT lastResetTime FROM LastReset)
        AND DATE($COL_DATE_AND_TIME) = DATE('now')
    """.trimIndent()

        try {
            cursor = db.rawQuery(AllQueryInOneFun, null)

            if (cursor.moveToFirst()) {
                machineData = MachineData(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getInt(2),
                    cursor.getString(3) ?: "N/A",
                    cursor.getString(4) ?: "N/A",
                    cursor.getString(5) ?: "N/A",
                    cursor.getFloat(6),
                    cursor.getInt(7)
                )
            }

        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching machine data: ${e.message}")
        } finally {
            cursor?.close()
            db.close()
        }
        return machineData
    }


    fun resetMachineData() {
        val db = writableDatabase

        try {
            // Mark the latest row as reset = 1
            db.execSQL(
                """
            UPDATE $TABLE_NAME 
            SET $COL_RESET = 1 
            WHERE $COL_ID = (SELECT MAX($COL_ID) FROM $TABLE_NAME)
            """
            )

            // Insert a new row with zero values
            val values = ContentValues().apply {
                put(COL_TIME, 0)
                put(COL_PUSH_BACK_COUNT, 0)
                put(COL_TEMPERATURE, "0")
                put(COL_VIBRATION, "0")
                put(COL_OIL_LEVEL, "0")
                put(COL_THREAD_PERCENT, 0.0f)
                put(COL_STITCH_COUNT, 0)
                put(COL_RESET, 0)  // Fresh start
            }
            db.insert(TABLE_NAME, null, values)

        } catch (e: Exception) {
            Log.e("DbHelper", "Error resetting machine data: ${e.message}")
        } finally {
            db.close()
        }
    }


    fun getMachineDataByDateRange(startDate: String, endDate: String): MachineData? {
        val db = readableDatabase
        var cursor: Cursor? = null
        var machineData: MachineData? = null


        val query = """
    WITH FilteredData AS (
        SELECT *
        FROM $TABLE_NAME
        WHERE DATE($COL_DATE_AND_TIME) BETWEEN ? AND ?
    )
    SELECT
        -- Summing required columns
        COALESCE(SUM($COL_TIME), 0),
        COALESCE(SUM($COL_PUSH_BACK_COUNT), 0),
        COALESCE(SUM($COL_STITCH_COUNT), 0),

        -- Average sensor readings
        ROUND(AVG($COL_TEMPERATURE), 2),
        ROUND(AVG($COL_VIBRATION), 2),
        ROUND(AVG($COL_OIL_LEVEL), 2),

        -- Total Thread Length in cm
        ROUND(SUM($COL_THREAD_PERCENT) * 2.54, 2),

        -- Stitch Count Per cm
        CASE 
            WHEN SUM($COL_THREAD_PERCENT) > 0 
            THEN ROUND(CAST(SUM($COL_STITCH_COUNT) AS FLOAT) / SUM($COL_THREAD_PERCENT), 2) 
            ELSE 0 
        END
         FROM FilteredData
""".trimIndent()


        try {
            cursor = db.rawQuery(query, arrayOf(startDate, endDate))

            if (cursor.moveToFirst()) {
                machineData = MachineData(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getInt(2),
                    cursor.getString(3) ?: "N/A",
                    cursor.getString(4) ?: "N/A",
                    cursor.getString(5) ?: "N/A",
                    cursor.getFloat(6),
                    cursor.getInt(7)
                )


            }

        } catch (e: Exception) {
            Log.e("DbHelper", "Error fetching machine data: ${e.message}")
        } finally {
            cursor?.close()
            db.close()
        }

        return machineData
    }


}


