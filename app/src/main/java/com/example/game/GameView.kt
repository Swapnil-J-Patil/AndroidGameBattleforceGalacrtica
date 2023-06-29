package com.example.game
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Handler
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    var gameThread: GameThread? = null
    private val paint = Paint()
    private val scorePaint = Paint()
    private val airplaneDrawable = ContextCompat.getDrawable(context, R.drawable.spaceship)
    private val coinDrawable = ContextCompat.getDrawable(context, R.drawable.coin)
    private var airplaneX = 0f
    private var airplaneY = 0f
    private val obstacles = mutableListOf<Pair<Float, Float>>()
    private val coins = CopyOnWriteArrayList<Pair<Float, Float>>()
    private var score = 0
    private var bestScore = 0
    private var gameOver = false
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var asteroidDrawable: Drawable? = null
    private var coinHandler = Handler()
    private val coinRunnable = object : Runnable {
        override fun run() {
            if (!gameOver) {
                addCoins()
                coinHandler.postDelayed(this, 3000) // Add a coin every 10 seconds
            }
        }
    }

    init {
        // Load the asteroid and coin image drawables
        asteroidDrawable = context.resources.getDrawable(R.drawable.asteroid, null)
    }

    init {
        holder.addCallback(this)
        paint.color = Color.WHITE
        scorePaint.color = Color.WHITE
        scorePaint.textSize = 64f

        sharedPreferences = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        bestScore = sharedPreferences.getInt("BestScore", 0)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(holder)
        gameThread?.start()

        startMusic()
        airplaneX = (width / 2).toFloat()
        airplaneY = (height / 2).toFloat()

        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -100f))
        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -300f))
        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -500f))
        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -700f))

        coinHandler.postDelayed(coinRunnable, 3000)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        coinHandler.removeCallbacks(coinRunnable) // Stop adding coins
        stopMusic()
        var retry = true
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                restartGame()
            }
        } else {
            airplaneX = event.x
            airplaneY = event.y
        }
        return true
    }
    private var movie: Movie? = null
    private var gifStartTime: Long = 0
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (movie == null) {
            // Initialize the Movie instance with the GIF drawable
            val inputStream = context.resources.openRawResource(R.raw.starbg)
            movie = Movie.decodeStream(inputStream)
            gifStartTime = System.currentTimeMillis()
            inputStream.close()
        }

        val currentTime = System.currentTimeMillis()
        val duration = movie?.duration() ?: 0
        val gifTime = (currentTime - gifStartTime) % duration.toInt()

        canvas.save()
        canvas.scale(width.toFloat() / movie?.width()!!, height.toFloat() / movie?.height()!!)
        movie?.setTime(gifTime.toInt())
        movie?.draw(canvas, 0f, 0f)
        canvas.restore()

        // Draw the airplane
        airplaneDrawable?.setBounds(
            (airplaneX - 100).toInt(),
            (airplaneY - 100).toInt(),
            (airplaneX + 100).toInt(),
            (airplaneY + 100).toInt()
        )
        airplaneDrawable?.draw(canvas)

        // Draw the asteroids
        val obstaclesCopy = obstacles.toList()
        for (obstacle in obstaclesCopy) {
            val obstacleX = obstacle.first
            val obstacleY = obstacle.second

            asteroidDrawable?.setBounds(
                obstacleX.toInt(),
                obstacleY.toInt(),
                (obstacleX + 200f).toInt(),
                (obstacleY + 200f).toInt()
            )
            asteroidDrawable?.draw(canvas)
        }

        // Draw the coins
        val coinsCopy = CopyOnWriteArrayList(coins)
        for (coin in coinsCopy) {
            val coinX = coin.first
            val coinY = coin.second

            coinDrawable?.setBounds(
                coinX.toInt(),
                coinY.toInt(),
                (coinX + 120f).toInt(),
                (coinY + 120f).toInt()
            )
            coinDrawable?.draw(canvas)
        }

        // Draw the score and best score
        if (gameOver) {
            // Draw the "Game Over" message in the center of the screen
            val gameOverText = "Game Over"
            val gameOverTextWidth = scorePaint.measureText(gameOverText)
            val gameOverTextX = (width - gameOverTextWidth) / 2f
            val gameOverTextY = height / 2f
            canvas.drawText(gameOverText, gameOverTextX, gameOverTextY, scorePaint)

            // Draw the "Tap to Restart" prompt below the "Game Over" message
            val restartText = "Tap to Restart"
            val restartTextWidth = scorePaint.measureText(restartText)
            val restartTextX = (width - restartTextWidth) / 2f
            val restartTextY = gameOverTextY + 100f
            canvas.drawText(restartText, restartTextX, restartTextY, scorePaint)
        } else {
            // Draw the score and best score
            canvas.drawText("Score: $score", 50f, 100f, scorePaint)
            canvas.drawText("Best Score: $bestScore", 50f, 200f, scorePaint)
        }
    }


    private fun startMusic() {
        mediaPlayer = MediaPlayer.create(context, R.raw.backgroundmusic)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private var obstacleSpeed = 35f // Initial speed of the obstacles
    private fun update() {
        if (gameOver) {
            return
        }

        var collisionDetected = false
        for (obstacle in obstacles) {
            val obstacleX = obstacle.first
            val obstacleY = obstacle.second
            if (airplaneY < obstacleY + 100f && airplaneY + 100f > obstacleY &&
                airplaneX + 50f > obstacleX && airplaneX < obstacleX + 200f
            ) {
                // Collision with asteroid occurred
                collisionDetected = true
                playBlastMusic()
                break
            }
        }

        if (collisionDetected) {
            gameOver = true
            if (score > bestScore) {
                bestScore = score
                saveBestScore()
            }
        } else {
            val updatedObstacles = mutableListOf<Pair<Float, Float>>()
            for (obstacle in obstacles) {
                var obstacleX = obstacle.first
                var obstacleY = obstacle.second
                obstacleY += obstacleSpeed // Increase the obstacle speed

                if (obstacleY > height) {
                    obstacleY = -100f
                    obstacleX = (0..(width - 200)).random().toFloat()
                    score++
                }
                updatedObstacles.add(Pair(obstacleX, obstacleY))
            }
            obstacles.clear()
            obstacles.addAll(updatedObstacles)

            val updatedCoins = mutableListOf<Pair<Float, Float>>()
            for (coin in coins) {
                val coinX = coin.first
                var coinY = coin.second
                coinY += obstacleSpeed // Increase the coin speed along with the obstacles

                if (coinY > height) {
                    coins.remove(coin)
                } else if (airplaneY < coinY + 100f && airplaneY + 100f > coinY &&
                    airplaneX + 50f > coinX && airplaneX < coinX + 100f
                ) {
                    // Collision with coin occurred
                    coins.remove(coin)
                    playCoinMusic()
                    score += 5
                } else {
                    updatedCoins.add(Pair(coinX, coinY))
                }
            }
            coins.clear()
            coins.addAll(updatedCoins)

            // Increase the difficulty as time passes
            if (score % 10 == 0 && obstacleSpeed < 20f) {
                obstacleSpeed += 2f
            }
        }
    }

    private fun playBlastMusic() {
        val mediaPlayer = MediaPlayer.create(context, R.raw.blastmusic)
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer.start()
    }

    private fun playCoinMusic() {
        val mediaPlayer = MediaPlayer.create(context, R.raw.coinmusic)
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer.start()
    }

    private fun addCoins() {
        val numCoins = (1..4).random() // Generate a random number of coins (between 1 and 3)
        repeat(numCoins) {
            val coinX = (0..(width - 100)).random().toFloat()
            val coinY = (-height..(-100)).random().toFloat() // Random coin position along the Y-axis
            coins.add(Pair(coinX, coinY))
        }
    }

    private fun saveBestScore() {
        val editor = sharedPreferences.edit()
        editor.putInt("BestScore", bestScore)
        editor.apply()
    }

    private fun restartGame() {
        obstacles.clear()
        coins.clear()

        // Stop the background music if it's playing
        stopMusic()

        // Start playing the background music again
        startMusic()
        score = 0
        gameOver = false
        airplaneX = (width / 2).toFloat()
        airplaneY = (height / 2).toFloat()

        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -100f))
        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -300f))
        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -500f))
        obstacles.add(Pair((0..(width - 200)).random().toFloat(), -700f))
        coinHandler.removeCallbacks(coinRunnable) // Stop adding coins
        coinHandler.postDelayed(coinRunnable, 3000) // Start adding coins after 10 seconds
    }


    inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        private var running = true

        override fun run() {
            while (running) {
                val canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    update()
                    draw(canvas)
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
        fun stopThread() {
            running = false
        }
    }

}