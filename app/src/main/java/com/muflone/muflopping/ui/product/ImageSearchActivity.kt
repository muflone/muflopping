package com.muflone.muflopping.ui.product

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.muflone.muflopping.databinding.ActivityImageSearchBinding
import com.muflone.muflopping.util.ThemeUtils

class ImageSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageSearchBinding
    private var lastSelectedImageUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this, noActionBar = true)
        super.onCreate(savedInstanceState)
        binding = ActivityImageSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val query = intent.getStringExtra(EXTRA_QUERY) ?: ""
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search: $query"

        setupWebView()
        
        val url = "https://www.google.com/search?q=${query}&tbm=isch"
        binding.webView.loadUrl(url)

        binding.fabSelect.setOnClickListener {
            lastSelectedImageUrl?.let { imageUrl ->
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_IMAGE_URL, imageUrl)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        binding.webView.setOnLongClickListener {
            val result = binding.webView.hitTestResult
            if (result.type == WebView.HitTestResult.IMAGE_TYPE || 
                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                
                val imageUrl = result.extra
                if (imageUrl != null) {
                    lastSelectedImageUrl = imageUrl
                    binding.fabSelect.visibility = View.VISIBLE
                }
                true
            } else {
                false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_QUERY = "extra_query"
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }
}
