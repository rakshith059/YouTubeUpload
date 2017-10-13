package rakshith.com.youtubeupload.utils

import com.google.api.services.youtube.model.VideoSnippet
import com.google.api.services.youtube.model.Video


/**
 * Created YouTubeUpload by rakshith on 10/5/17.
 */

class VideoData {
    var video: Video? = null

    val youTubeId: String
        get() = video!!.getId()

    val title: String
        get() = video!!.getSnippet().getTitle()

    fun addTags(tags: Collection<String>): VideoSnippet {
        val mSnippet = video!!.getSnippet()
        var mTags: MutableList<String>? = mSnippet.getTags()
        if (mTags == null) {
            mTags = ArrayList<String>(2)
        }
        mTags.addAll(tags)
        return mSnippet
    }

    val thumbUri: String
        get() = video!!.getSnippet().getThumbnails().getDefault().getUrl()

    val watchUri: String
        get() = "http://www.youtube.com/watch?v=" + youTubeId
}