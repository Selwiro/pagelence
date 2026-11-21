package com.pagelence

import android.app.Application

class PagelenceApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
