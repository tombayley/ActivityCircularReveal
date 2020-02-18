package com.tombayley.activitycircularrevealexample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import com.tombayley.activitycircularreveal.CircularReveal

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
    View.OnClickListener
{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fab -> {
                val builder = CircularReveal.Builder(
                    this,
                    fab,
                    Intent(this, OtherActivity::class.java),
                    1000
                ).apply {
                    revealColor = ContextCompat.getColor(
                        this@MainActivity,
                        R.color.colorPrimary
                    )
                }

                CircularReveal.presentActivity(builder)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
