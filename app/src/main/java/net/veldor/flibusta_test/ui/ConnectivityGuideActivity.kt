package net.veldor.flibusta_test.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityConnectivityGuideBinding

class ConnectivityGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectivityGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivityConnectivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.container)
    }
}