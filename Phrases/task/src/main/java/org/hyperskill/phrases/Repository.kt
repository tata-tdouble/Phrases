package org.hyperskill.phrases

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class Repository(context: Context) {

    val database = (context.applicationContext as PhrasesApplication).database

    fun insert(phrase: Phrase): Int {
        database.getPhrasesDao().insert(phrase)
        return database.getPhrasesDao().getMostRecentPhrase()!!.id
    }


    fun getPhrases(): Flow<List<Phrase>> {
        return database.getPhrasesDao().getAll()
    }

    fun delete(phrase: Phrase) {
        CoroutineScope(Dispatchers.IO).launch {
            database.getPhrasesDao().delete(phrase)
        }
    }

    fun checkDatabaseActive() {
        println("here")
        runBlocking {
            try {
                val count = database.getPhrasesDao().getAll().first()
                println("here 2 ${count}")
                count.forEach{
                    println("Database Items ID : ${it.id} PHRASE : ${it.phrase}")
                }
            } catch (e: Exception) {
                println("Repository Database is not active or has issues: ${e.message}")
            }
        }
    }

}