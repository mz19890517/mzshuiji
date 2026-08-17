package com.mz.shunji.di

import android.content.Context
import android.text.style.BackgroundColorSpan
import android.text.util.Linkify.EMAIL_ADDRESSES
import android.text.util.Linkify.WEB_URLS
import android.util.TypedValue
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolverDef
import io.noties.markwon.Markwon
import io.noties.markwon.Markwon.builder
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.SpanFactory
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.noties.markwon.simple.ext.SimpleExtPlugin
import me.saket.bettermovementmethod.BetterLinkMovementMethod.getInstance
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.mz.shunji.R.attr.colorBackground
import com.mz.shunji.R.attr.colorMarkdownTask
import com.mz.shunji.R.attr.colorNoteTextHighlight
import com.mz.shunji.preferences.PreferenceRepository
import com.mz.shunji.ui.editor.markdown.BlockQuoteHandler
import com.mz.shunji.ui.editor.markdown.CodeBlockHandler
import com.mz.shunji.ui.editor.markdown.CodeHandler
import com.mz.shunji.ui.editor.markdown.HeadingHandler
import com.mz.shunji.ui.editor.markdown.StrikethroughHandler
import com.mz.shunji.ui.utils.coil.CoilImagesPlugin
import com.mz.shunji.ui.utils.resolveAttribute

object MarkwonModule {

    val markwonModule = module {
        factory<Markwon> { getMarkwon(context = androidContext(), preferenceRepository = get()) }
        factory<MarkwonEditor> { getMarkWonEditor(markwon = get()) }
    }

    private fun getMarkwon(context: Context, preferenceRepository: PreferenceRepository): Markwon = builder(context)
        .usePlugin(LinkifyPlugin.create(EMAIL_ADDRESSES or WEB_URLS))
        .usePlugin(SoftBreakAddsNewLinePlugin.create())
        .usePlugin(MovementMethodPlugin.create(getInstance()))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver(LinkResolverDef())
            }
        })
        .usePlugin(SimpleExtPlugin.create { plugin: SimpleExtPlugin ->
            plugin
                .addExtension(
                    /* length = */ 2,
                    /* character = */ '=',
                    /* spanFactory = */ SpanFactory { _, _ ->
                        val typedValue = TypedValue()
                        context.theme.resolveAttribute(colorNoteTextHighlight, typedValue, true)
                        return@SpanFactory BackgroundColorSpan(typedValue.data)
                    })
        })
        .usePlugin(CoilImagesPlugin.create(context, preferenceRepository))
        .apply {
            val mainColor = context.resolveAttribute(colorMarkdownTask)
            val backgroundColor = context.resolveAttribute(colorBackground)
            val taskPlugin = if (mainColor != null && backgroundColor != null)
                TaskListPlugin.create(mainColor, mainColor, backgroundColor)
            else TaskListPlugin.create(context)
            usePlugin(taskPlugin)
        }
        .build()

    private fun getMarkWonEditor(markwon: Markwon): MarkwonEditor {
        return MarkwonEditor.builder(markwon)
            .useEditHandler(EmphasisEditHandler())
            .useEditHandler(StrongEmphasisEditHandler())
            .useEditHandler(CodeHandler())
            .useEditHandler(CodeBlockHandler())
            .useEditHandler(BlockQuoteHandler())
            .useEditHandler(StrikethroughHandler())
            .useEditHandler(HeadingHandler())
            .build()
    }

}
