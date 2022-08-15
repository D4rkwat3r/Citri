package com.liquid.citri

import android.app.Application
import kamino.Amino
import kamino.NDCLANG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class Citri : Application() {
    val amino: Amino = Amino()
    val scope = CoroutineScope(Dispatchers.Main)
    val ioScope = CoroutineScope(Dispatchers.IO)
    init { amino.lang = NDCLANG.RU }
}