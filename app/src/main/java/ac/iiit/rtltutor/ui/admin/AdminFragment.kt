package ac.iiit.rtltutor.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.data.UserRepository
import ac.iiit.rtltutor.databinding.FragmentAdminBinding

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private val adapter = StudentsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudents.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.totalUsers.observe(viewLifecycleOwner) { total ->
            binding.tvStatTotal.text = total.toString()
        }

        viewModel.totalSessions.observe(viewLifecycleOwner) { sessions ->
            binding.tvStatSessions.text = sessions.toString()
        }

        viewModel.students.observe(viewLifecycleOwner) { students ->
            adapter.submitList(students)
            binding.tvStatStudents.text = students.size.toString()
            binding.tvAdminSubtitle.text = "${students.size} student${if (students.size == 1) "" else "s"} registered"
            binding.tvEmptyStudents.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
            binding.rvStudents.visibility = if (students.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.btnSignOutAdmin.setOnClickListener {
            UserRepository.logout()
            findNavController().navigate(R.id.action_admin_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
