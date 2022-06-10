package com.lifeSavers.emergencyappsignup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.second_page_guide.*
import kotlinx.android.synthetic.main.third_page_guide.*

class GuidePage3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.first_page_guide)

        LoginPageButton.setOnClickListener {
            val intent = Intent(this, LogInActivity::class.java)
            startActivity(intent)
        }
    }
}