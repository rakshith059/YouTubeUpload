package rakshith.com.youtubeupload.utils

/**
 * Created YouTubeUpload by rakshith on 10/5/17.
 */
object Upload {
    fun generateKeywordFromPlaylistId(playlistId: String?): String {
        var playlistId = playlistId
        if (playlistId == null) playlistId = ""
        if (playlistId.indexOf("PL") == 0) {
            playlistId = playlistId.substring(2)
        }
        playlistId = playlistId.replace("\\W".toRegex(), "")
        var keyword = Constants.DEFAULT_KEYWORD + playlistId
        if (keyword.length > Constants.MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, Constants.MAX_KEYWORD_LENGTH)
        }
        return keyword
    }

}