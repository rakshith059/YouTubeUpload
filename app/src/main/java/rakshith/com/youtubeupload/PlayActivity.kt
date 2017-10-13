package rakshith.com.youtubeupload

import android.app.Activity
import android.app.FragmentTransaction
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayer.OnFullscreenListener
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener
import com.google.android.youtube.player.YouTubePlayerFragment
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import rakshith.com.youtubeupload.utils.Auth
import rakshith.com.youtubeupload.utils.VideoData


class PlayActivity : Activity(), PlayerStateChangeListener, OnFullscreenListener {
    internal val transport = AndroidHttp.newCompatibleTransport()
    internal val jsonFactory: JsonFactory = GsonFactory()
    internal var credential: GoogleAccountCredential? = null
    private var mYouTubePlayer: YouTubePlayer? = null
    private var mIsFullScreen = false

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()
    }

    fun directLite(view: View) {
        this.setResult(RESULT_OK, intent)
        finish()
    }

    fun panToVideo(youtubeId: String) {
        popPlayerFromBackStack()
        val playerFragment = YouTubePlayerFragment
                .newInstance()
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.detail_container, playerFragment,
                        YOUTUBE_FRAGMENT_TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null).commit()
        playerFragment.initialize(Auth.KEY,
                object : YouTubePlayer.OnInitializedListener {
                    override fun onInitializationSuccess(
                            provider: YouTubePlayer.Provider,
                            youTubePlayer: YouTubePlayer, b: Boolean) {
                        youTubePlayer.loadVideo(youtubeId)
                        mYouTubePlayer = youTubePlayer
                        youTubePlayer
                                .setPlayerStateChangeListener(this@PlayActivity)
                        youTubePlayer
                                .setOnFullscreenListener(this@PlayActivity)
                    }

                    override fun onInitializationFailure(
                            provider: YouTubePlayer.Provider,
                            result: YouTubeInitializationResult) {
                        showErrorToast(result.toString())
                    }
                })
    }

    fun popPlayerFromBackStack(): Boolean {
        if (mIsFullScreen) {
            mYouTubePlayer!!.setFullscreen(false)
            return false
        }
        if (getFragmentManager().findFragmentByTag(YOUTUBE_FRAGMENT_TAG) != null) {
            getFragmentManager().popBackStack()
            return false
        }
        return true
    }

    override fun onAdStarted() {}

    override fun onError(errorReason: YouTubePlayer.ErrorReason) {
        showErrorToast(errorReason.toString())
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT)
                .show()
    }

    override fun onLoaded(arg0: String) {}

    override fun onLoading() {}

    override fun onVideoEnded() {
        // popPlayerFromBackStack();
    }

    override fun onVideoStarted() {}

    override fun onFullscreen(fullScreen: Boolean) {
        mIsFullScreen = fullScreen
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        intent = getIntent()
        val submitButton = findViewById<Button>(R.id.submit_button)
        if (Intent.ACTION_VIEW == intent!!.action) {
            submitButton.setVisibility(View.GONE)
            setTitle(R.string.playing_uploaded_video)
        }
        val youtubeId = intent!!.getStringExtra(MainActivity.YOUTUBE_ID)
        panToVideo(youtubeId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play, menu)
        return true
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

    override fun onBackPressed() {
        super.onBackPressed()
//        NavUtils.navigateUpFromSameTask(this)
        finish()
    }

    interface Callbacks {

        fun onVideoSelected(video: VideoData)

        fun onResume()

    }

    companion object {

        private val YOUTUBE_FRAGMENT_TAG = "youtube"
    }
}

