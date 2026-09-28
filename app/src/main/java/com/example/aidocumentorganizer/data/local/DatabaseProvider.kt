package com.example.aidocumentorganizer.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: DocumentDatabase? = null

    fun getDatabase(context: Context): DocumentDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                DocumentDatabase::class.java,
                "document_database"
            ).build()

            INSTANCE = instance

            instance
        }
    }
}