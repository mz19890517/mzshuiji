package com.mz.shunji.ui.media

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.mz.shunji.R
import com.mz.shunji.data.model.Attachment
import com.mz.shunji.ui.BaseActivity
import com.mz.shunji.ui.attachments.getAttachmentUri

class HtmlPreviewActivity : BaseActivity() {
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val attachment = intent.extras?.getParcelable<Attachment>(ATTACHMENT) ?: return finish()
        val title = attachment.description.ifEmpty { attachment.fileName }

        val toolbar = Toolbar(this).apply {
            setBackgroundColor(Color.BLACK)
            setTitleTextColor(Color.WHITE)
            setTitle(title)
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { finish() }
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        root.addView(toolbar, android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(webView, android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0, 1f
        ))

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            toolbar.updatePadding(top = top)
            WindowInsetsCompat.CONSUMED
        }

        window.setBackgroundDrawable(Color.BLACK.toDrawable())

        setupWebView(attachment)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(attachment: Attachment) {
        val wv = webView ?: return
        val uri = getAttachmentUri(this, attachment.path) ?: return finish()

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        wv.webViewClient = WebViewClient()
        wv.loadUrl(uri.toString())
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        const val ATTACHMENT = "ATTACHMENT"
    }
}
