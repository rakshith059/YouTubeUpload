package rakshith.com.youtubeupload

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.InputStreamContent
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoSnippet
import com.google.api.services.youtube.model.VideoStatus
import rakshith.com.youtubeupload.utils.Constants
import rakshith.com.youtubeupload.utils.Upload
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.*


/**
 * Created YouTubeUpload by rakshith on 10/5/17.
 */

object ResumableUpload {
    /**
     * Assigned to the upload
     */
    val DEFAULT_KEYWORDS = arrayOf("MultiSquash", "Game")
    /**
     * Indicates that the video is fully processed, see https://www.googleapis.com/discovery/v1/apis/youtube/v3/rpc
     */
    private val SUCCEEDED = "succeeded"
    private val TAG = "UploadingActivity"
    private val UPLOAD_NOTIFICATION_ID = 1001
    private val PLAYBACK_NOTIFICATION_ID = 1002
    private val NOT_PROCCESSED_NOTIFICATION_ID = 1003
    /*
     * Global instance of the format used for the video being uploaded (MIME type).
     */
    private val VIDEO_FILE_FORMAT = "video/*"

    /**
     * Uploads user selected video in the project folder to the user's YouTube account using OAuth2
     * for authentication.
     */

    fun upload(youtube: YouTube, fileInputStream: InputStream,
               fileSize: Long, mFileUri: Uri, path: String, context: Context, selectedAccountName: String?): String? {
        val notifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context)

        val notificationIntent = Intent(context, ReviewActivity::class.java)
        notificationIntent.data = mFileUri
        notificationIntent.action = Intent.ACTION_VIEW
        val thumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND)
        val contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        builder.setContentTitle(context.getString(R.string.youtube_upload))
                .setContentText(context.getString(R.string.youtube_upload_started))
                .setSmallIcon(R.drawable.ic_stat_device_access_video).setContentIntent(contentIntent).setStyle(NotificationCompat.BigPictureStyle().bigPicture(thumbnail))
        notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())

        var videoId: String? = null
        try {
            // Add extra information to the video before uploading.
            val videoObjectDefiningMetadata = Video()

            /*
       * Set the video to public, so it is available to everyone (what most people want). This is
       * actually the default, but I wanted you to see what it looked like in case you need to set
       * it to "unlisted" or "private" via API.
       */
            val status = VideoStatus()
            status.privacyStatus = "public"
            videoObjectDefiningMetadata.setStatus(status)

            // We set a majority of the metadata with the VideoSnippet object.
            val snippet = VideoSnippet()

            /*
       * The Calendar instance is used to create a unique name and description for test purposes, so
       * you can see multiple files being uploaded. You will want to remove this from your project
       * and use your own standard names.
       */
            val cal = Calendar.getInstance()
            snippet.title = "Video upload"
            snippet.description = "Video uploaded by " + selectedAccountName//"Video uploaded via YouTube Data API V3 using the Java library " + "on " + cal.getTime()

            // Set your keywords.
            snippet.tags = Arrays.asList(Constants.DEFAULT_KEYWORD, Upload.generateKeywordFromPlaylistId(Constants.UPLOAD_PLAYLIST))

            // Set completed snippet to the video object.
            videoObjectDefiningMetadata.setSnippet(snippet)

            val mediaContent = InputStreamContent(VIDEO_FILE_FORMAT, BufferedInputStream(fileInputStream))
            mediaContent.length = fileSize

            /*
       * The upload command includes: 1. Information we want returned after file is successfully
       * uploaded. 2. Metadata we want associated with the uploaded video. 3. Video file itself.
       */
            val videoInsert = youtube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata,
                    mediaContent)

            // Set the upload type and add event listener.
            val uploader = videoInsert.mediaHttpUploader

            /*
       * Sets whether direct media upload is enabled or disabled. True = whole media content is
       * uploaded in a single request. False (default) = resumable media upload protocol to upload
       * in data chunks.
       */
            uploader.isDirectUploadEnabled = false

            val progressListener = object : MediaHttpUploaderProgressListener {
                @Throws(IOException::class)
                override fun progressChanged(uploader: MediaHttpUploader) {
                    when (uploader.uploadState) {
                        MediaHttpUploader.UploadState.INITIATION_STARTED -> {
                            builder.setContentText(context.getString(R.string.initiation_started)).setProgress(fileSize.toInt(),
                                    uploader.numBytesUploaded.toInt(), false)
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())
                        }
                        MediaHttpUploader.UploadState.INITIATION_COMPLETE -> {
                            builder.setContentText(context.getString(R.string.initiation_completed)).setProgress(fileSize.toInt(),
                                    uploader.numBytesUploaded.toInt(), false)
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())
                        }
                        MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                            builder
                                    .setContentTitle(context.getString(R.string.youtube_upload) +
                                            (uploader.progress * 100).toInt() + "%")
                                    .setContentText(context.getString(R.string.upload_in_progress))
                                    .setProgress(fileSize.toInt(), uploader.numBytesUploaded.toInt(), false)
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())
                        }
                        MediaHttpUploader.UploadState.MEDIA_COMPLETE -> {
                            builder.setContentTitle(context.getString(R.string.yt_upload_completed))
                                    .setContentText(context.getString(R.string.upload_completed))
                                    // Removes the progress bar
                                    .setProgress(0, 0, false)
                            notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())
                            Log.d(this.javaClass.simpleName, context.getString(R.string.upload_not_started))
                        }
                        MediaHttpUploader.UploadState.NOT_STARTED -> Log.d(this.javaClass.simpleName, context.getString(R.string.upload_not_started))
                    }
                }
            }
            uploader.progressListener = progressListener

            // Execute upload.
            val returnedVideo = videoInsert.execute()
            Log.d(TAG, "Video upload completed")
            videoId = returnedVideo.id
            Log.d(TAG, String.format("videoId = [%s]", videoId))
        } catch (availabilityException: GooglePlayServicesAvailabilityIOException) {
            Log.e(TAG, "GooglePlayServicesAvailabilityIOException", availabilityException)
            notifyFailedUpload(context, context.getString(R.string.cant_access_play), notifyManager, builder)
        } catch (userRecoverableException: UserRecoverableAuthIOException) {
            Log.i(TAG, String.format("UserRecoverableAuthIOException: %s",
                    userRecoverableException.message))
            requestAuth(context, userRecoverableException)
        } catch (e: IOException) {
            Log.e(TAG, "IOException", e)
            notifyFailedUpload(context, context.getString(R.string.please_try_again), notifyManager, builder)
        }

        return videoId
    }

    private fun requestAuth(context: Context,
                            userRecoverableException: UserRecoverableAuthIOException) {
        val manager = LocalBroadcastManager.getInstance(context)
        val authIntent = userRecoverableException.intent
        val runReqAuthIntent = Intent(MainActivity.REQUEST_AUTHORIZATION_INTENT)
        runReqAuthIntent.putExtra(MainActivity.REQUEST_AUTHORIZATION_INTENT_PARAM, authIntent)
        manager.sendBroadcast(runReqAuthIntent)
        Log.d(TAG, String.format("Sent broadcast %s", MainActivity.REQUEST_AUTHORIZATION_INTENT))
    }

    private fun notifyFailedUpload(context: Context, message: String, notifyManager: NotificationManager,
                                   builder: NotificationCompat.Builder) {
        builder.setContentTitle(context.getString(R.string.yt_upload_failed))
                .setContentText(message)
        notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build())
        Log.e(ResumableUpload::class.java.simpleName, message)
    }

    fun showSelectableNotification(videoId: String, context: Context) {
        Log.d(TAG, String.format("Posting selectable notification for video ID [%s]", videoId))
        val notifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context)
        val notificationIntent = Intent(context, PlayActivity::class.java)
        notificationIntent.putExtra(MainActivity.YOUTUBE_ID, videoId)
        notificationIntent.setAction(Intent.ACTION_VIEW)

        val url: URL
        try {
            url = URL("https://i1.ytimg.com/vi/$videoId/mqdefault.jpg")
            val thumbnail = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            val contentIntent = PendingIntent.getActivity(context,
                    0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            builder.setContentTitle(context.getString(R.string.watch_your_video))
                    .setContentText(context.getString(R.string.see_the_newly_uploaded_video)).setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_stat_device_access_video).setStyle(NotificationCompat.BigPictureStyle().bigPicture(thumbnail))
            notifyManager.notify(PLAYBACK_NOTIFICATION_ID, builder.build())
            Log.d(TAG, String.format("Selectable notification for video ID [%s] posted", videoId))
        } catch (e: MalformedURLException) {
            Log.e(TAG, e.message)
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        }

    }

    fun showLocalNotification(context: Context, notificationString: String) {
        val notifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context)

        val url: URL? = null
        try {
            val thumbnail = BitmapFactory.decodeStream(url?.openConnection()?.getInputStream())
            builder.setContentTitle(notificationString)
                    .setContentText(notificationString).setSmallIcon(R.drawable.ic_stat_device_access_video).setStyle(NotificationCompat.BigPictureStyle().bigPicture(thumbnail))
            notifyManager.notify(NOT_PROCCESSED_NOTIFICATION_ID, builder.build())
        } catch (e: MalformedURLException) {
            Log.e(TAG, e.message)
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        }

    }


    /**
     * @return url of thumbnail if the video is fully processed
     */
    fun checkIfProcessed(videoId: String, youtube: YouTube): Boolean {
        try {
            val list = youtube.videos().list("processingDetails")
            list.id = videoId
            val listResponse = list.execute()
            val videos = listResponse.items
            if (videos.size == 1) {
                val video = videos[0]
                val status = video.processingDetails.processingStatus
                Log.e(TAG, String.format("Processing status of [%s] is [%s]", videoId, status))
                if (status == SUCCEEDED) {
                    return true
                }
            } else {
                // can't find the video
                Log.e(TAG, String.format("Can't find video with ID [%s]", videoId))
                return false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error fetching video metadata", e)
        }

        return false
    }
}
