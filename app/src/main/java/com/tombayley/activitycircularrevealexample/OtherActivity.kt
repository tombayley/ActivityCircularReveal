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

    protected lateinit var activityCircularReveal: CircularReveal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other)

        val rootView: View = findViewById(R.id.root_coord)

        activityCircularReveal = CircularReveal(rootView)
        activityCircularReveal.onActivityCreate(intent)
    }

    override fun onBackPressed() {
        activityCircularReveal.unRevealActivity(this)
    }

}
