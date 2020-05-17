package com.example.teamtotest.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.example.teamtotest.R
import kotlinx.android.synthetic.main.activity_alert.*

class AlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        setSupportActionBar(alert_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 만들기

        alert_display.setOnClickListener {
            intent = Intent(this, AlertDisplayActivity::class.java)
            startActivity(intent)
        }
    }

    // 뒤로가기 버튼 활성화
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
