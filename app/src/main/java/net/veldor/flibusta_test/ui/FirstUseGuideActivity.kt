package net.veldor.flibusta_test.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityFirstUseBinding

class FirstUseGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirstUseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivityFirstUseBinding.inflate(layoutInflater)
        setContentView(binding.container)
    }
}