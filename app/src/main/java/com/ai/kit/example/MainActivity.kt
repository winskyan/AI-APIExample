package com.ai.kit.example

import android.ai.kit.AiCallback
import android.ai.kit.AiConstants
import android.ai.kit.AiManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ai.kit.example.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), AiCallback {
    companion object {
        const val TAG: String = "AIKit-MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    private var aiManager: AiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkBootStart()

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

        appendResultText("AI Kit Service 已启动，等待命令词识别结果...")

        binding.btnScanWifiQr.setOnClickListener {
            val intent = Intent("com.ai.kit.tools.action.SCAN_QR")
            startActivity(intent)
        }


        binding.btn1.setOnClickListener {
            val commandWordFsaContent =
                "#FSA 1.0;\n" + "0\t1\t<open>\n" + "1\t2\t<di>\n" + "2\t3\t<number>\n" + "3\t4\t<close>\n" + ";\n" + "<open>:打开;\n" + "<di>:第;\n" + "<number>:一|二|三|四|五|六|七|八|九|十|十一|十二|十三|十四|十五|十六;\n" + "<close>:个;"
            val setResult = aiManager?.setCommandWords(
                "rain", commandWordFsaContent, AiConstants.LANGUAGE_TYPE_CHINESE
            )
        }

        binding.btn2.setOnClickListener {
            val commandWordFsaContent =
                "#FSA 1.0;\n" + "0\t1\t<start>\n" + "1\t2\t<play>\n" + ";\n" + "<start>:开始|停止;\n" + "<play>:播放;"
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

        binding.btn5.setOnClickListener {
            val externalStorageDirectory =
                File(android.os.Environment.getExternalStorageDirectory().absolutePath)
            val ret = aiManager?.silentInstall(
                "/sdcard/Download/AI-APIExample.apk",
                true
            )
            Log.d(TAG, "silentInstall: $ret")
        }
    }

    private fun checkBootStart() {
        val isBootStart = intent?.getBooleanExtra("boot_start", false) ?: false
        if (isBootStart) {
            Log.i(TAG, "main is started by boot self start")
        } else {
            Log.i(TAG, "main is started by user click")
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
        val timestamp =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        runOnUiThread { appendResultText("[$timestamp] 识别到命令词: $commandWord") }
    }

    override fun onError(errorCode: Int, errorMessage: String) {
        super.onError(errorCode, errorMessage)
        Log.e(TAG, "onError: $errorCode, $errorMessage")
        runOnUiThread {
            val errorStr = "Error: $errorCode, $errorMessage"
            binding.tvResult.text = errorStr
        }
    }

    /**
     * Scroll to the bottom to display the latest content
     */
    private fun scrollToBottom() {
        binding.scrollViewResult.post {
            binding.scrollViewResult.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    /**
     * Add new result text and automatically scroll to the bottom
     */
    private fun appendResultText(text: String) {
        val currentText = binding.tvResult.text.toString()
        val newText = if (currentText.isEmpty()) {
            text
        } else {
            "$currentText\n$text"
        }
        binding.tvResult.text = newText
        scrollToBottom()
    }

    /**
     * Clear result text
     */
    private fun clearResultText() {
        binding.tvResult.text = ""
    }

}