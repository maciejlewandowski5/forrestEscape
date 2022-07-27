package com.example.forestescape

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.forestescape.databinding.FragmentNoGameBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.viewmodel.CurrentGameSharedViewModel

class NoGame : Fragment(), Observer<CurrentGame> {
    private lateinit var currentGameSharedViewModelViewModel: CurrentGameSharedViewModel

    private var dummyButton: Button? = null
    private var fullscreenContent: View? = null
    private var fullscreenContentControls: View? = null

    private var _binding: FragmentNoGameBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoGameBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dummyButton = _binding?.dummyButton
        fullscreenContent = _binding?.fullscreenContent
        fullscreenContentControls = _binding?.fullscreenContentControls

        dummyButton?.setOnClickListener() {
            val action = NoGameDirections.actionNoGameToPasswordGame()
            findNavController().navigate(action)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        currentGameSharedViewModelViewModel =
            ViewModelProvider(requireActivity()).get(CurrentGameSharedViewModel::class.java)

        currentGameSharedViewModelViewModel.currentGame.observe(requireActivity(), this)
        currentGameSharedViewModelViewModel.setCurrentGame(CurrentGame.MAP)
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
        Log.i(ContentValues.TAG, "NO GAME OBSERVED CURRENT GAME: $it")
        when (it) {
            CurrentGame.MAP ->
                findNavController().navigate(NoGameDirections.actionNoGameToMapGame())
            CurrentGame.PASSWORD ->
                findNavController().navigate(NoGameDirections.actionNoGameToPasswordGame())
            CurrentGame.NO_GAME -> Unit
            CurrentGame.CHARGE ->
                findNavController().navigate(NoGameDirections.actionNoGameToChargeGame())
            CurrentGame.SCAN ->
                findNavController().navigate(NoGameDirections.actionNoGameToScanGame())
        }
    }
}
