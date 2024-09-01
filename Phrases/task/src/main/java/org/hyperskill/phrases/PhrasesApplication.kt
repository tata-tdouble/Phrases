package org.hyperskill.phrases

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.GlobalScope

class PhrasesApplication: Application() {
    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "phrases.db"
        ).allowMainThreadQueries().build()
    }

}