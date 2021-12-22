package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.horizontalsystems.bankwallet.databinding.FragmentSlideIntroBinding

class IntroSlideFragment : Fragment() {

    private var _binding: FragmentSlideIntroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideIntroBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleResId = requireArguments().getInt(TITLE_KEY)
        val descriptionResId = requireArguments().getInt(DESCRIPTION_KEY)

        binding.title.isVisible = titleResId != 0
        if (titleResId != 0) {
            binding.title.text = getString(titleResId)
        }
        binding.description.text = getString(descriptionResId)
    }

    companion object {
        const val TITLE_KEY = "title_key"
        const val DESCRIPTION_KEY = "description_key"

        @JvmStatic
        fun newInstance(titleResId: Int?, descriptionResId: Int) =
            IntroSlideFragment().apply {
                arguments = Bundle(2).apply {
                    titleResId?.let { putInt(TITLE_KEY, it) }
                    putInt(DESCRIPTION_KEY, descriptionResId)
                }
            }
    }
}
