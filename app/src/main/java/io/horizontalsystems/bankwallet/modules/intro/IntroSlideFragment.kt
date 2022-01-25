package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        val imageResId = requireArguments().getInt(IMAGE_KEY)

        if (imageResId != 0) {
            binding.imageView.setImageResource(imageResId)
        }
    }

    companion object {
        const val IMAGE_KEY = "image_key"

        @JvmStatic
        fun newInstance(imageResId: Int) =
            IntroSlideFragment().apply {
                arguments = Bundle(1).apply {
                    putInt(IMAGE_KEY, imageResId)
                }
            }
    }
}
