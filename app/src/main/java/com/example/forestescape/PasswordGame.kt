package com.example.forestescape

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.forestescape.databinding.FragmentPasswordGameBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.viewmodel.CurrentGameSharedViewModel
import com.example.forestescape.viewmodel.PasswordGameViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class PasswordGame : Fragment(), Observer<CurrentGame> {
    private lateinit var currentGameSharedViewModelViewModel: CurrentGameSharedViewModel
    private lateinit var passwordGameViewModel: PasswordGameViewModel

    private var _binding: FragmentPasswordGameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPasswordViewModel(view)
        buttonsListeners()

    }

    private fun initPasswordViewModel(view: View) {
        val correctSnackbar = Snackbar.make(
            view,
            "Correct password",
            BaseTransientBottomBar.LENGTH_LONG
        )

        val uncorrectSnackbar = Snackbar.make(
            view,
            "Uncorrect password",
            BaseTransientBottomBar.LENGTH_LONG
        )
        passwordGameViewModel =
            ViewModelProvider(requireActivity()).get(PasswordGameViewModel::class.java)
        passwordGameViewModel.isInputCorrect.observe(requireActivity()) {
            if (it) {
                correctSnackbar.show()
            } else {
                uncorrectSnackbar.show()
            }
        }
        passwordGameViewModel.password.observe(requireActivity()) {
            binding.linearLayout.removeAllViews()
            it.forEach { char -> addViewFromInputValue(char) }
        }
    }

    private fun buttonsListeners() {
        binding.imageButton1.setOnClickListener { passwordGameViewModel.addLetter(1) }
        binding.imageButton2.setOnClickListener { passwordGameViewModel.addLetter(2) }
        binding.imageButton3.setOnClickListener { passwordGameViewModel.addLetter(3) }
        binding.imageButton4.setOnClickListener { passwordGameViewModel.addLetter(4) }
        binding.imageButton5.setOnClickListener { passwordGameViewModel.addLetter(5) }
        binding.imageButton6.setOnClickListener { passwordGameViewModel.addLetter(6) }
        binding.imageButton7.setOnClickListener { passwordGameViewModel.addLetter(7) }
        binding.imageButton8.setOnClickListener { passwordGameViewModel.addLetter(8) }
        binding.imageButton9.setOnClickListener { passwordGameViewModel.addLetter(9) }
    }

    private fun addViewFromInputValue(char: Int) {
        val imageView = ImageView(requireContext())
        resolveInputChar(char, imageView)
        binding.linearLayout.addView(imageView)
    }

    private fun resolveInputChar(char: Int, imageView: ImageView) {
        when (char) {
            1 -> {
                imageView.setImageResource(R.drawable.password_char_1)
            }
            2 -> {
                imageView.setImageResource(R.drawable.password_char_2)
            }
            3 -> {
                imageView.setImageResource(R.drawable.password_char_3)
            }
            4 -> {
                imageView.setImageResource(R.drawable.password_char_4)
            }
            5 -> {
                imageView.setImageResource(R.drawable.password_char_5)
            }
            6 -> {
                imageView.setImageResource(R.drawable.password_char_6)
            }
            7 -> {
                imageView.setImageResource(R.drawable.password_char_7)
            }
            8 -> {
                imageView.setImageResource(R.drawable.password_char_8)
            }
            9 -> {
                imageView.setImageResource(R.drawable.password_char_9)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        currentGameSharedViewModelViewModel =
            ViewModelProvider(requireActivity()).get(CurrentGameSharedViewModel::class.java)

        currentGameSharedViewModelViewModel.currentGame.observe(requireActivity(), this)
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        currentGameSharedViewModelViewModel.currentGame.removeObserver(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onChanged(it: CurrentGame?) {

        println("PASSWORD GAME OBSERVED CURRENT GAME: $it")
        when (it) {
            CurrentGame.MAP -> {
                val action = PasswordGameDirections.actionPasswordGameToMapGame()
                findNavController().navigate(action)
            }
            CurrentGame.PASSWORD -> {
                //do nothing
            }
            CurrentGame.NO_GAME -> {
                val action = PasswordGameDirections.actionPasswordGameToNoGame()
                findNavController().navigate(action)
            }
            CurrentGame.CHARGE -> {
                findNavController().navigate(PasswordGameDirections.actionPasswordGameToChargeGame())
            }
            CurrentGame.SCAN -> {
                findNavController().navigate(PasswordGameDirections.actionPasswordGameToScanGame())
            }
        }
    }
}