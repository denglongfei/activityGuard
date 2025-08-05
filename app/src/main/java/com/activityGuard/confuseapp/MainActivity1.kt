package com.activityGuard.confuseapp


import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.activityGuard.confuseapp.databinding.ActivityMain1Binding
import com.activityGuard.model.Bean
import com.activityGuard.model.Bean1
import com.activityGuard.model.Bean2
import com.activityGuard.model.Bean3
import com.activityGuard.view.PlayView
import com.activityGuard.vm.MainViewModel
import com.ndk.model1.ModelActivity1
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity1 : AppCompatActivity() {
    val viewModel : MainViewModel  by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)
        val view = findViewById<View>(R.id.main)
        println("view -----1 " + view.tag)
        println("---" + Bean().toString() + Bean3().toString() + Bean2().toString())
        val binding =
            DataBindingUtil.setContentView<ActivityMain1Binding>(this, R.layout.activity_main1)
        binding.item = Bean(name ="aaaa")
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
        println("  viewModel  "+viewModel.sss)

    }

    fun changePlayView(playView:PlayView){
        playView.setColor("9999")
    }

    fun changePlayView2(playView:PlayView,ss:String):Bean3{
        playView.setColor("9999")
        return  Bean3("aaaa").apply {
            aaaaaaaaa(Bean1(ss))
        }
    }
}