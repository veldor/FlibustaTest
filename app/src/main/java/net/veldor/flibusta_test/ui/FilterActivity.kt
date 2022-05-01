package net.veldor.flibusta_test.ui

import android.os.Bundle
import net.veldor.flibusta_test.databinding.ActivityFilterBinding

class FilterActivity : BaseActivity() {
    private lateinit var binding: ActivityFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
    }
}