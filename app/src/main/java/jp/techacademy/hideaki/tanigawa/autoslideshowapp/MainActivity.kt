package jp.techacademy.hideaki.tanigawa.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import jp.techacademy.hideaki.tanigawa.autoslideshowapp.databinding.ActivityMainBinding
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val PERMISSIONS_REQUEST_CODE = 100

    val imageList = ArrayList<String>()

    private var timer: Timer? = null

    // 配列のINDEX値を入れる変数
    private var imgNo = 0
    // 再生・停止を入れ替えるための変数
    var startEnd:Int = 0
    private var handler = Handler(Looper.getMainLooper())

    // APIレベルによって許可が必要なパーミッションを切り替える
    private val readImagesPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preference.edit()

        // パーミッションの許可状態を確認する
        if (checkSelfPermission(readImagesPermission) == PackageManager.PERMISSION_GRANTED) {
            // 許可されている
            getContentsInfo()
        } else {
            // 許可されていないので許可ダイアログを表示する
            requestPermissions(
                arrayOf(readImagesPermission),
                PERMISSIONS_REQUEST_CODE
            )
        }

        /**
         * 戻るボタンが押された時に発火する
         */
        binding.pauseButton.setOnClickListener{
            // URIの配列のINDEX値を受け取る
            imgNo = preference.getInt("Number", 0)
            if(imgNo != 0){
                imgNo = imgNo - 1

            }else{
                imgNo = imageList.size - 1
            }
            // URIの配列のINDEX値を保存する
            editor.putInt("Number", imgNo)
            editor.apply()
            binding.imageView.setImageURI(Uri.parse(imageList[imgNo]))
        }

        /**
         * 進むボタンが押された時に発火する
         */
        binding.startButton.setOnClickListener{
            // URIの配列のINDEX値を受け取る
            imgNo = preference.getInt("Number", 0)
            if(imgNo != imageList.size - 1){
                imgNo = imgNo + 1
            }else{
                imgNo = 0
            }
            // URIの配列のINDEX値を保存する
            editor.putInt("Number", imgNo)
            editor.apply()
            binding.imageView.setImageURI(Uri.parse(imageList[imgNo]))
        }

        /**
         * 再生・停止ボタンを押された時に発火する
         */
        binding.resetButton.setOnClickListener{
            if(startEnd == 0){
                // ボタンのテキストを停止にする
                binding.resetButton.text = "停止"
                startEnd = 1
                // クリックを無効にする
                binding.startButton.isClickable = false
                binding.pauseButton.isClickable = false

                // スライドショー処理
                if (timer == null) {
                    timer = Timer()
                    timer!!.schedule(object : TimerTask() {
                        override fun run() {
                            handler.post {
                                // URIの配列のINDEX値を受け取る
                                imgNo = preference.getInt("Number", 0)
                                if(imgNo != imageList.size - 1){
                                    imgNo = imgNo + 1
                                }else{
                                    imgNo = 0
                                }
                                // URIの配列のINDEX値を保存する
                                editor.putInt("Number", imgNo)
                                editor.apply()
                                binding.imageView.setImageURI(Uri.parse(imageList[imgNo]))
                            }
                        }
                    }, 2000, 2000) // 最初に始動させるまで2000ミリ秒、ループの間隔を2000ミリ秒 に設定
                }
            }else{
                // ボタンのテキストを再生にする
                binding.resetButton.text = "再生"
                // クリックを有効にする
                binding.startButton.isClickable = true
                binding.pauseButton.isClickable = true
                startEnd = 0

                // スライドショーの停止処理
                if (timer != null){
                    timer!!.cancel()
                    timer = null
                }
            }
        }
    }
    
    /**
     * パーミッションが許可されているかを判別する処理
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                }
        }
    }

    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )

        if (cursor!!.moveToFirst()) {
            do {
                // indexからIDを取得し、そのIDから画像のURIを取得する
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                Log.d("ANDROID", "URI : $imageUri")
                imageList.add(imageUri.toString())
            } while (cursor.moveToNext())
        }
        cursor.close()

        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        if(preference.getInt("Number", 0) == 0){
            binding.imageView.setImageURI(Uri.parse(imageList[0]))
        }else{
            binding.imageView.setImageURI(Uri.parse(imageList[preference.getInt("Number",0)]))
        }
        for(i in imageList.indices){
            Log.d("REALLY",imageList[i])
        }
    }
}