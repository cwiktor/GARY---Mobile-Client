package com.example.mobileclient.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.example.mobileclient.R
import com.example.mobileclient.adapter.TutorialsAdapter
import com.example.mobileclient.databinding.FragmentLoggedInScreenBinding
import com.example.mobileclient.model.Review
import com.example.mobileclient.model.Tutorial
import com.example.mobileclient.util.Constants.Companion.USER_EMAIL_TO_PREFS
import com.example.mobileclient.util.Constants.Companion.USER_INFO_PREFS
import com.example.mobileclient.util.Constants.Companion.USER_TOKEN_TO_PREFS
import com.example.mobileclient.util.Constants.Companion.tutorialsEmpty
import com.example.mobileclient.viewmodels.TutorialsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoggedInScreen : Fragment(), AdapterView.OnItemSelectedListener,
    TutorialsAdapter.OnItemClickListener {
    private var _binding: FragmentLoggedInScreenBinding? = null
    private val tutorialsViewModel: TutorialsViewModel by activityViewModels()
    private var tutorialsFromAPI: List<Tutorial>? = null
    private var currentlyDisplayedTutorials: List<Tutorial>? = null
    private var userEmail = ""

    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoggedInScreenBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.tutorialsGrid.adapter =
            TutorialsAdapter(requireContext(), tutorialsEmpty, this, ratingBarChangeListener,false)
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences(USER_INFO_PREFS, Context.MODE_PRIVATE)
        val token: String = sharedPreferences.getString(USER_TOKEN_TO_PREFS, "")!!
        userEmail = sharedPreferences.getString(USER_EMAIL_TO_PREFS, "")!!
        getTutorialsFromAPI()
        binding.refresh.setOnRefreshListener {
            getTutorialsFromAPI()
            binding.refresh.isRefreshing = false
        }
        if (requireActivity().getPreferences(Context.MODE_PRIVATE)
                .getBoolean("createIncidentON", false)
        ) {
            binding.addIncidentButton.hide()
        }
        binding.addIncidentButton.setOnClickListener {
            context?.let { it1 ->
                MaterialAlertDialogBuilder(it1).setTitle(getString(R.string.create_incident_popup_title))
                    .setMessage(getString(R.string.create_incident_popup_alert))
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton(getString(R.string.accept)) { _, _ ->
                        Navigation.findNavController(view)
                            .navigate(R.id.action_loggedInScreen_to_incident)
                    }
                    .show()
            }
        }
        //Filter menu
        val filterMenu: Spinner = binding.filterMenu
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filterTutorialsArray,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            filterMenu.adapter = adapter
        }
        filterMenu.onItemSelectedListener = this
        return view
    }

    //Methods for filterMenu from AdapterView.OnItemSelectedListener
    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        val stringArray = resources.getStringArray(R.array.filterTutorialsArray)
        val allTutorials = stringArray[0]
        val general = stringArray[1]
        val inCaseOfDeath = stringArray[2]
        val course = stringArray[3]
        when (p0?.getItemAtPosition(p2).toString()) {
            allTutorials -> {
                currentlyDisplayedTutorials = checkIfTutorialsAvailable()
                (binding.tutorialsGrid.adapter as TutorialsAdapter).setTutorials(currentlyDisplayedTutorials!!)
                binding.tutorialsGrid.adapter?.notifyDataSetChanged()
            }
            general -> {
                val filteredEmergenceTutorials =
                    checkIfTutorialsAvailable().filter { it.tutorialType == "IN_CASE_OF_DEATH_EMERGENCY" }
                currentlyDisplayedTutorials = filteredEmergenceTutorials
                (binding.tutorialsGrid.adapter as TutorialsAdapter).setTutorials(currentlyDisplayedTutorials!!)
                binding.tutorialsGrid.adapter?.notifyDataSetChanged()
            }
            inCaseOfDeath -> {
                val filteredInCaseOfDeathTutorials =
                    checkIfTutorialsAvailable().filter { it.tutorialType == "GENERAL" }
                currentlyDisplayedTutorials = filteredInCaseOfDeathTutorials
                (binding.tutorialsGrid.adapter as TutorialsAdapter).setTutorials(currentlyDisplayedTutorials!!)
                binding.tutorialsGrid.adapter?.notifyDataSetChanged()
            }
            course -> {
                val filteredCourseTutorials =
                    checkIfTutorialsAvailable().filter { it.tutorialType == "COURSE" }
                currentlyDisplayedTutorials = filteredCourseTutorials
                (binding.tutorialsGrid.adapter as TutorialsAdapter).setTutorials(currentlyDisplayedTutorials!!)
                binding.tutorialsGrid.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        currentlyDisplayedTutorials = checkIfTutorialsAvailable()
        (binding.tutorialsGrid.adapter as TutorialsAdapter).setTutorials(currentlyDisplayedTutorials!!)
        binding.tutorialsGrid.adapter?.notifyDataSetChanged()
    }

    override fun onItemClick(position: Int) {
        Log.d("Tutorial clicked", "User clicked tutorial $position")
        val tutorial = currentlyDisplayedTutorials?.get(position)
        tutorialsViewModel.pickedTutorial = tutorial
        //navigate to tutorial html view
        Navigation.findNavController(binding.root)
            .navigate(R.id.action_loggedInScreen_to_tutorialHtmlView)
    }

    private fun getTutorialsFromAPI() {
        tutorialsViewModel.getTutorials()
        tutorialsViewModel.getTutorialsResponse.observe(viewLifecycleOwner) { response ->
            if (response.code() == 200) {
                tutorialsFromAPI = response.body()
                currentlyDisplayedTutorials = checkIfTutorialsAvailable()
                (binding.tutorialsGrid.adapter as TutorialsAdapter).setTutorials(currentlyDisplayedTutorials!!)
                binding.tutorialsGrid.adapter?.notifyDataSetChanged()
                binding.filterMenu.setSelection(0)
            } else {
                Log.d("getTutorialsResponseCode", response.code().toString())
            }
        }
    }

    private val ratingBarChangeListener =
        RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
            if (fromUser) {
                val tag = ratingBar.tag.toString().toInt()
                val ratingForAPI = Review(rating.toDouble(), "")
                tutorialsViewModel.addTutorialRating(tag, userEmail, ratingForAPI)
                tutorialsViewModel.addTutorialRatingResponse.observe(viewLifecycleOwner) { response ->
                    if (response.code() == 200) {
                        Log.d("addTutorialRatingResponse", response.code().toString())
                        ratingBar.rating = rating
                    } else {
                        Log.d("addTutorialRatingResponse", response.code().toString())
                    }
                }
            }
        }

    private fun checkIfTutorialsAvailable(): List<Tutorial> {
        if (tutorialsFromAPI.isNullOrEmpty()) {
            return tutorialsEmpty
        }
        return tutorialsFromAPI!!
    }
}