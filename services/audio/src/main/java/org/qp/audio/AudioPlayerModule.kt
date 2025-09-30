package org.qp.audio

import org.koin.dsl.module

val audioModule = module {
    single { AudioPlayerViewModel() }
}
