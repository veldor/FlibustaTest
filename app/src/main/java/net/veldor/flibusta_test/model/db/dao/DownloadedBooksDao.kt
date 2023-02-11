package net.veldor.flibusta_test.model.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks

@Dao
interface DownloadedBooksDao {
    @Query("SELECT * FROM downloadedbooks WHERE bookId = :id")
    fun getBookById(id: String?): DownloadedBooks?

    @get:Query("SELECT * FROM downloadedbooks")
    val allBooks: List<DownloadedBooks?>?

    @get:Query("SELECT * FROM downloadedbooks")
    val allBooksLive: LiveData<List<DownloadedBooks?>?>?

    @get:Query("SELECT * FROM downloadedbooks ORDER BY id DESC LIMIT 1")
    val lastDownloadedBookLive: LiveData<DownloadedBooks?>?

    @Insert
    fun insert(book: DownloadedBooks?)

    @Delete
    fun delete(book: DownloadedBooks?)

    @Query("DELETE FROM downloadedbooks;")
    fun deleteTable(): Int

    @Query("SELECT COUNT(*) FROM downloadedbooks")
    fun count(): Int

    @Query("SELECT * FROM downloadedbooks ORDER BY id DESC LIMIT :requestedLoadSize OFFSET :requestedStartPosition")
    fun request(requestedStartPosition: Int, requestedLoadSize: Int): List<DownloadedBooks>
}