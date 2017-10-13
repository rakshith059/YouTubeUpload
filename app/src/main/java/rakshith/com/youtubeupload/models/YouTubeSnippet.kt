package rakshith.com.youtubeupload.models

import com.google.gson.annotations.SerializedName

/**
 * Created YouTubeUpload by rakshith on 10/13/17.
 */
class YouTubeSnippet {
    @SerializedName("publishedAt")
    var youtubeSnippetPublishedAt: String? = null
    @SerializedName("channelId")
    var youtubeSnippetChannelId: String? = null
    @SerializedName("title")
    var youtubeSnippetTitle: String? = null
    @SerializedName("description")
    var youtubeSnippetDescription: String? = null
    @SerializedName("thumbnails")
    var youtubeSnippetThumbnail: YouTubeSnippetThumbnail? = null
    @SerializedName("channelTitle")
    var youtubeSnippetChannelTitle: String? = null
    @SerializedName("playlistId")
    var youtubeSnippetPlaylistId: String? = null
    @SerializedName("resourceId")
    var youtubeSnippetResorceId: YouTubeSnippetResourceId? = null
}