package com.ccxmt.aiuidemo

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechUtility
import java.io.InputStream

class mApp : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        /**
         * 读取assets目录下的配置文件
         */
        fun getAIUIParamas(): String? {
            var params = ""
            val assets = context.resources.assets
            val ins: InputStream = assets.open("cfg/aiui_phone.cfg")
            val buffer = ByteArray(ins.available())
            ins.read(buffer)
            ins.close()
            params = String(buffer)
            return params

        }
    }


    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        InitIfly()
    }




    private fun InitIfly() {

        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=0023b74d");

    }


}