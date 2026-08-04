package com.github.rar91279.plugin.tapestry.lang

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

/**
 * @author Alexey Chmutov
 */
class TelLanguage private constructor() : Language("TEL"), InjectableLanguage {
    override fun getAssociatedFileType(): TelFileType {
        return TelFileType
    }

    companion object {
        @JvmField
        val INSTANCE: TelLanguage = TelLanguage()
    }
}
