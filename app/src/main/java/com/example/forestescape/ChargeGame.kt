package com.example.forestescape

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.forestescape.databinding.FragmentChargeGameBinding
import com.example.forestescape.model.CurrentGame
import com.example.forestescape.viewmodel.ChargeGameViewModel
import com.example.forestescape.viewmodel.CurrentGameSharedViewModel
import android.view.animation.AlphaAnimation
import com.example.forestescape.viewmodel.LightSharedViewModel


class ChargeGame : Fragment(), Observer<CurrentGame> {
    private lateinit var currentGameSharedViewModelViewModel: CurrentGameSharedViewModel
    private lateinit var chargeGameViewModel: ChargeGameViewModel
    private lateinit var lightSharedViewModel: LightSharedViewModel

    private var _binding: FragmentChargeGameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentChargeGameBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 50
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE

        lightSharedViewModel =
            ViewModelProvider(requireActivity()).get(LightSharedViewModel::class.java)
        chargeGameViewModel =
            ViewModelProvider(requireActivity()).get(ChargeGameViewModel::class.java)
        lightSharedViewModel.light.observe(requireActivity()) {
            if (it > BASE_LIGHT_INTENSITY + CHARGE_LIGHT_INTENSITY) {
                chargeGameViewModel.startCharge()
            } else {
                chargeGameViewModel.stopCharge()
            }
        }
        chargeGameViewModel.isCharging.observe(requireActivity()) { isCharging ->
            if (isCharging == false) {
                binding.zero.clearAnimation()
                binding.one.clearAnimation()
                binding.two.clearAnimation()
                binding.three.clearAnimation()
                binding.four.clearAnimation()
            }
        }

        chargeGameViewModel.charge.observe(requireActivity()) { charge ->
            when {
                charge < 20 -> {
                    binding.zero.visibility = VISIBLE
                    binding.zero.startAnimation(anim)
                }
                charge in 20..39 -> {
                    binding.zero.clearAnimation()
                    binding.zero.visibility = VISIBLE
                    binding.one.visibility = VISIBLE
                    binding.one.startAnimation(anim)
                }
                charge in 40..59 -> {
                    binding.one.clearAnimation()
                    binding.zero.visibility = VISIBLE
                    binding.one.visibility = VISIBLE
                    binding.two.visibility = VISIBLE
                    binding.two.startAnimation(anim)
                }
                charge in 60..79 -> {
                    binding.two.clearAnimation()
                    binding.zero.visibility = VISIBLE
                    binding.one.visibility = VISIBLE
                    binding.two.visibility = VISIBLE
                    binding.three.visibility = VISIBLE
                    binding.three.startAnimation(anim)
                }
                charge in 80..99 -> {
                    binding.three.clearAnimation()
                    binding.zero.visibility = VISIBLE
                    binding.one.visibility = VISIBLE
                    binding.two.visibility = VISIBLE
                    binding.three.visibility = VISIBLE
                    binding.four.visibility = VISIBLE
                    binding.four.startAnimation(anim)
                }
                charge >= 99 -> {
                    binding.four.clearAnimation()
                    binding.zero.visibility = VISIBLE
                    binding.one.visibility = VISIBLE
                    binding.two.visibility = VISIBLE
                    binding.three.visibility = VISIBLE
                    binding.four.visibility = VISIBLE
                }
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

        println("MAP GAME OBSERVED CURRENT GAME: $it")

        when (it) {
            CurrentGame.MAP -> {
                findNavController().navigate(ChargeGameDirections.actionChargeGameToMapGame())
            }
            CurrentGame.PASSWORD -> {
                findNavController().navigate(ChargeGameDirections.actionChargeGameToPasswordGame())
            }
            CurrentGame.NO_GAME -> {
                findNavController().navigate(ChargeGameDirections.actionChargeGameToNoGame())
            }
            CurrentGame.CHARGE -> {
                //do nothing
            }
            CurrentGame.SCAN -> {
                findNavController().navigate(ChargeGameDirections.actionChargeGameToScanGame())
            }
        }
    }

    companion object {
        private const val BASE_LIGHT_INTENSITY = 200L
        private const val CHARGE_LIGHT_INTENSITY = 500L
    }
}