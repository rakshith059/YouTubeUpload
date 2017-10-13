package rakshith.com.youtubeupload.utils

import android.app.Activity
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.GoogleAuthException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import rakshith.com.youtubeupload.R

/**
 * Created YouTubeUpload by rakshith on 10/5/17.
 */
object Utils {

    fun hasFroyo(): Boolean {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO
    }

    fun hasGingerbread(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
    }

    fun hasHoneycomb(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
    }

    fun hasHoneycombMR1(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
    }

    fun hasJellyBean(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
    }

    /**
     * Logs the given throwable and shows an error alert dialog with its message.

     * @param activity activity
     * *
     * @param tag      log tag to use
     * *
     * @param t        throwable to log and show
     */
    fun logAndShow(activity: Activity, tag: String, t: Throwable) {
        Log.e(tag, "Error", t)
        var message = t.message
        if (t is GoogleJsonResponseException) {
            val details = t.details
            if (details != null) {
                message = details.message
            }
        } else if (t.cause is GoogleAuthException) {
            message = (t.cause as GoogleAuthException).message
        }
        showError(activity, message as String)
    }

    /**
     * Logs the given message and shows an error alert dialog with it.

     * @param activity activity
     * *
     * @param tag      log tag to use
     * *
     * @param message  message to log and show or `null` for none
     */
    fun logAndShowError(activity: Activity, tag: String, message: String) {
        val errorMessage = getErrorMessage(activity, message)
        Log.e(tag, errorMessage)
        showErrorInternal(activity, errorMessage)
    }

    /**
     * Shows an error alert dialog with the given message.

     * @param activity activity
     * *
     * @param message  message to show or `null` for none
     */
    fun showError(activity: Activity, message: String) {
        val errorMessage = getErrorMessage(activity, message)
        showErrorInternal(activity, errorMessage)
    }

    private fun showErrorInternal(activity: Activity, errorMessage: String) {
        activity.runOnUiThread { Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show() }
    }

    private fun getErrorMessage(activity: Activity, message: String?): String {
        val resources = activity.resources
        if (message == null) {
            return resources.getString(R.string.error)
        }
        return resources.getString(R.string.error_format, message)
    }
}
