package com.jlpt.wordoftheday.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DatabaseSeedCallback(
    private val context: android.content.Context
) : RoomDatabase.Callback() {

    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            val database = WordDatabase.getDatabase(context)
            val dao = database.wordDao()
            if (dao.getCount() == 0) {
                dao.insertAll(JlptVocabularyData.getAllWords())
            }
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        scope.launch {
            val database = WordDatabase.getDatabase(context)
            if (database.wordDao().getCount() == 0) {
                database.wordDao().insertAll(JlptVocabularyData.getAllWords())
            }
        }
    }
}
