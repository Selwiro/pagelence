package com.pagelence.eink

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.pagelence.PagelenceViewModel

/**
 * 墨水屏入口。Activity 只做四件事：装配 ViewModel、接生命周期落盘、提供全刷
 * 接入口、把状态栏/导航栏刷成白底深色图标。
 *
 * 全刷走后端探测：墨案系设备发 `android.eink.force.refresh`，其余机型退化为
 * 应用层重绘（[NoopEinkRefresher]），绝不因为缺少厂商通道而崩溃。
 */
class MainActivity : ComponentActivity() {
    private val viewModel: PagelenceViewModel by viewModels {
        PagelenceViewModel.factory((application as EinkApplication).graph)
    }

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
                onFullRefresh = ::fullRefresh,
            )
        }
    }

    /** 每次按当前偏好选后端：用户可能在设置页切换过「强制广播」。 */
    private fun fullRefresh() {
        EinkRefreshers.create(
            context = this,
            forceMoaanBroadcast = EinkDeviceSettings.forceMoaanBroadcast(this),
        ).fullRefresh()
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
