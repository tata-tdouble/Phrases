package org.hyperskill.phrases

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hyperskill.phrases.databinding.ActivityMainBinding
import org.hyperskill.phrases.databinding.DialogLayoutBinding
import java.util.Calendar


class MainActivity : AppCompatActivity(), Callback {

    val channelId = "org.hyperskill.phrases"
    val channelName = "Phrases App"

    private lateinit var binding: ActivityMainBinding
    private lateinit var customAdapter : RecyclerAdapter
    private lateinit var timePickerDialog : TimePickerDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //create adapter
        customAdapter = RecyclerAdapter( this, this, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = customAdapter

        createNotificationChannel(this)

        binding.reminderTextView.setOnClickListener {
            //check for empty list
            if (customAdapter.phrases.isEmpty()){
                Toast.makeText(this, "Error: Empty database", Toast.LENGTH_SHORT).show()
                //showTimePickerDialog(this, Phrase("error: the list is empty"))
            } else {
                // get random phrase
                val data = customAdapter.phrases[(0..customAdapter.phrases.lastIndex).random()]
                //show time picker
                showTimePickerDialog(this, data)
            }

        }
        binding.addButton.setOnClickListener{
            checkNotificationChannel(this)
            customAdapter.repo.checkDatabaseActive()
            showDialog()
        }


    }


    fun checkNotificationChannel(context: Context) : Boolean {
        var res : NotificationChannel? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            res = notificationManager.getNotificationChannel(channelId)
            if (res != null) {
                println("NOtification $res")
                if (isAlarmSet(this, 18))
                    println("ALarm is set")
            } else {
                println("notification is null")
            }
        }
        return res == null
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, channelName, importance)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    fun isAlarmSet(context: Context, id: Int): Boolean {
        val intent = Intent(context, AlarmNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE // This flag checks if the PendingIntent already exists
        )
        return pendingIntent != null
    }

    fun showTimePickerDialog(context: Context, phrase: Phrase) {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        timePickerDialog = object : TimePickerDialog(context,
            { view, selectedHour, selectedMinute ->
                //format time
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                //update ui
                binding.reminderTextView.text = "Reminder set for $formattedTime"
                //set alarm
                setDailyAlarm(context, phrase, selectedHour, selectedMinute)

                println("Alarm called")
                if(isAlarmSet(context, phrase.id))
                    println("Alarm is set: ${phrase.id} phrase ${phrase.phrase}")
            },
            hour, minute, true) { // true indicates 24 hour format
            override fun onTimeChanged(view: TimePicker?, hourOfDay: Int, minute: Int) {
                super.onTimeChanged(view, hourOfDay, minute)
                val button = findViewById<Button>(android.R.id.button1)
                button.isEnabled = true
            }
        }
        timePickerDialog.show()
    }

    fun showDialog(){
        val context = this

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Enter phrase")

        val input = DialogLayoutBinding.inflate(layoutInflater)

        builder.setView(input.root)
        builder.setPositiveButton("OK") { dialog, which ->
            val userInput = input.editText.text.toString()

            // Do something with the user input here
            val phrase = Phrase(userInput)
            if (checkNotificationChannel(this)){
                createNotificationChannel(this)
            }
            customAdapter.addPhrase(phrase)
            Log.d("TAG", "ALARM CREATED")
            Log.d("TAG", "id: ${phrase.id}")
            Log.d("TAG", "phrase: ${phrase.phrase}")
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.create().show()
    }

    fun setDailyAlarm(context: Context, phrase: Phrase, selectedHour: Int, selectedMinute: Int) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Check if selected time has already passed for the current day
        val shouldTriggerTomorrow = selectedHour < currentHour || (selectedHour == currentHour && selectedMinute <= currentMinute)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmNotificationReceiver::class.java)
        intent.putExtra("phrase", phrase.phrase)
        val pendingIntent = PendingIntent.getBroadcast(context, phrase.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val triggerAtMillis: Long = if (shouldTriggerTomorrow) {
            // Set alarm for tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        } else {
            // Set alarm for today (considering it might be the next day due to time passing)
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val currentTimeInMillis = calendar.timeInMillis
            if (currentTimeInMillis < System.currentTimeMillis()) {
                // If selected time already passed today, set for tomorrow
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            } else {
                currentTimeInMillis
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun resetReminderTV() {
        binding.reminderTextView.text = "No reminder set"
    }

}