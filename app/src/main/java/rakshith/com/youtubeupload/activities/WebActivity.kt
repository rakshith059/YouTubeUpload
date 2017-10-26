package rakshith.com.youtubeupload.activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_web.*
import rakshith.com.youtubeupload.R
import rakshith.com.youtubeupload.utils.Constants

class WebActivity : AppCompatActivity() {

    private val EXTRA_INTENT_PAGE_URL: String = "https://www.mixcloud.com/oauth/authorize?client_id=sqM2pz7u2VMPsKbyHe&redirect_uri=https://rakshith.com/mixcloud-uri"
    private val EXTRA_INTENT_PAGE_TITLE: String = "mixCloud"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        val toolbar = findViewById<Toolbar>(R.id.webview_toolbar)
        setSupportActionBar(toolbar)
        assert(supportActionBar != null)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))

        setUpWebView(EXTRA_INTENT_PAGE_TITLE, EXTRA_INTENT_PAGE_URL)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun setUpWebView(pageTitle: String, url: String) {
        web_view?.settings?.javaScriptEnabled = true

        web_view?.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loading_panel?.setVisibility(View.VISIBLE)
                Log.d("Rakshith", "On Page started loading == " + url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("Rakshith", "On Page finished loading == " + url)
                web_view.settings.javaScriptEnabled = false

                if (url?.contains("code=") as Boolean) {
                    var oAuthCode: String = url?.substring(url.indexOf("code=") + 5, url.length) as String
                    Log.d("Rakshith", "oAuthCode == " + oAuthCode)

                    Constants.setSharedPrefrence(this@WebActivity, Constants.OAUTH_CODE, oAuthCode)

                    var intent: Intent = Intent(Constants.CALLBACK_INTENT_FILTER_RECIVER)
                    intent?.putExtra(Constants.OAUTH_CODE, oAuthCode)
                    LocalBroadcastManager.getInstance(this@WebActivity).sendBroadcast(intent)
                    finish()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return true
            }
        }

//        web_view?.webViewClient = object : WebViewClient() {
//            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
////                loading_panel?.setVisibility(View.VISIBLE)
//                Log.d("Rakshith", "On Page started loading == " + url)
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
////                loading_panel.setVisibility(View.GONE)
//                Log.d("Rakshith", "On Page finished loading == " + url)
//                web_view.settings.javaScriptEnabled = false
//            }
//
//            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
//                return true
//            }
//        }
        title = pageTitle
        web_view?.loadUrl(url)
    }

}
