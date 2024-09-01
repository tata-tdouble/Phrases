package org.hyperskill.phrases


import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hyperskill.phrases.databinding.RecyclerViewItemBinding


interface Callback {
    fun resetReminderTV()
}


class RecyclerAdapter(val callback: Callback, val context: Context, val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<RecyclerAdapter.ItemViewHolder>() {


    val phrases = mutableListOf<Phrase>()

    val channelId = "org.hyperskill.phrases"

    val repo = Repository(context)

    init {
        MainScope().launch {
            //add items to list
            val item = repo.getPhrases().first()
            item.forEachIndexed{ i, it ->
                println("POSITION ${i}")
                println("ID ${it.id}")
                println("PHRASE ${it.phrase}")
            }
            phrases.addAll(item)
            notifyDataSetChanged()
        }
    }

    fun addPhrase(phrase: Phrase){
        val id = repo.insert(phrase)
        phrase.id = id
        phrases.add(phrase)
        notifyItemInserted(phrases.lastIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            RecyclerViewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.apply {

            phraseTextView.text = phrases[position].phrase
            deleteTextView.setOnClickListener {
                Log.d("TAG", "ALARM DELETED")
                Log.d("TAG", "position: $position")
                Log.d("TAG", "id: ${phrases[position].id}")
                Log.d("TAG", "phrase: ${phrases[position].phrase}")

                // Delete the phrase and its associated alarm

                repo.delete(phrases[position])
                // Remove the phrase from the list and update the UI
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    phrases[position].id,
                    Intent(context, AlarmNotificationReceiver::class.java), // Make sure to specify the right receiver class
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                phrases.removeAt(position)
                notifyDataSetChanged()

                // Check if the list is empty and reset reminder TV
                if (phrases.isEmpty()) {
                    callback.resetReminderTV()

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    notificationManager.cancelAll()
                    notificationManager.deleteNotificationChannel(channelId)

                }
            }

        }
    }

    override fun getItemCount(): Int {
        return phrases.size
    }


    class ItemViewHolder(val binding: RecyclerViewItemBinding) : ViewHolder(binding.root)

}