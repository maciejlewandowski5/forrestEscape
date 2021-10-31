package com.example.forestescape

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.forestescape.databinding.FragmentPasswordGameBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.viewmodel.CurrentGameSharedViewModel

class PasswordGame : Fragment(), Observer<CurrentGame> {
    private lateinit var currentGameSharedViewModelViewModel: CurrentGameSharedViewModel

    private var dummyButton: Button? = null
    private var fullscreenContent: View? = null
    private var fullscreenContentControls: View? = null

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
        dummyButton = binding.dummyButton
        fullscreenContent = binding.fullscreenContent
        fullscreenContentControls = binding.fullscreenContentControls

        dummyButton?.setOnClickListener {
            dummyButton?.setOnClickListener {
                val action = PasswordGameDirections.actionPasswordGameToNoGame()
                findNavController().navigate(action)
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

    override fun onDestroy() {
        super.onDestroy()
        dummyButton = null
        fullscreenContent = null
        fullscreenContentControls = null
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

            }
            CurrentGame.SCAN -> {

            }
        }
    }
}