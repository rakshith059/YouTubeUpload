package rakshith.com.youtubeupload.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


/**
 * Created YouTubeUpload by rakshith on 10/13/17.
 */
class YouTubeSnippetThumbnail {

    @SerializedName("default")
    @Expose
    var default: YouTubeSnippetThumbnailDefault? = null
    @SerializedName("medium")
    @Expose
    var medium: YouTubeSnippetThumbnailMedium? = null
    @SerializedName("high")
    @Expose
    var high: YouTubeSnippetThumbnailHigh? = null
    @SerializedName("standard")
    @Expose
    var standard: YouTubeSnippetThumbnailStandard? = null
    @SerializedName("maxres")
    @Expose
    var maxres: YouTubeSnippetThumbnailMaxres? = null

}

class YouTubeSnippetThumbnailDefault {
    @SerializedName("url")
    var youTubeSnippetThumbnailDefaultUrl: String? = null
    @SerializedName("width")
    var youTubeSnippetThumbnailDefaultWidth: Long? = null
    @SerializedName("height")
    var youTubeSnippetThumbnailDefaultHeight: Long? = null
}

class YouTubeSnippetThumbnailMedium {
    @SerializedName("url")
    var youTubeSnippetThumbnailMediumtUrl: String? = null
    @SerializedName("width")
    var youTubeSnippetThumbnailMediumtWidth: Long? = null
    @SerializedName("height")
    var youTubeSnippetThumbnailmediumHeight: Long? = null
}

class YouTubeSnippetThumbnailHigh {
    @SerializedName("url")
    var youTubeSnippetThumbnailHightUrl: String? = null
    @SerializedName("width")
    var youTubeSnippetThumbnailHighWidth: Long? = null
    @SerializedName("height")
    var youTubeSnippetThumbnailHighHeight: Long? = null
}

class YouTubeSnippetThumbnailStandard {
    @SerializedName("url")
    var youTubeSnippetThumbnailStandardUrl: String? = null
    @SerializedName("width")
    var youTubeSnippetThumbnailStandardWidth: Long? = null
    @SerializedName("height")
    var youTubeSnippetThumbnailStandardHeight: Long? = null
}

class YouTubeSnippetThumbnailMaxres {
    @SerializedName("url")
    var youTubeSnippetThumbnailMaxresUrl: String? = null
    @SerializedName("width")
    var youTubeSnippetThumbnailMaxresWidth: Long? = null
    @SerializedName("height")
    var youTubeSnippetThumbnailMaxresHeight: Long? = null
}