package ac.iiit.rtltutor.ui.progress

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.databinding.FragmentProgressBinding

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRadarChart()
        observeViewModel()
    }

    private fun setupRadarChart() {
        binding.chartRadar.apply {
            description.isEnabled = false
            webColor = resources.getColor(R.color.divider, null)
            webColorInner = resources.getColor(R.color.divider, null)
            webAlpha = 100
            webLineWidth = 1f
            webLineWidthInner = 1f
            legend.isEnabled = false
            xAxis.textColor = resources.getColor(R.color.text_muted, null)
            xAxis.textSize = 10f
            yAxis.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.bloomMastery.observe(viewLifecycleOwner) { mastery ->
            updateRadarChart(mastery)
        }

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile ?: return@observe
            binding.tvProfileSummary.text = buildString {
                append("Sessions completed: ${profile.sessionCount}\n\n")
                append("Strong areas: ${profile.strongTopics.joinToString(", ")}\n\n")
                append("Focus areas: ${profile.weakTopics.joinToString(", ")}")
            }
        }
    }

    private fun updateRadarChart(mastery: Map<Int, Float>) {
        val labels = listOf("Remember", "Understand", "Apply", "Analyze", "Evaluate", "Create")
        val entries = (1..6).map { level ->
            RadarEntry((mastery[level] ?: 0f) * 100f)
        }

        val dataSet = RadarDataSet(entries, "Bloom Mastery").apply {
            color = resources.getColor(R.color.accent_cyan, null)
            fillColor = resources.getColor(R.color.accent_cyan_dim, null)
            setDrawFilled(true)
            lineWidth = 2f
            valueTextColor = Color.TRANSPARENT
        }

        binding.chartRadar.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.chartRadar.data = RadarData(dataSet)
        binding.chartRadar.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
