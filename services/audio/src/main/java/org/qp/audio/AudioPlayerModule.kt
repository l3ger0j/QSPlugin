package org.qp.audio

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val audioModule = module {
    singleOf(::AudioPlayerViewModel)
}
