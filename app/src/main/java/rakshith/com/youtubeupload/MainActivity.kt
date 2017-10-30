package rakshith.com.youtubeupload

import android.accounts.AccountManager
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.content.LocalBroadcastManager
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import io.vrinda.kotlinpermissions.PermissionsActivity
import rakshith.com.youtubeupload.fragments.UploadListFragment
import rakshith.com.youtubeupload.utils.*
import java.io.IOException
import java.util.*

class MainActivity : PermissionsActivity(), UploadListFragment.CallBacks {
    internal val transport = AndroidHttp.newCompatibleTransport()
    internal val jsonFactory: JsonFactory = GsonFactory()
    internal var credential: GoogleAccountCredential? = null
    private var mImageLoader: ImageLoader? = null
    private var mChosenAccountName: String? = null
    private var mFileURI: Uri? = null
    private var mVideoData: VideoData? = null
    private var broadcastReceiver: UploadBroadcastReceiver? = null
    private var mUploadsListFragment: UploadListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)

        mUploadsListFragment = UploadListFragment(getApplicationContext())

        // Check to see if the proper keys and playlist IDs have been set up
        if (!isCorrectlyConfigured) {
            setContentView(R.layout.developer_setup_required)
            showMissingConfigurations()
        } else {
            setContentView(R.layout.activity_main)

            ensureLoader()
            credential = GoogleAccountCredential.usingOAuth2(
                    applicationContext, Arrays.asList<String>(*Auth.SCOPES))

            // set exponential backoff policy
            credential?.backOff = ExponentialBackOff()

            if (savedInstanceState != null) {
                mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY, "null")
            } else {
//                loadAccount()
                chooseAccount()
            }

            credential?.selectedAccountName = mChosenAccountName

//            mUploadsListFragment = getFragmentManager()
//                    .findFragmentById(R.id.list_fragment) as UploadListFragment

        }
    }

    /**
     * This method checks various internal states to figure out at startup time
     * whether certain elements have been configured correctly by the developer.
     * Checks that:
     *
     *  * the API key has been configured
     *  * the playlist ID has been configured
     *

     * @return true if the application is correctly configured for use, false if
     * * not
     */
    private // This isn't going to internationalize well, but we only really need
            // this for the sample app.
            // Real applications will remove this section of code and ensure that
            // all of these values are configured.
    val isCorrectlyConfigured: Boolean
        get() {
            if (Auth.KEY.startsWith("Replace")) {
                return false
            }
            if (Constants.UPLOAD_PLAYLIST.startsWith("Replace")) {
                return false
            }
            return true
        }

    /**
     * This method renders the ListView explaining what the configurations the
     * developer of this application has to complete. Typically, these are
     * static variables defined in [Auth] and [Constants].
     */
    private fun showMissingConfigurations() {
        val missingConfigs = ArrayList<MissingConfig>()

        // Make sure an API key is registered
        if (Auth.KEY.startsWith("Replace")) {
            missingConfigs
                    .add(MissingConfig(
                            "API key not configured",
                            "KEY constant in Auth.java must be configured with your Simple API key from the Google API Console"))
        }

        // Make sure a playlist ID is registered
        if (Constants.UPLOAD_PLAYLIST.startsWith("Replace")) {
            missingConfigs
                    .add(MissingConfig(
                            "Playlist ID not configured",
                            "UPLOAD_PLAYLIST constant in Constants.java must be configured with a Playlist ID to submit to. (The playlist ID typically has a prexix of PL)"))
        }

        // Renders a simple_list_item_2, which consists of a title and a body
        // element
        val adapter = object : ArrayAdapter<MissingConfig>(this,
                android.R.layout.simple_list_item_2, missingConfigs) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row: View
                if (convertView == null) {
                    val inflater = getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    row = inflater.inflate(android.R.layout.simple_list_item_2, null)
                } else {
                    row = convertView
                }

                val titleView = row.findViewById<TextView>(android.R.id.text1)
                val bodyView = row.findViewById<TextView>(android.R.id.text2)
                val config = getItem(position)
                titleView.text = config!!.title
                bodyView.text = config.body
                return row
            }
        }

        // Wire the data adapter up to the view
        val missingConfigList = findViewById<ListView>(R.id.missing_config_list)
        missingConfigList.setAdapter(adapter)
    }

    protected override fun onResume() {
        super.onResume()
        if (broadcastReceiver == null)
            broadcastReceiver = UploadBroadcastReceiver()
        val intentFilter = IntentFilter(
                REQUEST_AUTHORIZATION_INTENT)
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, intentFilter)
    }

    private fun ensureLoader() {
        if (mImageLoader == null) {
            // Get the ImageLoader through your singleton class.
            mImageLoader = NetworkSingleton.getInstance(this).imageLoader
        }
    }

    private fun loadAccount() {
        val sp = PreferenceManager
                .getDefaultSharedPreferences(this)
        mChosenAccountName = sp.getString(ACCOUNT_KEY, "null")
        invalidateOptionsMenu()
    }

    private fun saveAccount() {
        val sp = PreferenceManager
                .getDefaultSharedPreferences(this)
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).commit()
    }

    private fun loadData() {
        if (mChosenAccountName == null) {
            return
        }

        loadUploadedVideos()
    }

    protected override fun onPause() {
        super.onPause()
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(
                    broadcastReceiver)
        }
        if (isFinishing()) {
            // mHandler.removeCallbacksAndMessages(null);
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> loadData()
            R.id.menu_accounts -> {
                chooseAccount()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GMS_ERROR_DIALOG -> {
            }
            RESULT_PICK_IMAGE_CROP -> if (resultCode == RESULT_OK) {
                mFileURI = data!!.data
                if (mFileURI != null) {
                    val intent = Intent(this, ReviewActivity::class.java)
                    intent.setData(mFileURI)
                    startActivity(intent)
                }
            }

            RESULT_VIDEO_CAP -> if (resultCode == RESULT_OK) {
                mFileURI = data!!.data
                if (mFileURI != null) {
                    val intent = Intent(this, ReviewActivity::class.java)
                    intent.setData(mFileURI)
                    startActivity(intent)
                }
            }
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode == Activity.RESULT_OK) {
                haveGooglePlayServices()
            } else {
                checkGooglePlayServicesAvailable()
            }
            REQUEST_AUTHORIZATION -> if (resultCode != Activity.RESULT_OK) {
                chooseAccount()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null
                    && data.extras != null) {
                val accountName = data.extras?.getString(
                        AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    mChosenAccountName = accountName
                    credential?.selectedAccountName = accountName
                    saveAccount()
                }
            }
            REQUEST_DIRECT_TAG -> if (resultCode == Activity.RESULT_OK && data != null
                    && data.extras != null) {
                val youtubeId = data.getStringExtra(YOUTUBE_ID)
                if (youtubeId == mVideoData!!.youTubeId) {
                    directTag(mVideoData as VideoData)
                }
            }
        }
    }

    private fun directTag(video: VideoData) {
        val updateVideo = Video()
        val snippet = video
                .addTags(Arrays.asList(
                        Constants.DEFAULT_KEYWORD,
                        Upload.generateKeywordFromPlaylistId(Constants.UPLOAD_PLAYLIST)))
        updateVideo.setSnippet(snippet)
        updateVideo.setId(video.youTubeId)

        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg voids: Void): Void? {

                val youtube = YouTube.Builder(transport, jsonFactory,
                        credential).setApplicationName(Constants.APP_NAME)
                        .build()
                try {
                    youtube.videos().update("snippet", updateVideo).execute()
                } catch (e: UserRecoverableAuthIOException) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                } catch (e: IOException) {
                    Log.e(TAG, e.message)
                }

                return null
            }

        }.execute(null as Void?)
        Toast.makeText(this,
                R.string.video_submitted_to_ytdl, Toast.LENGTH_LONG)
                .show()
    }

    protected override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ACCOUNT_KEY, mChosenAccountName)
    }

    private fun loadUploadedVideos() {
        if (mChosenAccountName == null) {
            return
        }

        setProgressBarIndeterminateVisibility(true)
        object : AsyncTask<Void, Void, List<VideoData>>() {
            override fun doInBackground(vararg voids: Void): List<VideoData>? {

                val youtube = YouTube.Builder(transport, jsonFactory,
                        credential).setApplicationName(Constants.APP_NAME)
                        .build()

                try {
                    /*
                     * Now that the user is authenticated, the app makes a
					 * channels list request to get the authenticated user's
					 * channel. Returned with that data is the playlist id for
					 * the uploaded videos.
					 * https://developers.google.com/youtube
					 * /v3/docs/channels/list
					 */
                    val clr = youtube.channels()
                            .list("contentDetails").setMine(true).execute()

                    // Get the user's uploads playlist's id from channel list
                    // response
                    val uploadsPlaylistId = clr.items[0]
                            .contentDetails.relatedPlaylists
                            .uploads

                    val videos = ArrayList<VideoData>()

                    // Get videos from user's upload playlist with a playlist
                    // items list request
                    val pilr = youtube.playlistItems()
                            .list("id,contentDetails")
                            .setPlaylistId(uploadsPlaylistId)
                            .setMaxResults(20L).execute()
                    val videoIds = ArrayList<String>()

                    // Iterate over playlist item list response to get uploaded
                    // videos' ids.
                    for (item in pilr.items) {
                        videoIds.add(item.contentDetails.videoId)
                    }

                    // Get details of uploaded videos with a videos list
                    // request.
                    val vlr = youtube.videos()
                            .list("id,snippet,status")
                            .setId(TextUtils.join(",", videoIds)).execute()

                    // Add only the public videos to the local videos list.
                    for (video in vlr.items) {
                        if ("public" == video.status
                                .privacyStatus) {
                            val videoData = VideoData()
                            videoData.video = video
                            videos.add(videoData)
                        }
                    }

                    // Sort videos by title
                    Collections.sort(videos, object : Comparator<VideoData> {
                        override fun compare(videoData: VideoData,
                                             videoData2: VideoData): Int {
                            return videoData.title.compareTo(
                                    videoData2.title)
                        }
                    })

                    return videos as List<VideoData>

                } catch (availabilityException: GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(availabilityException
                            .connectionStatusCode)
                } catch (userRecoverableException: UserRecoverableAuthIOException) {
                    startActivityForResult(
                            userRecoverableException.intent,
                            REQUEST_AUTHORIZATION)
                } catch (e: IOException) {
                    Utils.logAndShow(this@MainActivity, Constants.APP_NAME, e)
                }

                return null
            }

            override fun onPostExecute(videos: List<VideoData>?) {
                setProgressBarIndeterminateVisibility(false)

                if (videos == null) {
                    return
                }

                mUploadsListFragment?.setVideos(videos)
            }

        }.execute(null as Void?)
    }

    override fun onBackPressed() {
        // if (mDirectFragment.popPlayerFromBackStack()) {
        // super.onBackPressed();
        // }
    }

    override fun onGetImageLoader(): ImageLoader? {
        ensureLoader()
        return mImageLoader
    }

    override fun onVideoSelected(video: VideoData) {
        mVideoData = video
        val intent = Intent(this, PlayActivity::class.java)
        intent.putExtra(YOUTUBE_ID, video.youTubeId)
        startActivityForResult(intent, REQUEST_DIRECT_TAG)
    }

    override fun onConnected(connectedAccountName: String) {
        // Make API requests only when the user has successfully signed in.
        loadData()
    }

    fun pickFile(view: View) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, RESULT_PICK_IMAGE_CROP)
    }

    fun recordVideo(view: View) {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        // Workaround for Nexus 7 Android 4.3 Intent Returning Null problem
        // create a file to save the video in specific folder (this works for
        // video only)
        // mFileURI = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        // intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileURI);

        // set the video image quality to high
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)

        // start the Video Capture Intent
        startActivityForResult(intent, RESULT_VIDEO_CAP)
    }

    fun showGooglePlayServicesAvailabilityErrorDialog(
            connectionStatusCode: Int) {
        runOnUiThread(Runnable {
            val dialog = GooglePlayServicesUtil.getErrorDialog(
                    connectionStatusCode, this@MainActivity,
                    REQUEST_GOOGLE_PLAY_SERVICES)
            dialog.show()
        })
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private fun checkGooglePlayServicesAvailable(): Boolean {
        val connectionStatusCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this)
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
            return false
        }
        return true
    }

    private fun haveGooglePlayServices() {
        // check if there is already an account selected
        if (credential?.selectedAccountName == null) {
            // ask user to choose account
            chooseAccount()
        }
    }

    private fun chooseAccount() {
        startActivityForResult(credential?.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER)
    }

    /**
     * Private class representing a missing configuration and what the developer
     * can do to fix the issue.
     */
    private inner class MissingConfig(val title: String, val body: String)

    // public Uri getOutputMediaFile(int type)
    // {
    // // To be safe, you should check that the SDCard is mounted
    // if(Environment.getExternalStorageState() != null) {
    // // this works for Android 2.2 and above
    // File mediaStorageDir = new
    // File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
    // "SMW_VIDEO");
    //
    // // This location works best if you want the created images to be shared
    // // between applications and persist after your app has been uninstalled.
    //
    // // Create the storage directory if it does not exist
    // if (! mediaStorageDir.exists()) {
    // if (! mediaStorageDir.mkdirs()) {
    // Log.d(TAG, "failed to create directory");
    // return null;
    // }
    // }
    //
    // // Create a media file name
    // String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
    // Locale.getDefault()).format(new Date());
    // File mediaFile;
    // if(type == MEDIA_TYPE_VIDEO) {
    // mediaFile = new File(mediaStorageDir.getPath() + File.separator +
    // "VID_"+ timeStamp + ".mp4");
    // } else {
    // return null;
    // }
    //
    // return Uri.fromFile(mediaFile);
    // }
    //
    // return null;
    // }

    private inner class UploadBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == REQUEST_AUTHORIZATION_INTENT) {
                Log.d(TAG, "Request auth received - executing the intent")
                val toRun = intent
                        .getParcelableExtra<Intent>(REQUEST_AUTHORIZATION_INTENT_PARAM)
                startActivityForResult(toRun, REQUEST_AUTHORIZATION)
            }
        }
    }

    companion object {
        // private static final int MEDIA_TYPE_VIDEO = 7;
        val ACCOUNT_KEY = "accountName"
        val MESSAGE_KEY = "message"
        val YOUTUBE_ID = "youtubeId"
        val YOUTUBE_WATCH_URL_PREFIX = "http://www.youtube.com/watch?v="
        internal val REQUEST_AUTHORIZATION_INTENT = "com.google.example.yt.RequestAuth"
        internal val REQUEST_AUTHORIZATION_INTENT_PARAM = "com.google.example.yt.RequestAuth.param"
        private val REQUEST_GOOGLE_PLAY_SERVICES = 0
        private val REQUEST_GMS_ERROR_DIALOG = 1
        private val REQUEST_ACCOUNT_PICKER = 2
        private val REQUEST_AUTHORIZATION = 3
        private val RESULT_PICK_IMAGE_CROP = 4
        private val RESULT_VIDEO_CAP = 5
        private val REQUEST_DIRECT_TAG = 6
        private val TAG = "MainActivity"
    }
}

