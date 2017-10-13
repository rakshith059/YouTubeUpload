package rakshith.com.youtubeupload.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.plus.Plus
import com.google.android.gms.plus.PlusOneButton
import kotlinx.android.synthetic.main.upload_list_fragment.*
import rakshith.com.youtubeupload.R
import android.widget.TextView
import android.widget.BaseAdapter
import rakshith.com.youtubeupload.utils.VideoData
import java.util.List
import android.widget.GridView
import java.security.Permission


@SuppressLint("ValidFragment")
/**
 * Created YouTubeUpload by rakshith on 10/5/17.
 */

class UploadListFragment() : Fragment(), GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    var mContext: Context? = null
    var RESOLVE_CONNECTION_REQUEST_CODE: Int = 1001

    constructor(context: Context?) : this() {
        mContext = context
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (connectionResult?.hasResolution() as Boolean) {
            Toast.makeText(activity,
                    R.string.connection_to_google_play_failed, Toast.LENGTH_SHORT)
                    .show()

            Log.e(TAG,
                    String.format(
                            "Connection to Play Services Failed, error: %d, reason: %s",
                            connectionResult.errorCode,
                            connectionResult.toString()))
            try {
                connectionResult.startResolutionForResult(activity, RESOLVE_CONNECTION_REQUEST_CODE)
            } catch (e: IntentSender.SendIntentException) {
                googleApiClient?.connect();
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESOLVE_CONNECTION_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                googleApiClient?.connect();
            }
        }
    }

    private val REQUEST_CODE_ASK_PERMISSIONS: Int = 1002

    override fun onConnected(p0: Bundle?) {
        if (gvGridView?.getAdapter() != null) {
            (gvGridView?.getAdapter() as UploadedVideoAdapter).notifyDataSetChanged()
        }

        setProfileInfo()

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            var hasGetAccountPermission: Int = ContextCompat.checkSelfPermission(mContext as Context, android.Manifest.permission.GET_ACCOUNTS)
            if (hasGetAccountPermission != PackageManager.PERMISSION_GRANTED)
                requestPermissions(Array(3, { android.Manifest.permission.GET_ACCOUNTS }),
                        REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }

        callBack?.onConnected(Plus.AccountApi.getAccountName(googleApiClient))
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

    override fun onConnectionSuspended(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    var callBack: CallBacks? = null
    var googleApiClient: GoogleApiClient? = null
    var imageLoader: ImageLoader? = null
    var gvGridView: GridView? = null

    companion object {
        var TAG: String = UploadListFragment.javaClass.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view: View? = inflater?.inflate(R.layout.upload_list_fragment, container, false)
        if (mContext == null) {
            mContext = view?.context
        }
        googleApiClient = GoogleApiClient.Builder(mContext as Context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build()
        gvGridView = view?.findViewById<GridView>(R.id.grid_view)
        val emptyView = view?.findViewById<TextView>(android.R.id.empty)
        gvGridView?.setEmptyView(emptyView)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setProfileInfo()
    }

    private fun setProfileInfo() {
        //not sure if mGoogleapiClient.isConnect is appropriate...
        if (!googleApiClient?.isConnected()!! || Plus.PeopleApi.getCurrentPerson(googleApiClient) == null) {
            (view?.findViewById<View>(R.id.avatar) as ImageView)
                    .setImageDrawable(null)
            (view?.findViewById<View>(R.id.display_name) as TextView)
                    .setText(R.string.not_signed_in)
        } else {
            val currentPerson = Plus.PeopleApi.getCurrentPerson(googleApiClient)
//            if (currentPerson.hasImage()) {
//                // Set the URL of the image that should be loaded into this view, and
//                // specify the ImageLoader that will be used to make the request.
//                (view?.findViewById<View>(R.id.avatar) as NetworkImageView).setImageUrl(currentPerson?.image?.url, imageLoader)
//            }
            if (currentPerson.hasDisplayName()) {
                (view?.findViewById<View>(R.id.display_name) as TextView).text = currentPerson.displayName
            }
        }
    }

    override fun onResume() {
        super.onResume()
        googleApiClient?.connect()
    }

    override fun onPause() {
        super.onPause()
        googleApiClient?.disconnect()
    }

    private inner class UploadedVideoAdapter(private val mVideos: List<VideoData>) : BaseAdapter() {

        override fun getCount(): Int {
            return mVideos?.size
        }

        override fun getItem(i: Int): Any {
            return mVideos[i]
        }

        override fun getItemId(i: Int): Long {
            return mVideos[i].youTubeId?.hashCode()?.toLong()
        }

        override fun getView(position: Int, convertView: View?,
                             container: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(
                        R.layout.list_item, container, false)
            }

            val video = mVideos[position]
            (convertView?.findViewById<View>(R.id.list_item_tv_text) as TextView)
                    .setText(video?.title)
//            (convertView?.findViewById<View>(R.id.thumbnail) as NetworkImageView).setImageUrl(video?.thumbUri, imageLoader)
            if (googleApiClient?.isConnected() as Boolean) {
                (convertView?.findViewById<View>(R.id.plus_button) as PlusOneButton)
                        .initialize(video?.watchUri, null)
            }
            convertView.findViewById<View>(R.id.main_target).setOnClickListener { callBack?.onVideoSelected(mVideos[position]) }
            return convertView
        }
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        if (activity !is CallBacks) {
            throw ClassCastException("Activity must implement callbacks.")
        }

        callBack = activity as CallBacks
        imageLoader = callBack?.onGetImageLoader()
    }

    override fun onDetach() {
        super.onDetach()
        callBack = null
        imageLoader = null
    }

    interface CallBacks {
        fun onGetImageLoader(): ImageLoader?

        fun onVideoSelected(video: VideoData)

        fun onConnected(connectedAccountName: String)
    }

    fun setVideos(videos: java.util.List<VideoData>?) {
        if (!isAdded) {
            return
        }

        gvGridView?.setAdapter(UploadedVideoAdapter(videos as List<VideoData>))
    }
}
