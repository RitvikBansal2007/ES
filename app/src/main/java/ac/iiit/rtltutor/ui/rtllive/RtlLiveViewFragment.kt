package ac.iiit.rtltutor.ui.rtllive

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.databinding.FragmentRtlLiveViewBinding
import ac.iiit.rtltutor.models.ExperimentState

class RtlLiveViewFragment : Fragment() {

    private var _binding: FragmentRtlLiveViewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RtlLiveViewModel by viewModels()

    private val voltageEntries = mutableListOf<Entry>()
    private var entryIndex = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRtlLiveViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupChart() {
        binding.chartLive.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            axisLeft.textColor = resources.getColor(R.color.text_muted, null)
            axisLeft.gridColor = resources.getColor(R.color.divider, null)
            axisRight.isEnabled = false
            xAxis.textColor = resources.getColor(R.color.text_muted, null)
            xAxis.gridColor = resources.getColor(R.color.divider, null)
            legend.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.isRunning.observe(viewLifecycleOwner) { running ->
            binding.btnToggleSim.text = if (running) getString(R.string.btn_stop_sim)
            else getString(R.string.btn_start_sim)
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { connected ->
            binding.tvLiveStatus.text = if (connected) "Simulator Active" else "Offline"
        }

        viewModel.currentState.observe(viewLifecycleOwner) { state ->
            state ?: return@observe
            updateDataCards(state)
            updateChart(state)
        }
    }

    private fun updateDataCards(state: ExperimentState) {
        binding.tvVoltage.text = String.format("%.2f", state.voltage)
        binding.tvCurrent.text = String.format("%.2f", state.current)
        binding.tvFrequency.text = String.format("%.0f", state.frequency)
    }

    private fun updateChart(state: ExperimentState) {
        voltageEntries.add(Entry(entryIndex++, state.voltage.toFloat()))
        if (voltageEntries.size > 50) voltageEntries.removeAt(0)

        val dataSet = LineDataSet(voltageEntries.toList(), "Voltage").apply {
            color = resources.getColor(R.color.accent_cyan, null)
            setDrawCircles(false)
            lineWidth = 2f
            setDrawFilled(true)
            fillColor = resources.getColor(R.color.accent_cyan_dim, null)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.chartLive.data = LineData(dataSet)
        binding.chartLive.invalidate()
    }

    private fun setupClickListeners() {
        binding.btnToggleSim.setOnClickListener {
            viewModel.toggleSimulation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
