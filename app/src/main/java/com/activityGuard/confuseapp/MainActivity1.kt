package com.activityGuard.confuseapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.activityGuard.confuseapp.databinding.ActivityMain1Binding
import model.UserModel
import model.UserModel1
import model.UserModel2
import model.UserModel3
import com.activityGuard.view.PlayView
import com.ndk.model1.ModelActivity1

class MainActivity1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)
        val view = findViewById<View>(R.id.main)
        println("view -----1 " + view.tag)
        println("---" + UserModel().toString() + UserModel3().toString() + UserModel2().toString())
        val binding =
            DataBindingUtil.setContentView<ActivityMain1Binding>(this, R.layout.activity_main1)
        binding.item = UserModel("aaaa")
        binding.item2 = "bbbbb"
        println("view -----2 " + binding.root.tag)

        val playView = findViewById<PlayView>(R.id.playView)
        playView.setColor("9999")
        println("view -----3 " + playView.toString() +TestClass())
        changePlayView(playView)
        changePlayView2(playView,"aaaaa")

        playView.setOnClickListener {
            startActivity(Intent(this, ModelActivity1::class.java))
        }

    }

    fun changePlayView(playView:PlayView){
        playView.setColor("9999")
    }

    fun changePlayView2(playView:PlayView,ss:String): UserModel3 {
        playView.setColor("9999")
        return  UserModel3("aaaa").apply {
            aaaaaaaaa(UserModel1(ss))
        }
    }
}