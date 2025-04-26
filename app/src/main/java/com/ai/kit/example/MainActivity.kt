package com.ai.kit.example

import android.ai.kit.AiCallback
import android.ai.kit.AiConstants
import android.ai.kit.AiManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ai.kit.example.databinding.ActivityMainBinding
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), AiCallback {
    companion object {
        const val TAG: String = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    private var aiManager: AiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()

        initService()
    }

    private fun initService() {
        aiManager = applicationContext.getSystemService("ai") as AiManager
        aiManager?.addListener(this)
    }

    override fun onDestroy() {
        aiManager?.removeListener(this)
        super.onDestroy()
    }

    private fun initView() {
        val version = "Version: ${BuildConfig.VERSION_NAME}"
        binding.tvVersion.text = version

        binding.btn1.setOnClickListener {
            val commandWordFsaContent =
                "#FSA 1.0;\n" + "0\t1\t<a>\n" + "0\t1\t-\n" + "1\t2\t<b>\n" + "2\t3\t<c>\n" + ";\n" + "<a>:今天;\n" + "<b>:你好;\n" + "<c>:了吗|不好;"
            val setResult = aiManager?.setCommandWords(
                "rain", commandWordFsaContent, AiConstants.LANGUAGE_TYPE_CHINESE
            )
        }

        binding.btn2.setOnClickListener {
            val commandWordFsaContent =
                "#FSA 1.0;\n" + "0\t1\t<a>\n" + "0\t1\t-\n" + "1\t2\t<b>\n" + "2\t3\t<c>\n" + ";\n" + "<a>:今天;\n" + "<b>:下雨;\n" + "<c>:了吗|下没;"
            val setResult = aiManager?.setCommandWords(
                "rain2", commandWordFsaContent, AiConstants.LANGUAGE_TYPE_CHINESE
            )
        }

        binding.btn3.setOnClickListener {
            Log.d(
                TAG,
                "getAllCommandWordFsaContent: ${aiManager?.getAllCommandWordFsaContent(AiConstants.LANGUAGE_TYPE_CHINESE)}"
            )
        }

        binding.btn4.setOnClickListener {
            val ret =
                aiManager?.deleteCommandWordFsaContent("rain", AiConstants.LANGUAGE_TYPE_CHINESE)
            Log.d(TAG, "deleteCommandWordFsaContent: $ret")
        }
    }


    private fun exit() {
        finishAffinity()
        finish()
        exitProcess(0)
    }

    private fun updateToolbarTitle(title: String) {
        binding.toolbarTitle.text = title
    }

    override fun onCommandWordRecognized(commandWord: String) {
        Log.d(TAG, "onCommandWordRecognized: $commandWord")
        runOnUiThread { binding.tvResult.text = commandWord }
    }

}