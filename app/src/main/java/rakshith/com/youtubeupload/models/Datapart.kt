package rakshith.com.youtubeupload.models

/**
 * Created YouTubeUpload by rakshith on 10/25/17.
 */
class DataPart {
    /**
     * Getter file name.

     * @return file name
     */
    /**
     * Setter file name.

     * @param fileName string file name
     */
    var fileName: String? = null
    /**
     * Getter content.

     * @return byte file data
     */
    /**
     * Setter content.

     * @param content byte file data
     */
    var content: ByteArray? = null
    /**
     * Getter mime type.

     * @return mime type
     */
    /**
     * Setter mime type.

     * @param type mime type
     */
    var type: String? = null

    /**
     * Default data part
     */
    constructor() {}

    /**
     * Constructor with data.

     * @param name label of data
     * *
     * @param data byte data
     */
    constructor(name: String, data: ByteArray?) {
        fileName = name
        content = data
    }

    /**
     * Constructor with mime data type.

     * @param name     label of data
     * *
     * @param data     byte data
     * *
     * @param mimeType mime data like "image/jpeg"
     */
    constructor(name: String, data: ByteArray, mimeType: String) {
        fileName = name
        content = data
        type = mimeType
    }
}