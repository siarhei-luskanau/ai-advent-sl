package template.core.common

import org.koin.dsl.module

actual val coreCommonModule =
    module {
        single<DispatcherSet> { DispatcherSetAndroid() }
        single<PlatformService> { PlatformServiceAndroid() }
    }
