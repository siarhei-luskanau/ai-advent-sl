package template.core.pref

import org.koin.dsl.module

actual val corePrefModule =
    module {
        single<PrefService> { PrefServiceMemory() }
    }
