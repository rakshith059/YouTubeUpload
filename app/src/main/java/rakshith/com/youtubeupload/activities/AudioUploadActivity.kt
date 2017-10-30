package rakshith.com.youtubeupload.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_audio_upload.*
import rakshith.com.youtubeupload.R
import rakshith.com.youtubeupload.utils.NetworkVolleyRequest.Callback
import java.io.IOException
import android.text.TextUtils
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import rakshith.com.youtubeupload.models.AccessToken
import rakshith.com.youtubeupload.models.DataPart
import rakshith.com.youtubeupload.utils.*
import java.io.BufferedInputStream
import java.io.File
import java.net.URL
import java.util.*


class AudioUploadActivity : AppCompatActivity() {
    internal var audioSavePath: String? = null
    internal var mediaRecorder: MediaRecorder? = null
    internal var mediaPlayer: MediaPlayer? = null
    var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_upload)

        accessToken = Constants.getSharedPreference(this@AudioUploadActivity, Constants.ACCESS_TOKEN)

        LocalBroadcastManager.getInstance(this).registerReceiver(mCallbackReciver, IntentFilter(Constants.CALLBACK_INTENT_FILTER_RECIVER))

//        getAuthToken()
        fab_record.setOnClickListener {
            if (checkPermission()) {

                audioSavePath = Environment.getExternalStorageDirectory().absolutePath + "/" + resources.getString(R.string.app_name) + System.currentTimeMillis() + ".mp3"

                MediaRecorderReady()

                try {
                    mediaRecorder?.prepare()
                    mediaRecorder?.start()
                } catch (e: IllegalStateException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }

                fab_record.visibility = View.GONE
                fab_stop.visibility = View.VISIBLE
                fab_upload.visibility = View.GONE

                Toast.makeText(this, "Recording started", Toast.LENGTH_LONG).show()
            } else {
                requestPermission()
            }
        }

        fab_stop.setOnClickListener {
            mediaRecorder?.stop()

            fab_record.visibility = View.GONE
            fab_stop.visibility = View.GONE
            fab_upload.visibility = View.VISIBLE

            Toast.makeText(this, "Recording Completed", Toast.LENGTH_LONG).show()
        }

        fab_upload.setOnClickListener {
            fab_record.visibility = View.VISIBLE
            fab_stop.visibility = View.GONE
            fab_upload.visibility = View.GONE

            getAccessToken()

            mediaPlayer = MediaPlayer()

            try {
                mediaPlayer?.setDataSource(audioSavePath)
                mediaPlayer?.prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mediaPlayer?.start()
        }

//        buttonPlayLastRecordAudio.setOnClickListener {
//            buttonStop.isEnabled = false
//            buttonStart.isEnabled = false
//            buttonStopPlayingRecording.isEnabled = true
//
//            mediaPlayer = MediaPlayer()
//
//            try {
//                mediaPlayer!!.setDataSource(AudioSavePathInDevice)
//                mediaPlayer!!.prepare()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//
//            mediaPlayer!!.start()
//
//            Toast.makeText(this@MainActivity, "Recording Playing", Toast.LENGTH_LONG).show()
//        }

//        buttonStopPlayingRecording.setOnClickListener {
//            buttonStop.isEnabled = false
//            buttonStart.isEnabled = true
//            buttonStopPlayingRecording.isEnabled = false
//            buttonPlayLastRecordAudio.isEnabled = true
//
//            if (mediaPlayer != null) {
//
//                mediaPlayer!!.stop()
//                mediaPlayer!!.release()
//
//                MediaRecorderReady()
//
//            }
//        }
    }

    var mCallbackReciver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            var oAuthCode: String? = intent?.getStringExtra(Constants.OAUTH_CODE)

            if (!TextUtils.isEmpty(oAuthCode)) {
                getAuthToken(oAuthCode as String)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallbackReciver)
    }

    private fun getAccessToken() {
        if (TextUtils.isEmpty(accessToken)) {
            var intent: Intent = Intent(this, WebActivity::class.java)
            startActivity(intent)

            var oAuthCode = Constants.getSharedPreference(this@AudioUploadActivity, Constants.OAUTH_CODE)
            Log.d("Rakshith", "oAuthCode from redirectUri == " + oAuthCode)

            if (!TextUtils.isEmpty(oAuthCode)) {
                var handler: Handler = Handler()
                handler.run {
                    getAuthToken(oAuthCode)
                }
            }
        } else {
            uploadAudioToMixCloud()
        }
    }

    private fun uploadAudioToMixCloud() {

        if (!TextUtils.isEmpty(accessToken)) {

            var uploadAudioUrl = Constants.UPLOAD_AUDIO_URL + accessToken
            var uuid = UUID.randomUUID().toString()
            var boundary = "----------------------------" + uuid
            var fileName: String = "SampleUpload-" + System.currentTimeMillis()

            var file: File = File(audioSavePath)

            var params: HashMap<String, String> = HashMap<String, String>()
            params.put("mp3", file.toString())
            params.put("name", fileName)

            var imageUploadReq = MultipartRequest(uploadAudioUrl, object : Response.ErrorListener {
                override fun onErrorResponse(error: VolleyError?) {
                    Log.d("Rakshith", "response from multipart error ==> " + error?.message)
                }
            }, object : Response.Listener<String> {
                override fun onResponse(response: String?) {
                    Log.d("Rakshith", "response from multipart success ==> " + response?.toString())
                }
            }, file, params)

            imageUploadReq.retryPolicy = DefaultRetryPolicy(1000 * 60, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

            AppController.getInstance().addToRequestQueue(imageUploadReq)

//            RequestMultiPart(file, fileName, boundary, uploadAudioUrl, "File", params, object : ApiResponse<String> {
//                override fun onCompletion(result: String) {
//                    Log.d("Rakshith", "response from multipart success == " + result)
//                }
//            })
        }

//        if (!TextUtils.isEmpty(accessToken)) {
//            var uploadAudioUrl = Constants.UPLOAD_AUDIO_URL + accessToken
//
//            var uploadAudioRequest = object : VolleyMultipartRequest(NetworkVolleyRequest.RequestMethod.POST, uploadAudioUrl, object : Response.Listener<NetworkResponse> {
//                override fun onResponse(response: NetworkResponse?) {
//                    var successResponse = response?.data
//                    Log.d("Rakshith", "response from multipart success == " + successResponse)
//                }
//
//            }, object : Response.ErrorListener {
//                override fun onErrorResponse(error: VolleyError?) {
//                    val networkResponse = error?.networkResponse
//                    Log.d("Rakshith", "error response from multipart == " + error?.message)
//                }
//            }) {
//                override fun getParams(): Map<String, String> {
//                    val params = HashMap<String, String>()
//                    params.put("name", "sample upload" + System.currentTimeMillis())
//                    params.put("description", "Sample Audio uploaded at " + System.currentTimeMillis())
//                    return params
//                }
//
//                override fun getByteData(): HashMap<String, DataPart?> {
//                    val params = HashMap<String, DataPart?>()
//                    // file name could found file base or direct access from real path
//                    // for now just get bitmap data from ImageView
//                    val bitmapData = getByteArrayImage(audioSavePath)
//                    params.put("mp3", DataPart("sampleUpload", bitmapData))
//
//                    return params
//                }
//            }
//            Volley.newRequestQueue(this).add(uploadAudioRequest)
//        }
    }

//    private fun getByteArrayImage(url: String?): ByteArray? {
//        try {
//            val imageUrl = URL(url)
//            val ucon = imageUrl.openConnection()
//
//            val `is` = ucon.getInputStream()
//            val bis = BufferedInputStream(`is`)
//
//            val baf = ByteArrayBuffer(500)
//            var current = 0
//            while ({ current = bis.read(); current }() != -1) {
//                baf.append(current.toByte().toInt())
//            }
//
//            return baf.toByteArray()
//        } catch (e: Exception) {
//            Log.d("ImageManager", "Error: " + e.toString())
//        }
//
//        return null
//    }

    private val CLIENT_ID: String = "sqM2pz7u2VMPsKbyHe"
    private val REDIRECT_URI: String = "https://rakshith.com/mixcloud-uri"
    private val CLIENT_SECRET: String = "g2fXM2KcgUf9UdKVhVysYuuKRZCrC8Zm"

    private fun getAuthToken(oAuthCode: String) {
        var parameters: HashMap<String, String> = HashMap()
        var url: String = "https://www.mixcloud.com/oauth/access_token?client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URI + "&client_secret=" + CLIENT_SECRET + "&code=" + oAuthCode
        var networkRequest = NetworkVolleyRequest(NetworkVolleyRequest.RequestMethod.GET, url, String::class.java, parameters, HashMap<String, Any>(), object : Callback<Any> {
            override fun onSuccess(response: Any) {
                Log.d("Rakshith", "success response==" + response.toString())
                var gson: Gson = Gson()
                val uploadResponseJson = gson.fromJson(response as String, AccessToken::class.java)

                Log.d("Rakshith", "success response accessToken ==" + uploadResponseJson?.accessToken)
                Constants.setSharedPrefrence(this@AudioUploadActivity, Constants.ACCESS_TOKEN, uploadResponseJson?.accessToken)

                uploadAudioToMixCloud()
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                Log.d("Rakshith", "failure response errorMessage == " + errorMessage + " errorcode == " + errorCode)
            }
        }, NetworkVolleyRequest.ContentType.JSON)
        networkRequest.execute()
    }

    fun MediaRecorderReady() {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder?.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
        mediaRecorder?.setOutputFile(audioSavePath)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO), RequestPermissionCode)
    }

    override fun onResume() {
        super.onResume()
//        getAccessToken()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RequestPermissionCode -> if (grantResults.size > 0) {

                val StoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val RecordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED

                if (StoragePermission && RecordPermission) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()

                }
            }
        }
    }

    fun checkPermission(): Boolean {

        val result = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val RequestPermissionCode = 1
    }
}