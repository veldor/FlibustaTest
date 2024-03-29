package net.veldor.flibusta_test.model.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.veldor.flibusta_test.model.db.dao.*
import net.veldor.flibusta_test.model.db.entity.*

@Database(
    entities = [ReadedBooks::class, DownloadedBooks::class, BooksDownloadSchedule::class, Bookmark::class, DownloadError::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readBooksDao(): ReadedBooksDao
    abstract fun downloadedBooksDao(): DownloadedBooksDao
    abstract fun booksDownloadScheduleDao(): BooksDownloadScheduleDao
    abstract fun bookmarksDao(): BookmarksDao
    abstract fun downloadErrorDao(): DownloadErrorDao

    companion object {
        @JvmField
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("drop table ReadedBooks;")
                database.execSQL("create table ReadedBooks (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL);")
            }
        }

        @JvmField
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("create table DownloadedBooks (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL);")
            }
        }

        @JvmField
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("create table BooksDownloadSchedule (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL, link TEXT NOT NULL, name TEXT NOT NULL, size TEXT NOT NULL, author TEXT NOT NULL, format TEXT NOT NULL);")
            }
        }

        @JvmField
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE BooksDownloadSchedule ADD COLUMN authorDirName TEXT NOT NULL DEFAULT \"\"")
                database.execSQL("ALTER TABLE BooksDownloadSchedule ADD COLUMN sequenceDirName TEXT NOT NULL  DEFAULT \"\"")
            }
        }

        @JvmField
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE BooksDownloadSchedule ADD COLUMN reservedSequenceName TEXT NOT NULL  DEFAULT \"\"")
            }
        }

        @JvmField
        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE Bookmark (id INTEGER primary key autoincrement NOT NULL, name TEXT NOT NULL DEFAULT \"\", link TEXT NOT NULL DEFAULT \"\")"
                )
            }
        }

        @JvmField
        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "create table ScheduleTemp (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL, link TEXT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "size TEXT NOT NULL, " +
                            "author TEXT NOT NULL, " +
                            "format TEXT NOT NULL, " +
                            "authorDirName TEXT NOT NULL, " +
                            "sequenceDirName TEXT NOT NULL, " +
                            "reservedSequenceName TEXT NOT NULL);"
                )
                //database.execSQL ("INSERT INTO ScheduleTemp (id, bookId, link, name, size, author, format, authorDirName,sequenceDirName,reservedSequenceName) SELECT id, bookId, link, name, size, author, format, authorDirName,sequenceDirName,reservedSequenceName FROM BooksDownloadSchedule")
                database.execSQL("DROP TABLE BooksDownloadSchedule")
                database.execSQL("ALTER TABLE ScheduleTemp RENAME TO BooksDownloadSchedule")
            }
        }

        @JvmField
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "create table DownloadError (" +
                            "id integer primary key autoincrement NOT NULL," +
                            "bookId TEXT NOT NULL," +
                            "link TEXT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "size TEXT NOT NULL, " +
                            "author TEXT NOT NULL, " +
                            "format TEXT NOT NULL, " +
                            "authorDirName TEXT NOT NULL, " +
                            "sequenceDirName TEXT NOT NULL, " +
                            "reservedSequenceName TEXT NOT NULL," +
                            "error TEXT NOT NULL" +
                            ");"
                )
            }
        }
        @JvmField
        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE DownloadedBooks ADD COLUMN destination TEXT DEFAULT \"\"")

            }
        }
        @JvmField
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE DownloadedBooks ADD COLUMN relativePath TEXT DEFAULT \"\"")

            }
        }
    }
}