package com.example.aidocumentorganizer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.aidocumentorganizer.utils.DocumentClassifier

@Database(
    entities = [DocumentEntity::class],
    version = 5,
    exportSchema = false
)
abstract class DocumentDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {

        // v2 -> v3: adds category + expiryDate WITHOUT wiping the user's
        // existing documents, and back-fills a category for old rows by
        // running the classifier over their saved text.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE documents ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'"
                )
                db.execSQL(
                    "ALTER TABLE documents ADD COLUMN expiryDate INTEGER"
                )

                // Read everything first, then update, so we never write to
                // the table while a cursor on it is still open.
                val rows = mutableListOf<Pair<Int, String>>()

                db.query("SELECT id, extractedText FROM documents").use { cursor ->
                    while (cursor.moveToNext()) {
                        rows.add(
                            cursor.getInt(0) to (cursor.getString(1) ?: "")
                        )
                    }
                }

                rows.forEach { (id, text) ->
                    db.execSQL(
                        "UPDATE documents SET category = ? WHERE id = ?",
                        arrayOf<Any>(DocumentClassifier.classify(text), id)
                    )
                }
            }
        }

        // v3 -> v4: adds favorites and comma-separated user tags.
        // Existing documents remain untouched.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE documents ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE documents ADD COLUMN tags TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // v4 -> v5: records how many scanned pages belong to a document.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE documents ADD COLUMN pageCount INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        @Volatile
        private var INSTANCE: DocumentDatabase? = null

        fun getDatabase(context: Context): DocumentDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocumentDatabase::class.java,
                    "document_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    // Only reached for very old (v1) dev installs that have
                    // no migration path.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}
