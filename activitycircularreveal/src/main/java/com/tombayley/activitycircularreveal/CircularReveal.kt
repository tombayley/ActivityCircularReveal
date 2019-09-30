package com.tombayley.activitycircularreveal

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.ColorUtils.blendARGB
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.abs
import kotlin.math.max

class CircularReveal constructor(var rootLayout: View) {

    protected var originalColor: Int? = null

    protected var revealX: Int = 0
    protected var revealY: Int = 0
    protected var duration: Long = 0
    protected var color: Int = Color.TRANSPARENT

    init {
        val background = rootLayout.background
        if (background is ColorDrawable) originalColor = background.color
    }

    companion object {
        const val EXTRA_REVEAL_X_POS    = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_REVEAL_X_POS"
        const val EXTRA_REVEAL_Y_POS    = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_REVEAL_Y_POS"
        const val EXTRA_COLOR           = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_COLOR"
        const val EXTRA_DURATION        = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_DURATION"

        @JvmStatic
        fun presentActivity(builder: Builder) {
            val options: Bundle? = setup(builder.activity, builder.viewClicked, builder.intent, builder.duration, builder.revealColor)

            if (builder.requestCode == null) {
                ActivityCompat.startActivity(builder.activity, builder.intent, options)
            } else {
                ActivityCompat.startActivityForResult(builder.activity, builder.intent, builder.requestCode!!, options)
            }
        }

        private fun setup(activity: Activity, viewClicked: View, intent: Intent, duration: Long, color: Int): Bundle? {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                activity.startActivity(intent)
                return null
            }

            val location = IntArray(2)
            viewClicked.getLocationInWindow(location)

            intent.putExtra(EXTRA_COLOR, color)
                .putExtra(EXTRA_DURATION, duration)
                .putExtra(EXTRA_REVEAL_X_POS, location[0] + viewClicked.width / 2)
                .putExtra(EXTRA_REVEAL_Y_POS, location[1] + viewClicked.height / 2)

            return ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                View(activity),
                "transition"
            ).toBundle()
        }
    }

    class Builder(
        val activity: Activity,
        val viewClicked: View,
        val intent: Intent,
        val duration: Long
    ) {
        /**
         * Separated to functions for better use with Java
         */
        var requestCode: Int? = null
        var revealColor: Int = Color.TRANSPARENT
    }

    fun onActivityCreate(intent: Intent) {
        if (!intent.hasExtra(EXTRA_REVEAL_X_POS) || !intent.hasExtra(EXTRA_REVEAL_Y_POS) || !intent.hasExtra(EXTRA_DURATION)) {
            rootLayout.visibility = View.VISIBLE
            return
        }

        rootLayout.visibility = View.INVISIBLE
        revealX = intent.getIntExtra(EXTRA_REVEAL_X_POS, 0)
        revealY = intent.getIntExtra(EXTRA_REVEAL_Y_POS, 0)
        duration = intent.getLongExtra(EXTRA_DURATION, duration)
        color = intent.getIntExtra(EXTRA_COLOR, color)

        val viewTreeObserver = rootLayout.viewTreeObserver
        if (!viewTreeObserver.isAlive) return

        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                revealActivity(duration)
                rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    protected fun revealActivity(duration: Long) {
        val maxRadius = max(rootLayout.width, rootLayout.height) * 1.1f

        // make the view visible and start the animation
        rootLayout.visibility = View.VISIBLE

        // duration * 1.2 here to make the animation feel more consistent between revealing and un-revealing
        val circularReveal = getAnimator(0f, maxRadius, (duration * 1.2).toLong())

        circularReveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                circularReveal.cancel()
            }
        })

        circularReveal.start()
        startTimer(true)
    }

    fun unRevealActivity(activity: Activity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            activity.finish()
            return
        }

        val maxRadius = max(rootLayout.width, rootLayout.height) * 1.1f

        val circularReveal = getAnimator(maxRadius, 0f, duration)

        circularReveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                rootLayout.visibility = View.INVISIBLE
                activity.finish()
                circularReveal.cancel()
            }
        })

        circularReveal.start()
        startTimer(false)
    }

    protected fun getAnimator(startRadius: Float, endRadius: Float, duration: Long): Animator {
        val circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, revealX, revealY, startRadius, endRadius)
            .setDuration(duration)
        circularReveal.interpolator = FastOutSlowInInterpolator()
        return circularReveal
    }

    protected var countDownTimer: CountDownTimer? = null

    protected fun startTimer(reveal: Boolean) {
        cancelTimer()

        val alphaFadeLengthPct = 0.4f
        val waitTime: Long = if (reveal) 0 else (alphaFadeLengthPct * duration / 2).toLong()

        Handler().postDelayed({
            val alphaDuration = duration * alphaFadeLengthPct

            countDownTimer = object : CountDownTimer(alphaDuration.toLong(), 1) {
                override fun onTick(millisUntilFinished: Long) {
                    val completePct = abs(alphaDuration - millisUntilFinished) / alphaDuration
                    val pct = 1f - if (reveal) completePct else 1 - completePct
                    if (originalColor != null) rootLayout.setBackgroundColor(blendARGB(originalColor!!, color, pct))
                }

                override fun onFinish() {
                    if (originalColor != null) rootLayout.setBackgroundColor(originalColor!!)
                    cancelTimer()
                }
            }.start()
        }, waitTime)
    }

    protected fun cancelTimer() {
        countDownTimer?.cancel()
    }


}