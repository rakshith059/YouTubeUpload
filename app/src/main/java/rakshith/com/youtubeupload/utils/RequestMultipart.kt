package rakshith.com.youtubeupload.utils

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import java.io.File
import java.util.HashMap

/**
 * Created YouTubeUpload by rakshith on 10/26/17.
 */

/**
 *
 */
fun RequestMultiPart(file: File, filename: String, boundary: String, url: String, fileField: String, params: Map<String, String>, completion: ApiResponse<String>) {

    val reqUrl = url
    val imageUploadReq = object : MultipartRequest(reqUrl, params, file, filename, fileField,
            Response.ErrorListener { error ->
                Log.d("Multipart Request Url: ", reqUrl)
                Log.d("Multipart ERROR", "error => " + error.toString())
                completion.onCompletion(error.toString())
            },
            Response.Listener<String> { response ->
                Log.d("MediaSent Response", response)
                completion.onCompletion(response)
            }
    ) {

        /* The following method sets the cookies in the header, I needed it for my server
         but you might want to remove it if it is not useful in your case */
        @Throws(AuthFailureError::class)
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            val manager = AppController.getInstance().getCookieManager()
            val cookies = manager.getCookieStore().getCookies()
            var cookie = ""
            for (eachCookie in cookies) {
                val cookieName = eachCookie.getName().toString()
                val cookieValue = eachCookie.getValue().toString()
                cookie += cookieName + "=" + cookieValue + "; "
            }
            headers.put("Cookie", cookie)
            return headers
        }

    }

    imageUploadReq.retryPolicy = DefaultRetryPolicy(1000 * 60, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

    AppController.getInstance().addToRequestQueue(imageUploadReq)
}

//create an interface with a callback method
interface ApiResponse<T> {
    fun onCompletion(result: T)
}