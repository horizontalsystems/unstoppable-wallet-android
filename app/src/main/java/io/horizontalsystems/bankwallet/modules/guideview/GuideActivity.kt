package io.horizontalsystems.bankwallet.modules.guideview

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.lifecycle.Observer
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Guide
import io.noties.markwon.Markwon
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.picasso.PicassoImagesPlugin
import io.noties.markwon.image.picasso.PicassoImagesPlugin.PicassoStore
import kotlinx.android.synthetic.main.activity_guide.*
import org.apache.commons.io.IOUtils
import java.io.StringWriter


class GuideActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        appBarLayout.outlineProvider = null
        setTransparentStatusBar()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val markwon = Markwon.builder(this) // automatically create Picasso instance
                .usePlugin(PicassoImagesPlugin.create(this)) // use provided picasso instance
                .usePlugin(PicassoImagesPlugin.create(Picasso.get())) // if you need more control
                .usePlugin(PicassoImagesPlugin.create(object : PicassoStore {
                    @NonNull
                    override fun load(@NonNull drawable: AsyncDrawable): RequestCreator {
                        return Picasso.get()
                                .load(drawable.destination) // please note that drawable should be used as tag (not a destination)
                                // otherwise there won't be support for multiple images with the same URL
                                .tag(drawable)
                    }

                    override fun cancel(@NonNull drawable: AsyncDrawable) {
                        Picasso.get()
                                .cancelTag(drawable)
                    }
                }))
                .build()


        val guide = intent.extras?.getParcelable<Guide>(GuideModule.GuideKey)
        val viewModel by viewModels<GuideViewModel> { GuideModule.Factory(guide) }

        viewModel.guideLiveData.observe(this, Observer {
            Picasso.get().load(it.imageUrl).into(image)

            val writer = StringWriter()
            IOUtils.copy(assets.open("guides/${it.fileName}.md"), writer, Charsets.UTF_8)
            markwon.setMarkdown(textView, writer.toString())
        })

    }

    private fun getMarkDown(): String {
        return  "![logo](./art/markwon_logo.png)\n" +
                "\n" +
                "# Markwon\n" +
                "\n" +
                "[![Build](https://github.com/noties/Markwon/workflows/Build/badge.svg)](https://github.com/noties/Markwon/actions)\n" +
                "\n" +
                "**Markwon** is a markdown library for Android. It parses markdown\n" +
                "following [commonmark-spec] with the help of amazing [commonmark-java]\n" +
                "library and renders result as _Android-native_ Spannables. **No HTML**\n" +
                "is involved as an intermediate step. <u>**No WebView** is required</u>.\n" +
                "It's extremely fast, feature-rich and extensible.\n" +
                "\n" +
                "It gives ability to display markdown in all TextView widgets\n" +
                "(**TextView**, **Button**, **Switch**, **CheckBox**, etc), **Toasts**\n" +
                "and all other places that accept **Spanned content**. Library provides\n" +
                "reasonable defaults to display style of a markdown content but also \n" +
                "gives all the means to tweak the appearance if desired. All markdown\n" +
                "features listed in [commonmark-spec] are supported\n" +
                "(including support for **inlined/block HTML code**, **markdown tables**,\n" +
                "**images** and **syntax highlight**).\n" +
                "\n" +
                "Since version **4.2.0** **Markwon** comes with an [editor](./markwon-editor/) to _highlight_ markdown input\n" +
                "as user types (for example in **EditText**).\n" +
                "\n" +
                "[commonmark-spec]: https://spec.commonmark.org/0.28/\n" +
                "[commonmark-java]: https://github.com/atlassian/commonmark-java/blob/master/README.md\n" +
                "\n" +
                "<sup>*</sup>*This file is displayed by default in the [sample-apk] (`markwon-sample-{latest-version}-debug.apk`) application. Which is a generic markdown viewer with support to display markdown via `http`, `https` & `file` schemes and 2 themes included: Light & Dark*\n" +
                "\n" +
                "[sample-apk]: https://github.com/noties/Markwon/releases\n" +
                "\n" +
                "## Installation\n" +
                "\n" +
                "![stable](https://github.com/noties/Markwon/raw/master/art/mw_light_01.png)\n" +
                "![snapshot](https://img.shields.io/nexus/s/https/oss.sonatype.org/io.noties.markwon/core.svg?label=snapshot)\n" +
                "\n" +
                "```kotlin\n" +
                "implementation \"io.noties.markwon:core:4.4.0\n" +
                "```\n" +
                "\n" +
                "Full list of available artifacts is present in the [install section](https://noties.github.io/Markwon/docs/v4/install.html)\n" +
                "of the [documentation] web-site.\n" +
                "\n" +
                "Please visit [documentation] web-site for further reference.\n" +
                "\n" +
                "\n" +
                "> You can find previous version of Markwon in [2.x.x](https://github.com/noties/Markwon/tree/2.x.x)\n" +
                "and [3.x.x](https://github.com/noties/Markwon/tree/3.x.x) branches\n" +
                "\n" +
                "\n" +
                "## Supported markdown features:\n" +
                "* Emphasis (`*`, `_`)\n" +
                "* Strong emphasis (`**`, `__`)\n" +
                "* Strike-through (`~~`)\n" +
                "* Headers (`#{1,6}`)\n" +
                "* Links (`[]()` && `[][]`)\n" +
                "* Images\n" +
                "* Thematic break (`---`, `***`, `___`)\n" +
                "* Quotes & nested quotes (`>{1,}`)\n" +
                "* Ordered & non-ordered lists & nested ones\n" +
                "* Inline code\n" +
                "* Code blocks\n" +
                "* Tables (*with limitations*)\n" +
                "* Syntax highlight\n" +
                "* LaTeX formulas\n" +
                "* HTML\n" +
                "  * Emphasis (`<i>`, `<em>`, `<cite>`, `<dfn>`)\n" +
                "  * Strong emphasis (`<b>`, `<strong>`)\n" +
                "  * SuperScript (`<sup>`)\n" +
                "  * SubScript (`<sub>`)\n" +
                "  * Underline (`<u>`, `ins`)\n" +
                "  * Strike-through (`<s>`, `<strike>`, `<del>`)\n" +
                "  * Link (`a`)\n" +
                "  * Lists (`ul`, `ol`)\n" +
                "  * Images (`img` will require configured image loader)\n" +
                "  * Blockquote (`blockquote`)\n" +
                "  * Heading (`h1`, `h2`, `h3`, `h4`, `h5`, `h6`)\n" +
                "  * there is support to render any HTML tag\n" +
                "* Task lists:\n" +
                "- [ ] Not _done_\n" +
                "  - [X] **Done** with `X`\n" +
                "  - [x] ~~and~~ **or** small `x`\n" +
                "---\n" +
                "\n" +
                "## Screenshots\n" +
                "\n" +
                "Taken with default configuration (except for image loading):\n" +
                "\n" +
                "<a href=\"./art/mw_light_01.png\"><img src=\"./art/mw_light_01.png\" width=\"30%\" /></a>\n" +
                "<a href=\"./art/mw_light_02.png\"><img src=\"./art/mw_light_02.png\" width=\"30%\" /></a>\n" +
                "<a href=\"./art/mw_light_03.png\"><img src=\"./art/mw_light_03.png\" width=\"30%\" /></a>\n" +
                "<a href=\"./art/mw_dark_01.png\"><img src=\"./art/mw_dark_01.png\" width=\"30%\" /></a>\n" +
                "\n" +
                "By default configuration uses TextView textColor for styling, so changing textColor changes style\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Documentation\n" +
                "\n" +
                "Please visit [documentation] web-site for reference\n" +
                "\n" +
                "[documentation]: https://noties.github.io/Markwon\n" +
                "\n" +
                "---\n" +
                "\n" +
                "# Demo\n" +
                "Based on [this cheatsheet][cheatsheet]\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Headers\n" +
                "---\n" +
                "# Header 1\n" +
                "## Header 2\n" +
                "### Header 3\n" +
                "#### Header 4\n" +
                "##### Header 5\n" +
                "###### Header 6\n" +
                "---\n" +
                "\n" +
                "## Emphasis\n" +
                "\n" +
                "Emphasis, aka italics, with *asterisks* or _underscores_.\n" +
                "\n" +
                "Strong emphasis, aka bold, with **asterisks** or __underscores__.\n" +
                "\n" +
                "Combined emphasis with **asterisks and _underscores_**.\n" +
                "\n" +
                "Strikethrough uses two tildes. ~~Scratch this.~~\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Lists\n" +
                "1. First ordered list item\n" +
                "2. Another item\n" +
                "  * Unordered sub-list.\n" +
                "1. Actual numbers don't matter, just that it's a number\n" +
                "  1. Ordered sub-list\n" +
                "4. And another item.\n" +
                "\n" +
                "   You can have properly indented paragraphs within list items. Notice the blank line above, and the leading spaces (at least one, but we'll use three here to also align the raw Markdown).\n" +
                "\n" +
                "   To have a line break without a paragraph, you will need to use two trailing spaces.\n" +
                "   Note that this line is separate, but within the same paragraph.\n" +
                "   (This is contrary to the typical GFM line break behaviour, where trailing spaces are not required.)\n" +
                "\n" +
                "* Unordered list can use asterisks\n" +
                "- Or minuses\n" +
                "+ Or pluses\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Links\n" +
                "\n" +
                "[I'm an inline-style link](https://www.google.com)\n" +
                "\n" +
                "[I'm a reference-style link][Arbitrary case-insensitive reference text]\n" +
                "\n" +
                "[I'm a relative reference to a repository file](../blob/master/LICENSE)\n" +
                "\n" +
                "[You can use numbers for reference-style link definitions][1]\n" +
                "\n" +
                "Or leave it empty and use the [link text itself].\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Code\n" +
                "\n" +
                "Inline `code` has `back-ticks around` it.\n" +
                "\n" +
                "```javascript\n" +
                "var s = \"JavaScript syntax highlighting\";\n" +
                "alert(s);\n" +
                "```\n" +
                "\n" +
                "```python\n" +
                "s = \"Python syntax highlighting\"\n" +
                "print s\n" +
                "```\n" +
                "\n" +
                "```java\n" +
                "/**\n" +
                " * Helper method to obtain a Parser with registered strike-through &amp; table extensions\n" +
                " * &amp; task lists (added in 1.0.1)\n" +
                " *\n" +
                " * @return a Parser instance that is supported by this library\n" +
                " * @since 1.0.0\n" +
                " */\n" +
                "@NonNull\n" +
                "public static Parser createParser() {\n" +
                "  return new Parser.Builder()\n" +
                "      .extensions(Arrays.asList(\n" +
                "          StrikethroughExtension.create(),\n" +
                "          TablesExtension.create(),\n" +
                "          TaskListExtension.create()\n" +
                "      ))\n" +
                "      .build();\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "```xml\n" +
                "<ScrollView\n" +
                "  android:id=\"@+id/scroll_view\"\n" +
                "  android:layout_width=\"match_parent\"\n" +
                "  android:layout_height=\"match_parent\"\n" +
                "  android:layout_marginTop=\"?android:attr/actionBarSize\">\n" +
                "\n" +
                "  <TextView\n" +
                "    android:id=\"@+id/text\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"wrap_content\"\n" +
                "    android:layout_margin=\"16dip\"\n" +
                "    android:lineSpacingExtra=\"2dip\"\n" +
                "    android:textSize=\"16sp\"\n" +
                "    tools:text=\"yo\\nman\" />\n" +
                "\n" +
                "</ScrollView>\n" +
                "```\n" +
                "\n" +
                "```\n" +
                "No language indicated, so no syntax highlighting.\n" +
                "But let's throw in a <b>tag</b>.\n" +
                "```\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Tables\n" +
                "\n" +
                "Colons can be used to align columns.\n" +
                "\n" +
                "| Tables        | Are           | Cool  |\n" +
                "| ------------- |:-------------:| -----:|\n" +
                "| col 3 is      | right-aligned | \$1600 |\n" +
                "| col 2 is      | centered      |   \$12 |\n" +
                "| zebra stripes | are neat      |    \$1 |\n" +
                "\n" +
                "There must be at least 3 dashes separating each header cell.\n" +
                "The outer pipes (|) are optional, and you don't need to make the\n" +
                "raw Markdown line up prettily. You can also use inline Markdown.\n" +
                "\n" +
                "Markdown | Less | Pretty\n" +
                "--- | --- | ---\n" +
                "*Still* | `renders` | **nicely**\n" +
                "1 | 2 | 3\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Blockquotes\n" +
                "\n" +
                "> Blockquotes are very handy in email to emulate reply text.\n" +
                "> This line is part of the same quote.\n" +
                "\n" +
                "Quote break.\n" +
                "\n" +
                "> This is a very long line that will still be quoted properly when it wraps. Oh boy let's keep writing to make sure this is long enough to actually wrap for everyone. Oh, you can *put* **Markdown** into a blockquote.\n" +
                "\n" +
                "Nested quotes\n" +
                "> Hello!\n" +
                ">> And to you!\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Inline HTML\n" +
                "\n" +
                "```html\n" +
                "<u><i>H<sup>T<sub>M</sub></sup><b><s>L</s></b></i></u>\n" +
                "```\n" +
                "\n" +
                "<u><i>H<sup>T<sub>M</sub></sup><b><s>L</s></b></i></u>\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## Horizontal Rule\n" +
                "\n" +
                "Three or more...\n" +
                "\n" +
                "---\n" +
                "\n" +
                "Hyphens (`-`)\n" +
                "\n" +
                "***\n" +
                "\n" +
                "Asterisks (`*`)\n" +
                "\n" +
                "___\n" +
                "\n" +
                "Underscores (`_`)\n" +
                "\n" +
                "\n" +
                "## License\n" +
                "\n" +
                "```\n" +
                "  Copyright 2019 Dimitry Ivanov (legal@noties.io)\n" +
                "\n" +
                "  Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                "  you may not use this file except in compliance with the License.\n" +
                "  You may obtain a copy of the License at\n" +
                "\n" +
                "      http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "  Unless required by applicable law or agreed to in writing, software\n" +
                "  distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "  See the License for the specific language governing permissions and\n" +
                "  limitations under the License.\n" +
                "```\n" +
                "\n" +
                "[cheatsheet]: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet\n" +
                "\n" +
                "[arbitrary case-insensitive reference text]: https://www.mozilla.org\n" +
                "[1]: http://slashdot.org\n" +
                "[link text itself]: http://www.reddit.com"
    }

}
