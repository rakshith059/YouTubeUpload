package rakshith.com.youtubeupload.utils

import com.android.volley.Response
import com.android.volley.VolleyError

/**
 * Created YouTubeUpload by rakshith on 10/12/17.
 */
class NetworkVolleyResponse<T>(private val mCallBack: NetworkVolleyRequest.Callback<T>?) : Response.Listener<T>, Response.ErrorListener {

    override fun onResponse(t: T) {
        mCallBack?.onSuccess(t)
    }

    override fun onErrorResponse(volleyError: VolleyError?) {
        if (mCallBack != null) {
            if (volleyError != null && volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
                mCallBack.onError(volleyError.networkResponse.statusCode, volleyError.networkResponse.data.toString())
            } else {
                mCallBack.onError(0, "")
            }
        }
    }
}