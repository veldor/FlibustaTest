package net.veldor.flibusta_test.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.veldor.flibusta_test.databinding.ActivityErrorBinding

class ErrorActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }
}