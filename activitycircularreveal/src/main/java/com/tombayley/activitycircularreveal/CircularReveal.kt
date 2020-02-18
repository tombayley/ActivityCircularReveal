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

class CircularReveal(
    val rootLayout: View
) {

    // Background color of new activity
    protected var originalColor: Int? = null

    // X and Y positions relative to activity of the view clicked
    protected var revealX: Int = 0
    protected var revealY: Int = 0

    // Length of animation
    protected var duration: Long = 0

    // Color used in the reveal animation
    protected var color: Int = Color.TRANSPARENT

    // Duration is this many times larger when revealing to make
    // the animation feel more consistent between revealing and un-revealing
    protected var revealDurationMult = 1.8

    protected var countDownTimer: CountDownTimer? = null

    init {
        val background = rootLayout.background
        if (background is ColorDrawable) originalColor = background.color
    }

    companion object {
        const val EXTRA_REVEAL_X_POS    = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_REVEAL_X_POS"
        const val EXTRA_REVEAL_Y_POS    = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_REVEAL_Y_POS"
        const val EXTRA_COLOR           = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_COLOR"
        const val EXTRA_DURATION        = BuildConfig.LIBRARY_PACKAGE_NAME + ".EXTRA_DURATION"

        /**
         * Starts a new activity using the options defined in the builder
         *
         * @param builder Builder
         */
        @JvmStatic
        fun presentActivity(builder: Builder) {
            val options: Bundle? = setup(builder)

            if (builder.requestCode == null) {
                ActivityCompat.startActivity(builder.activity, builder.intent, options)
            } else {
                ActivityCompat.startActivityForResult(
                    builder.activity, builder.intent, builder.requestCode!!, options
                )
            }
        }

        /**
         * Creates activity options and sets up the animation options
         *
         * @param builder Builder
         * @return Bundle?
         */
        private fun setup(builder: Builder): Bundle? {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) return null

            val location = IntArray(2)
            builder.viewClicked.getLocationInWindow(location)

            builder.intent.putExtra(EXTRA_COLOR, builder.revealColor)
                .putExtra(EXTRA_DURATION, builder.duration)
                .putExtra(EXTRA_REVEAL_X_POS, location[0] + builder.viewClicked.width / 2)
                .putExtra(EXTRA_REVEAL_Y_POS, location[1] + builder.viewClicked.height / 2)

            return ActivityOptionsCompat.makeSceneTransitionAnimation(
                builder.activity,
                View(builder.activity),
                "transition"
            ).toBundle()
        }
    }

    /**
     * Used to configure the animation
     *
     * @property activity Activity
     * @property viewClicked View
     * @property intent Intent
     * @property duration Long
     * @property requestCode Int?
     * @property revealColor Int
     * @constructor
     */
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

    /**
     * Must be called in onCreate() of the activity being revealed
     *
     * @param intent Intent
     */
    fun onActivityCreate(intent: Intent) {
        if (!intent.hasExtra(EXTRA_REVEAL_X_POS) ||
            !intent.hasExtra(EXTRA_REVEAL_Y_POS) ||
            !intent.hasExtra(EXTRA_DURATION)
        ) {
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

    /**
     * Begins the animation for revealing the activity
     *
     * @param duration Long
     */
    protected fun revealActivity(duration: Long) {
        val maxRadius = max(rootLayout.width, rootLayout.height) * 1.1f

        // make the view visible and start the animation
        rootLayout.visibility = View.VISIBLE

        val circularReveal = getAnimator(
            0f,
            maxRadius,
            (duration * revealDurationMult).toLong()
        )

        circularReveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                circularReveal.cancel()
            }
        })

        circularReveal.start()
        startTimer(true)
    }

    /**
     * Begins the animation for un-revealing the activity
     *
     * @param activity Activity
     */
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

    /**
     * Creates the reveal animation
     *
     * @param startRadius Float
     * @param endRadius Float
     * @param duration Long
     * @return Animator
     */
    protected fun getAnimator(startRadius: Float, endRadius: Float, duration: Long): Animator {
        return ViewAnimationUtils.createCircularReveal(
            rootLayout, revealX, revealY, startRadius, endRadius
        ).apply {
            setDuration(duration)
            interpolator = FastOutSlowInInterpolator()
        }
    }

    /**
     * Used to animate the reveal/un-reveal with color
     *
     * @param reveal Boolean
     */
    protected fun startTimer(reveal: Boolean) {
        cancelTimer()

        val alphaFadeLengthPct = 0.4f
        val waitTime =
            if (reveal) 0 else (alphaFadeLengthPct * duration * 1/revealDurationMult).toLong()

        Handler().postDelayed({
            val alphaDuration = duration * alphaFadeLengthPct

            countDownTimer = object : CountDownTimer(alphaDuration.toLong(), 1) {
                override fun onTick(millisUntilFinished: Long) {
                    val completePct = abs(alphaDuration - millisUntilFinished) / alphaDuration
                    val pct = 1f - if (reveal) completePct else 1 - completePct

                    if (originalColor != null) {
                        rootLayout.setBackgroundColor(blendARGB(originalColor!!, color, pct))
                    }
                }

                override fun onFinish() {
                    if (originalColor != null) rootLayout.setBackgroundColor(originalColor!!)
                    cancelTimer()
                }
            }.start()
        }, waitTime)
    }

    /**
     * Cancels the timer used for coloring the animation
     */
    protected fun cancelTimer() {
        countDownTimer?.cancel()
    }

}
