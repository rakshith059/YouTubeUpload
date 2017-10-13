package rakshith.com.youtubeupload.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

/**
 * Created YouTubeUpload by rakshith on 10/13/17.
 */
class AddVideoToPlaylist() {
    @SerializedName("kind")
    var youtubeKind: String? = null
    @SerializedName("etag")
    var youtubeEtag: String? = null
    @SerializedName("id")
    var youtubeId: String? = null
    @SerializedName("snippet")
    var youtubeSnippet: YouTubeSnippet? = null
}