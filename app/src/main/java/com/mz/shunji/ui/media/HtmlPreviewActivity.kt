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
import com.mz.shunji.ui.attachments.getHtmlBaseDir

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

        wv.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                view?.loadDataWithBaseURL(null,
                    "<html><body style='padding:16px;font-family:sans-serif;'>" +
                    "<h3>加载失败</h3><p>${description ?: "无法加载此 HTML 文件"}</p>" +
                    "<p>文件路径: ${attachment.path}</p>" +
                    "</body></html>",
                    "text/html", "UTF-8", null)
            }
        }

        // Resolve the actual file path for loadDataWithBaseURL so relative paths (JS/CSS) work
        val htmlFile = java.io.File(getHtmlBaseDir(this), attachment.path)
        if (!htmlFile.exists()) {
            // Fallback: try via attachment URI
            val uri = getAttachmentUri(this, attachment.path)
            if (uri != null) {
                wv.loadUrl(uri.toString())
            } else {
                wv.loadDataWithBaseURL(null,
                    "<html><body style='padding:16px;font-family:sans-serif;'>" +
                    "<h3>文件未找到</h3><p>${attachment.path}</p>" +
                    "</body></html>",
                    "text/html", "UTF-8", null)
            }
            return
        }

        val htmlContent = try {
            htmlFile.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }

        if (htmlContent != null) {
            // Use file:// base URL so relative paths to JS/CSS/images resolve correctly
            val baseUrl = "file://${htmlFile.parentFile?.absolutePath ?: htmlFile.absolutePath}/"
            wv.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
        } else {
            wv.loadDataWithBaseURL(null,
                "<html><body style='padding:16px;font-family:sans-serif;'>" +
                "<h3>读取失败</h3><p>无法读取文件内容</p>" +
                "</body></html>",
                "text/html", "UTF-8", null)
        }
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
