package template.core.pref

import okio.Path

interface PrefPathProvider {
    fun get(): Path
}
