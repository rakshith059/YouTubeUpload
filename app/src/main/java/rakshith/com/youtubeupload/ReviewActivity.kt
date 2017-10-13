package rakshith.com.youtubeupload

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_review.*
import android.support.v4.app.NavUtils
import android.widget.Toast
import android.content.Intent
import android.support.v4.app.ActivityCompat.invalidateOptionsMenu
import android.preference.PreferenceManager
import android.content.SharedPreferences
import com.google.android.youtube.player.internal.e
import android.support.v4.media.session.MediaControllerCompat.setMediaController
import android.widget.VideoView
import android.content.Intent.getIntent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.MediaController


class ReviewActivity : Activity() {
    internal var mVideoView: VideoView? = null
    internal var mc: MediaController? = null
    private var mChosenAccountName: String? = null
    private var mFileUri: Uri? = null

    private val REQUEST_CODE_ASK_PERMISSIONS: Int = 1002

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        getActionBar().setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_review)
        val uploadButton = findViewById<Button>(R.id.upload_button)
        val intent = getIntent()
        if (Intent.ACTION_VIEW == intent.action) {
            uploadButton.setVisibility(View.GONE)
            setTitle(R.string.playing_the_video_in_upload_progress)
        }
        mFileUri = intent.data
        loadAccount()

        reviewVideo(mFileUri!!)

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            var hasGetAccountPermission: Int = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.GET_ACCOUNTS)
            if (hasGetAccountPermission != PackageManager.PERMISSION_GRANTED)
                requestPermissions(Array(3, { android.Manifest.permission.GET_ACCOUNTS }),
                        REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                Log.d("Rakshith", "GET_ACCOUNTS granted == ")
            } else {
                // Permission Denied
                Log.d("Rakshith", "GET_ACCOUNTS Denied")
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private fun reviewVideo(mFileUri: Uri) {
        try {
            mVideoView = findViewById<VideoView>(R.id.videoView)
            mc = MediaController(this)
            (mVideoView as VideoView?)?.setMediaController(mc)
            (mVideoView as VideoView?)?.setVideoURI(mFileUri)
            mc?.show()
            (mVideoView as VideoView?)?.start()
        } catch (e: Exception) {
            Log.e(this.getLocalClassName(), e.toString())
        }
    }

    private fun loadAccount() {
        val sp = PreferenceManager
                .getDefaultSharedPreferences(this)
        mChosenAccountName = sp.getString(MainActivity.ACCOUNT_KEY, "null")
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.review, menu)
        return true
    }

    fun uploadVideo(view: View) {
        if (mChosenAccountName == null) {
            return
        }
        // if a video is picked or recorded.
        if (mFileUri != null) {
            val uploadIntent = Intent(this, UploadService::class.java)
            uploadIntent.setData(mFileUri)
            uploadIntent.putExtra(MainActivity.ACCOUNT_KEY, mChosenAccountName)
            Log.d("Rakshith", "Review Activity : account name == " + mChosenAccountName)
            startService(uploadIntent)
            Toast.makeText(this, R.string.youtube_upload_started,
                    Toast.LENGTH_LONG).show()
            // Go back to MainActivity after upload
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
        // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
