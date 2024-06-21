package com.mygame.battleforcegalactica
import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class HomeActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val startgame = findViewById<TextView>(R.id.start)
        val howtoplay = findViewById<TextView>(R.id.howtoplay)

        if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
            mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
        startgame.setOnClickListener{
            val intent = Intent(this@HomeActivity, MainActivity::class.java)
            startActivity(intent)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        howtoplay.setOnClickListener{
            val intent = Intent(this@HomeActivity, HowToPlay::class.java)
            startActivity(intent)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onResume() {
        super.onResume()
        if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
            mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }
}
