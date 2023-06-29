package com.example.game
import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameView(this)
        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView?.gameThread?.stopThread()
    }
}
