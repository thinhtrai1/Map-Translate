package com.app.translation.navigation

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar

class NavigationMediaPlayer: MediaPlayer() {
    private var seekBar: SeekBar? = null
    private val seekHandler = Handler(Looper.getMainLooper())
    private val seekRunnable = {
        if (try { isPlaying } catch (e: IllegalStateException) { false }) {
            seekBar?.progress = currentPosition * 100 / duration
            runnable()
        }
    }

    fun setupWithSeekBar(seekBar: SeekBar) {
        this.seekBar = seekBar
        this.seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar) {
                seekTo(duration * p0.progress / p0.max)
            }
        })
    }

    private fun runnable() {
        seekHandler.postDelayed(seekRunnable, 200)
    }

    override fun start() {
        super.start()
        runnable()
    }
}