package com.pixnpunk.audio

import org.koin.dsl.module

val audioModule = module {
    single { AudioPlayerViewModel() }
}
