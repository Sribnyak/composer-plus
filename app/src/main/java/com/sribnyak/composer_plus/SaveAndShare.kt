package com.sribnyak.composer_plus

object SaveAndShare {
    private const val BC = '@'
    fun makeText(body: String): String {
        return "$BC Composer Plus v${BuildConfig.VERSION_NAME}\n$BC tempo=${Sound.tempo}\n$body"
    }
}