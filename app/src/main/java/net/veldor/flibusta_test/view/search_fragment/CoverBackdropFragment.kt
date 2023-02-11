package net.veldor.flibusta_test.view.search_fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentCoverBackdropBinding
import net.veldor.flibusta_test.model.delegate.PictureLoadedDelegate
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.view_model.OpdsViewModel

class CoverBackdropFragment : Fragment(), PictureLoadedDelegate {
    private lateinit var viewModel: OpdsViewModel
    private lateinit var target: FoundEntity
    private lateinit var binding: FragmentCoverBackdropBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[OpdsViewModel::class.java]
        binding = FragmentCoverBackdropBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {

    }

    fun setTarget(item: FoundEntity) {
        target = item
        // load picture
        binding.coverLoader.showShimmer(true)
        binding.bookName.text = item.name
        viewModel.downloadPic(item, this)
        binding.coverContainer.setImageDrawable(
            ResourcesCompat.getDrawable(
                App.instance.resources,
                R.drawable.image_wait_load,
                null
            )
        )
    }

    override fun pictureLoaded() {
        requireActivity().runOnUiThread {
            try {
                binding.coverLoader.stopShimmer()
                binding.coverLoader.hideShimmer()
                binding.coverContainer.setImageBitmap(BitmapFactory.decodeFile(target.cover!!.path))
            } catch (_: Throwable) {

            }
        }
    }
}