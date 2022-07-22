package net.veldor.flibusta_test.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import net.veldor.flibusta_test.model.db.entity.ReadedBooks

@Dao
interface ReadedBooksDao {
    @Query("SELECT * FROM ReadedBooks WHERE bookId = :id")
    fun getBookById(id: String?): ReadedBooks?

    @get:Query("SELECT * FROM ReadedBooks")
    val allBooks: List<ReadedBooks?>?

    @Insert
    fun insert(book: ReadedBooks?)
    @Delete
    fun delete(book: ReadedBooks?)


    @Query("DELETE FROM ReadedBooks")
    fun deleteTable(): Int
}