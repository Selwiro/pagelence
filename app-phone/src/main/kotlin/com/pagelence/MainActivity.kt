package com.pagelence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: PagelenceViewModel by viewModels {
        PagelenceViewModel.factory((application as PagelenceApplication).graph)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PagelenceApp(
                viewModel = viewModel,
                appVersion = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
            )
        }
    }

    /**
     * Coming back is the one place that knows a manual retry is justified:
     * clear the sync backoff and run one automatic round.
     */
    override fun onStart() {
        super.onStart()
        viewModel.onAppForeground()
    }

    /**
     * Activity lifecycle is the only place that knows the app is leaving the
     * foreground. Flush the debounced reader save here; the ViewModel itself
     * has no lifecycle callbacks.
     */
    override fun onStop() {
        viewModel.flushProgress()
        super.onStop()
    }
}
