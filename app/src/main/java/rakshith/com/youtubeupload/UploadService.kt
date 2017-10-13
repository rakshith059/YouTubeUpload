package rakshith.com.youtubeupload

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.ResourceId
import com.google.gson.Gson
import rakshith.com.youtubeupload.models.AddVideoToPlaylist
import rakshith.com.youtubeupload.utils.Auth
import rakshith.com.youtubeupload.utils.Constants
import rakshith.com.youtubeupload.utils.NetworkVolleyRequest
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap


/**
 * Created YouTubeUpload by rakshith on 10/5/17.
 */


class UploadService : IntentService("UploadService") {
    internal val transport = AndroidHttp.newCompatibleTransport()
    internal val jsonFactory: JsonFactory = GsonFactory()
    internal var credential: GoogleAccountCredential? = null
    /**
     * tracks the number of upload attempts
     */
    private var mUploadAttemptCount: Int = 0
    private val REQUEST_CODE_ASK_PERMISSIONS: Int = 1002

    override fun onHandleIntent(intent: Intent?) {
        val fileUri = intent?.data
        val chosenAccountName = intent?.getStringExtra(MainActivity.ACCOUNT_KEY)
        Log.d("Rakshith", "account name " + chosenAccountName)

        credential = GoogleAccountCredential.usingOAuth2(applicationContext, Arrays.asList<String>(*Auth.SCOPES))
        credential?.selectedAccountName = chosenAccountName
        credential?.backOff = ExponentialBackOff()

        val appName = resources.getString(R.string.app_name)
        val youtube = YouTube.Builder(transport, jsonFactory, credential).setApplicationName(
                appName).build()


        try {
            if (fileUri != null) {
                tryUploadAndShowSelectableNotification(fileUri, youtube)
            }
        } catch (e: InterruptedException) {
            // ignore
        }

    }

    @Throws(InterruptedException::class)
    private fun tryUploadAndShowSelectableNotification(fileUri: Uri, youtube: YouTube) {
        while (true) {
            Log.i(TAG, String.format("Uploading [%s] to YouTube", fileUri.toString()))
            val videoId = tryUpload(fileUri, youtube)
            if (videoId != null) {
                Log.i(TAG, String.format("Uploaded video with ID: %s", videoId))
                tryShowSelectableNotification(videoId, youtube)
                return
            } else {
                Log.e(TAG, String.format("Failed to upload %s", fileUri.toString()))
                if (mUploadAttemptCount++ < MAX_RETRY) {
                    Log.i(TAG, String.format("Will retry to upload the video ([%d] out of [%d] reattempts)",
                            mUploadAttemptCount, MAX_RETRY))
                    zzz(UPLOAD_REATTEMPT_DELAY_SEC * 1000)
                } else {
                    Log.e(TAG, String.format("Giving up on trying to upload %s after %d attempts",
                            fileUri.toString(), mUploadAttemptCount))

                    ResumableUpload.showLocalNotification(applicationContext, resources.getString(R.string.video_not_uploaded))
                    return
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    private fun tryShowSelectableNotification(videoId: String, youtube: YouTube) {
        mStartTime = System.currentTimeMillis()
        var processed = false
        while (!processed) {
            processed = ResumableUpload.checkIfProcessed(videoId, youtube)
            if (!processed) {
                // wait a while
                Log.d(TAG, String.format("Video [%s] is not processed yet, will retry after [%d] seconds",
                        videoId, PROCESSING_POLL_INTERVAL_SEC))
                if (!timeoutExpired(mStartTime, PROCESSING_TIMEOUT_SEC)) {
                    zzz(PROCESSING_POLL_INTERVAL_SEC * 1000)
                } else {
                    Log.d(TAG, String.format("Bailing out polling for processing status after [%d] seconds",
                            PROCESSING_TIMEOUT_SEC))
                    ResumableUpload.showLocalNotification(applicationContext, resources.getString(R.string.uploaded_video_not_proccessed))
                    return
                }
            } else {
                ResumableUpload.showSelectableNotification(videoId, applicationContext)

                addVideoToPlaylist(videoId, youtube)
                return
            }
        }
    }

    private fun addVideoToPlaylist(videoId: String, youtube: YouTube) {
        try {
            val youTubePosition: Long = 0

            var parameters: HashMap<String, String> = HashMap()
            parameters.put("part", "snippet")
            parameters.put("onBehalfOfContentOwner", "")

            var playlistItem = PlaylistItem()
            var snippet = PlaylistItemSnippet()
            snippet.set("playlistId", Constants.UPLOAD_PLAYLIST)
            snippet.set("position", youTubePosition)
            var resourceId: ResourceId = ResourceId()
            resourceId.set("kind", "youtube#video")
            resourceId.set("videoId", videoId)

            snippet.setResourceId(resourceId)
            playlistItem.setSnippet(snippet)
            var context: Context? = null


            var playlistItemsInsertRequest = youtube.PlaylistItems().insert(parameters?.get("part")?.toString(), playlistItem)
            var playListItemResponse: PlaylistItem = playlistItemsInsertRequest.execute()

//            var networkVolleyRequest = NetworkVolleyRequest(NetworkVolleyRequest.RequestMethod.POST, Constants.ADD_VIDEO_PLAYLIST,
//                    String::class.java, parameters, playlistItem, object : NetworkVolleyRequest.Callback<Any> {
//                override fun onSuccess(response: Any) {
//
//                    var gson: Gson = Gson()
//                    val uploadResponseJson = gson.fromJson(response as String, AddVideoToPlaylist::class.java)
//
//
//                    var videoId = uploadResponseJson.youtubeSnippet?.youtubeSnippetResorceId?.youTubeSnippetResourceIdvideoId
//                    Log.d("Rakshith", "playlist added successfully videoId == " + videoId)
//
//                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//                    /* {
//                         "kind": "youtube#playlistItem",
//                         "etag": "\"cbz3lIQ2N25AfwNr-BdxUVxJ_QY/KsYxXMBAK930RiXY7nS3gFhD3MU\"",
//                         "id": "UExzUmJ2a3hRNUxzWDFyS2pWa25ULW1WUEVQOUxvTzZ6Ty4wOTA3OTZBNzVEMTUzOTMy",
//                         "snippet": {
//                         "publishedAt": "2017-10-12T12:27:21.000Z",
//                         "channelId": "UCMzNHuwfiBcPF52j80hH_3A",
//                         "title": "Test Upload via Java on Thu Oct 12 16:32:17 GMT+05:30 2017",
//                         "description": "Video uploaded via YouTube Data API V3 using the Java library on Thu Oct 12 16:32:17 GMT+05:30 2017",
//                         "thumbnails": {
//                         "default": {
//                         "url": "https://i.ytimg.com/vi/SCJJNcVpNXw/default.jpg",
//                         "width": 120,
//                         "height": 90
//                     },
//                         "medium": {
//                         "url": "https://i.ytimg.com/vi/SCJJNcVpNXw/mqdefault.jpg",
//                         "width": 320,
//                         "height": 180
//                     },
//                         "high": {
//                         "url": "https://i.ytimg.com/vi/SCJJNcVpNXw/hqdefault.jpg",
//                         "width": 480,
//                         "height": 360
//                     },
//                         "standard": {
//                         "url": "https://i.ytimg.com/vi/SCJJNcVpNXw/sddefault.jpg",
//                         "width": 640,
//                         "height": 480
//                     },
//                         "maxres": {
//                         "url": "https://i.ytimg.com/vi/SCJJNcVpNXw/maxresdefault.jpg",
//                         "width": 1280,
//                         "height": 720
//                     }
//                     },
//                         "channelTitle": "Rakshith Shankar",
//                         "playlistId": "PLsRbvkxQ5LsX1rKjVknT-mVPEP9LoO6zO",
//                         "resourceId": {
//                         "kind": "youtube#video",
//                         "videoId": "SCJJNcVpNXw"
//                     }
//                     }
//                     }*/
//
//                }
//
//                override fun onError(errorCode: Int, errorMessage: String) {
//                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//
//                    /* "error": {
//                         "errors": [
//                         {
//                             "domain": "youtube.playlistItem",
//                             "reason": "videoNotFound",
//                             "message": "Video not found."
//                         }
//                         ],
//                         "code": 404,
//                         "message": "Video not found."
//                     }
//                 }*/
//                }
//            }, NetworkVolleyRequest.ContentType.JSON)
//
//            networkVolleyRequest.execute()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun tryUpload(mFileUri: Uri, youtube: YouTube): String? {
        val fileSize: Long
        var fileInputStream: InputStream? = null
        var videoId: String? = null
        try {

            fileSize = contentResolver.openFileDescriptor(mFileUri, "r")!!.statSize
            fileInputStream = contentResolver.openInputStream(mFileUri)
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(mFileUri, proj, null, null, null)
            val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            Log.d("Rakshith", "column_index == " + column_index)
            cursor?.moveToFirst()

            if (cursor != null && column_index != null && cursor?.getString(column_index) != null)
                videoId = ResumableUpload.upload(youtube, fileInputStream, fileSize, mFileUri, cursor?.getString(column_index), applicationContext, credential?.selectedAccountName)

        } catch (e: FileNotFoundException) {
            Log.e(applicationContext.toString(), e.message)
        } finally {
            try {
                fileInputStream?.close()
            } catch (e: IOException) {
                // ignore
            }

        }
        return videoId
    }

    companion object {

        /**
         * defines how long we'll wait for a video to finish processing
         */
        private val PROCESSING_TIMEOUT_SEC = 60 * 20 // 20 minutes

        /**
         * controls how often to poll for video processing status
         */
        private val PROCESSING_POLL_INTERVAL_SEC = 60
        /**
         * how long to wait before re-trying the upload
         */
        private val UPLOAD_REATTEMPT_DELAY_SEC = 60
        /**
         * max number of retry attempts
         */
        private val MAX_RETRY = 3
        private val TAG = "UploadService"
        /**
         * processing start time
         */
        private var mStartTime: Long = 0

        @Throws(InterruptedException::class)
        private fun zzz(duration: Int) {
            Log.d(TAG, String.format("Sleeping for [%d] ms ...", duration))
            Thread.sleep(duration.toLong())
            Log.d(TAG, String.format("Sleeping for [%d] ms ... done", duration))
        }

        private fun timeoutExpired(startTime: Long, timeoutSeconds: Int): Boolean {
            val currTime = System.currentTimeMillis()
            val elapsed = currTime - startTime
            if (elapsed >= timeoutSeconds * 1000) {
                return true
            } else {
                return false
            }
        }
    }

}