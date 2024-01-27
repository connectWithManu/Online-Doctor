package com.example.doctoronline

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)




        Handler(Looper.getMainLooper()).postDelayed({
           if(FirebaseAuth.getInstance().currentUser != null) {
               startActivity(Intent(this@SplashScreen, MainActivity::class.java))
               finish()
           } else {
               startActivity(Intent(this@SplashScreen, WelcomeScreen::class.java))
               finish()
           }
        },3000)

    }
}