package net.veldor.flibusta_test.ui.filter_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.veldor.flibusta_test.databinding.FragmentFilterRulesBinding

class FilterRulesFragment : Fragment() {
    private lateinit var bindig: FragmentFilterRulesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindig = FragmentFilterRulesBinding.inflate(layoutInflater)
        return bindig.root
    }
}