package com.xlsoft.trywebapi

import android.R.attr.bitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    companion object{
        private const val DEBUG_TAG="TryMoe"
        private const val WEATHERINFO_URL="https://api.trace.moe/search"
        //private  const val API_ID="c987b71066495011f2e0f52d760875ee";
        private val CITYS = arrayOf(
            arrayOf("大阪","Osaka"),
            arrayOf("名古屋","Nagoya"),
            arrayOf("京都","Kyoto"),
            arrayOf("横浜","Yokohama"),
            arrayOf("神戸","Kobe"),
            arrayOf("鹿児島","Kagoshima"),
            arrayOf("札幌","Sapporo"),
        )
    }
    private  var _list:MutableList<MutableMap<String,String>> = mutableListOf()

    private fun createList():MutableList<MutableMap<String,String>> {
        var list: MutableList<MutableMap<String, String>> = mutableListOf()
        CITYS.forEach {
            var city = mutableMapOf("name" to it[0], "q" to it[1])
            list.add(city)
        }
        return list
    }
    @UiThread
    private  fun receiveWeatherInfo(urlFull: String){



        val handler=HandlerCompat.createAsync(mainLooper)
        val backgroundReceiver = WeatherInfoBackgroundReceiver(handler,urlFull)
        val executeService = Executors.newSingleThreadExecutor()
        executeService.submit(backgroundReceiver)


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.weather)
        _list=createList()
        val  lvCityList=findViewById<ListView>(R.id.lvCityList)
        val from= arrayOf("name")
        val to= intArrayOf(android.R.id.text1)


        val adapter=SimpleAdapter(this,_list,
        android.R.layout.simple_list_item_1,from,to)

        lvCityList.adapter=adapter
        lvCityList.onItemClickListener=ListItemClickListener()

        receiveWeatherInfo(WEATHERINFO_URL);
    }
    private inner class ListItemClickListener:AdapterView.OnItemClickListener{
        override fun onItemClick(parent : AdapterView<*>?, view : View?, position : Int, id: Long) {
//            val item=_list.get(position)
//            val q=item.get("q")
//            q?.let{
//                val urlFull="$WEATHERINFO_URL&q=$q&appid=$API_ID"
//                receiveWeatherInfo(urlFull)
//            }

        }


    }
    private inner class WeatherInfoBackgroundReceiver(handler: Handler,url:String):Runnable{
        private val _handler= handler
        private val _url=url

        private  fun is2String(stream: InputStream):String{
            val sb =StringBuilder()
            val reader =BufferedReader(InputStreamReader(stream,"UTF-8"))
            var line= reader.readLine()
            while (line !=null){
                sb.append(line)
                line= reader.readLine()
            }
            reader.close()
            return sb.toString()
        }
        @WorkerThread
        override fun run() {

            var result =""
            val url = URL(_url)

            val con=url.openConnection()as? HttpURLConnection
            con?.let {
                try {
                    val aa = resources.assets.open("pika.jpg")
                    val bm = BitmapFactory.decodeStream(aa)
                    val baos = ByteArrayOutputStream()
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val jpgarr: ByteArray = baos.toByteArray()

                    it.connectTimeout = 10000
                    it.readTimeout = 10000
                    it.requestMethod = "POST"
                    it.doOutput = true
                    it.setChunkedStreamingMode(0)
                    it.setRequestProperty("Content-type", "image/jpeg");
                    val outputStream = it.outputStream
                    outputStream.write(jpgarr)
                    outputStream.flush()
                    outputStream.close()
                    it.connect()
                    val stream = it.inputStream
                    result = is2String(stream)
                    stream.close()
                }catch (ex:SocketTimeoutException){
                    Log.w(DEBUG_TAG,"通信タイムアウト")
                }
                it.disconnect()
                val postExecutor = WeatherInfoPostExecutor(result)
                _handler.post(postExecutor)
            }
        }
        private inner class WeatherInfoPostExecutor(result:String):Runnable{
            private val  _result = result
            @UiThread
            override fun run() {
                val rootJSON=JSONObject(_result)
                println(rootJSON)
                val frameCount=rootJSON.getString("frameCount")
                val error=rootJSON.getString("error")
                val resultJSONArray= rootJSON.getJSONArray("result")
                val resultJSON=JSONObject(resultJSONArray[0].toString())
                val anilist= resultJSON.getString("anilist")
                val filename= resultJSON.getString("filename")
                val episode= resultJSON.getString("episode")
                val from= resultJSON.getString("from")
                val to= resultJSON.getString("to")
                val similarity= resultJSON.getString("similarity")
                val video= resultJSON.getString("video")
                val image= resultJSON.getString("image")
                println(frameCount)
                println(error)
                println(anilist)
                println(filename)
                println(episode)
                println(from)
                println(to)
                println(similarity)
                println(video)
                println(image)
            }

        }
    }

}