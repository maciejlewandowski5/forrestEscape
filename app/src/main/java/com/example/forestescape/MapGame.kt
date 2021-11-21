package com.example.forestescape

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.forestescape.databinding.FragmentMapGameBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.viewmodel.CurrentGameSharedViewModel
import com.example.forestescape.viewmodel.MapSharedViewModel

class MapGame : Fragment(), Observer<CurrentGame> {
    private lateinit var currentGameSharedViewModelViewModel: CurrentGameSharedViewModel
    private lateinit var mapSharedViewModel: MapSharedViewModel


    private var _binding: FragmentMapGameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapGameBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapSharedViewModel =
            ViewModelProvider(requireActivity()).get(MapSharedViewModel::class.java)
        mapSharedViewModel.azimuth.observe(requireActivity()) {
                _binding?.map?.angle = it
        }
        mapSharedViewModel.location.observe(requireActivity()) {
                _binding?.map?.location = it
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

        println("MAP GAME OBSERVED CURRENT GAME: $it")

        when (it) {
            CurrentGame.MAP -> {
                //do nothing
            }
            CurrentGame.PASSWORD -> {
                findNavController().navigate(MapGameDirections.actionMapGameToPasswordGame())
            }
            CurrentGame.NO_GAME -> {
                findNavController().navigate(MapGameDirections.actionMapGameToNoGame())
            }
            CurrentGame.CHARGE -> {
                findNavController().navigate(MapGameDirections.actionMapGameToChargeGame())
            }
            CurrentGame.SCAN -> {
                findNavController().navigate(MapGameDirections.actionMapGameToScanGame())
            }
        }
    }
}