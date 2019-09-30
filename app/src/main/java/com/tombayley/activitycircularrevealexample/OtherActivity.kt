package com.tombayley.activitycircularrevealexample

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.tombayley.activitycircularreveal.CircularReveal

import kotlinx.android.synthetic.main.activity_main.*

class OtherActivity : AppCompatActivity() {

    protected lateinit var mActivityCircularReveal: CircularReveal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other)
//        setSupportActionBar(toolbar)

        val rootView: View = findViewById(R.id.root_coord)

        mActivityCircularReveal = CircularReveal(rootView)
        mActivityCircularReveal.onActivityCreate(intent)
    }

    override fun onBackPressed() {
        mActivityCircularReveal.unRevealActivity(this)
    }

}
