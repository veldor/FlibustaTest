package net.veldor.flibusta_test.model.db

import android.util.Log
import androidx.room.Room
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule

object DatabaseInstance {
    fun dropDownloadQueue() {
        val schedule = mDatabase.booksDownloadScheduleDao().allBooks
        if (schedule != null && schedule.isNotEmpty()) {
            for (b in schedule) {
                mDatabase.booksDownloadScheduleDao().delete(b)
            }
        }
    }

    fun reloadDownloadErrors() {
        val schedule = mDatabase.downloadErrorDao().allBooks
        if (schedule != null && schedule.isNotEmpty()) {
            for (b in schedule) {
                mDatabase.booksDownloadScheduleDao().insert(b as BooksDownloadSchedule)
                mDatabase.downloadErrorDao().delete(b)
            }
        }
    }

    fun dropDownloadErrorsQueue() {
        val schedule = mDatabase.downloadErrorDao().allBooks
        if (schedule != null && schedule.isNotEmpty()) {
            for (b in schedule) {
                mDatabase.downloadErrorDao().delete(b)
            }
        }
    }

    fun reloadDownloadErrorByBookId(bookId: String?) {
        val book = mDatabase.downloadErrorDao().getBookById(bookId)
        Log.d("surprise", "reloadDownloadErrorByBookId: $book")
        if (book != null) {
            mDatabase.booksDownloadScheduleDao().insert(book as BooksDownloadSchedule)
            mDatabase.downloadErrorDao().delete(book)
        }
    }

    fun deleteDownloadErrorByBookId(bookId: String?) {
        val book = mDatabase.downloadErrorDao().getBookById(bookId)
        Log.d("surprise", "reloadDownloadErrorByBookId: $book")
        if (book != null) {
            mDatabase.downloadErrorDao().delete(book)
        }
    }

    var mDatabase: AppDatabase = Room.databaseBuilder(
        App.instance.applicationContext,
        AppDatabase::class.java, "database"
    )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
        )
        .allowMainThreadQueries()
        .build()
}