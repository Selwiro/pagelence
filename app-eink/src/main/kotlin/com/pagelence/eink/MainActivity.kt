package com.pagelence.eink

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.pagelence.PagelenceViewModel

/**
 * 墨水屏入口。Activity 只做三件事：装配 ViewModel、接生命周期落盘、提供全刷接入口。
 *
 * 全刷目前走 [NoopEinkRefresher]：真实设备需要接厂商 SDK（ONYX/Boox 等）的
 * `refreshScreen`，接入点就是 [refresher]，见 docs/app-eink.md「剩下的部分」。
 */
class MainActivity : ComponentActivity() {
    private val viewModel: PagelenceViewModel by viewModels {
        PagelenceViewModel.factory((application as EinkApplication).graph)
    }

    private val refresher: EinkRefresher = NoopEinkRefresher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 墨水屏默认浅色：状态栏与导航栏也用白色，避免深色条在整页刷新时留下残影。
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        // 白底白图标等于没有状态栏：把系统图标切回深色。
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            EinkApp(
                viewModel = viewModel,
                appVersion = BuildConfig.VERSION_NAME,
                onFullRefresh = refresher::fullRefresh,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForeground()
    }

    /** 唯一的「即将离开前台」信号：停计时并强制落盘。 */
    override fun onStop() {
        viewModel.onAppBackground()
        viewModel.flushProgress()
        super.onStop()
    }
}
