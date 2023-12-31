package com.example.mobileclient.fragments

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.mobileclient.R
import com.example.mobileclient.databinding.FragmentParamedicScreenBinding
import com.example.mobileclient.model.Incident
import com.example.mobileclient.util.Constants.Companion.USER_INFO_PREFS
import com.example.mobileclient.util.Constants.Companion.USER_TOKEN_TO_PREFS
import com.example.mobileclient.util.findNextShift
import com.example.mobileclient.util.setIncidentTypeFromApi
import com.example.mobileclient.viewmodels.ParamedicViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import retrofit2.Response
import java.lang.RuntimeException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ParamedicScreen : Fragment() {
    private var _binding: FragmentParamedicScreenBinding? = null
    private var mLocationOverlay: MyLocationNewOverlay? = null
    private val paramedicViewModel: ParamedicViewModel by activityViewModels()
    private lateinit var map: MapView
    private val binding get() = _binding!!
    private var ambulance: String = "AAA000"
    var waypoints: ArrayList<GeoPoint> = ArrayList()
    var roadOverlay: Polyline = Polyline()
    var firstRoadDraw: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParamedicScreenBinding.inflate(inflater, container, false)
        val view = binding.root
        map = binding.map
        binding.shiftButton.visibility = View.GONE
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val formatted = current.format(formatter)
        binding.dayField.text = formatted
        //get token from shared preferences
        val token = requireActivity().getSharedPreferences(USER_INFO_PREFS, Context.MODE_PRIVATE)
            .getString(USER_TOKEN_TO_PREFS, "")
        Log.d("token", token.toString())
        paramedicViewModel.getCurrentAmbulance(token ?: "")

        paramedicViewModel.currentAmbulanceResponse.observe(viewLifecycleOwner) {
            Log.d("AMBULANCE", it.toString())
            if (it.isSuccessful) {
                val licensePlate = it.body()?.licensePlate
                if (licensePlate != null) {
                    ambulance = licensePlate
                    paramedicViewModel.getAmbulanceEquipment(ambulance, token ?: "")
                    paramedicViewModel.getAssignedIncident(ambulance)
                }
            } else {
                binding.currentEmergencyLabel.text = getString(R.string.no_ambulance)
            }
        }
        paramedicViewModel.getSchedule(token ?: "")
        paramedicViewModel.scheduleResponse.observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                Log.d("Schedule", response.body()!!.schedule.toString())
                if (response.body()?.schedule != null) {
                    val scheduleForToday = findNextShift(response.body()!!.schedule!!)
                    val endString = getString(R.string.end_shift)
                    if (scheduleForToday.isNotEmpty()) {
                        val textToDisplay =
                            "Start - ${scheduleForToday[0]} $endString - ${scheduleForToday[1]}"
                        binding.nearestShiftField.text = textToDisplay
                    } else {
                        binding.nearestShiftField.text = getString(R.string.no_shifts)
                    }
                } else {
                    binding.nearestShiftField.text = getString(R.string.no_shifts)
                }
            }
        }


        binding.checkinButton.setOnClickListener {
            when (binding.checkinButton.text) {
                getString(R.string.ParamedicScreen_CheckIn) -> {
                    paramedicViewModel.startEmployeeShift(token ?: "")
                    paramedicViewModel.employeeShiftResponse.observe(viewLifecycleOwner) { response ->
                        if (response.isSuccessful) {
                            binding.checkinButton.text =
                                getString(R.string.ParamedicScreen_FinishShift)
                            binding.checkinButton.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.red
                                )
                            )
                        }
                    }
                    binding.checkinButton.text = getString(R.string.ParamedicScreen_FinishShift)
                    binding.checkinButton.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.red
                        )
                    )
                    binding.shiftButton.visibility = View.VISIBLE
                    binding.cardView.visibility = View.GONE
                }

                else -> {
                    paramedicViewModel.endEmployeeShift(token ?: "")
                    //Add message box to confirm end of shift
                    paramedicViewModel.employeeShiftResponse.observe(viewLifecycleOwner) { response ->
                        if (response.isSuccessful) {
                            binding.checkinButton.text = getString(R.string.ParamedicScreen_CheckIn)
                            binding.checkinButton.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.green_dark
                                )
                            )
                        }
                    }
                    binding.checkinButton.text = getString(R.string.ParamedicScreen_CheckIn)
                    binding.checkinButton.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.green_dark
                        )
                    )
                    binding.shiftButton.visibility = View.VISIBLE
                    binding.cardView.visibility = View.GONE
                }
            }
        }

        binding.shiftButton.setOnClickListener {
            binding.cardView.visibility = View.VISIBLE
            it.visibility = View.GONE
        }
        binding.cardView.setOnClickListener {
            binding.cardView.visibility = View.GONE
            binding.shiftButton.visibility = View.VISIBLE
        }
        val navController: NavController =
            Navigation.findNavController(requireActivity(), R.id.fragmentContainerView)
        binding.bottomNavigation.setOnItemSelectedListener {
            it.isChecked = true
            when (it.toString()) {
                resources.getString(R.string.menu_equipment) -> {
                    it.isChecked = true
                    navController.navigate(R.id.checkEquipment)
                }

                resources.getString(R.string.menu_victim) -> {
                    it.isChecked = true
                    navController.navigate(R.id.addVictimInfo)
                }

                resources.getString(R.string.menu_support) -> {
                    it.isChecked = true
                    navController.navigate(R.id.paramedicCallForSupport2)
                }

                else -> {
                    it.isChecked = true
                    navController.navigate(R.id.paramedicScreen)
                }
            }
            true
        }
        val color: Int = ContextCompat.getColor(requireContext(), R.color.green_light)
        paramedicViewModel.assignedIncidentResponse.observe(viewLifecycleOwner) { response ->
            Log.d("AssignedIncident", response.body().toString())
            if (response.code() == 200) {
                setIncidentFields(response)
                map.setTileSource(TileSourceFactory.MAPNIK)
                map.controller.setZoom(15)
                map.setMultiTouchControls(true)
                map.setBuiltInZoomControls(true)
                val gpsProvider = GpsMyLocationProvider(requireContext())
                gpsProvider.locationUpdateMinTime = 10000
                mLocationOverlay = MyLocationNewOverlay(gpsProvider, map)
                mLocationOverlay!!.enableMyLocation()
                mLocationOverlay!!.enableFollowLocation()
                val incidentMarker = Marker(map)
                incidentMarker.position = GeoPoint(
                    response.body()!!.accidentReport.location.latitude,
                    response.body()!!.accidentReport.location.longitude
                )
                incidentMarker.title = getString(R.string.incident)
                incidentMarker.icon = ContextCompat.getDrawable(
                    requireContext(), R.drawable.ic_baseline_warning_24
                )
                var ambulanceMarker = Marker(map)
                ambulanceMarker.title = "Ambulance"
                ambulanceMarker.icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ambulance_icon)
                mLocationOverlay!!.runOnFirstFix {
                    ambulanceMarker.position = mLocationOverlay!!.myLocation
                    waypoints.add(ambulanceMarker.position)
                    waypoints.add(incidentMarker.position)
                    map.overlays.add(ambulanceMarker).also { map.overlays.add(incidentMarker) }
                    mLocationOverlay!!.myLocationProvider.startLocationProvider { location, _ ->
                        val currentLocation = com.example.mobileclient.model.Location(
                            location.latitude, location.longitude
                        )
                        paramedicViewModel.updateAmbulanceLocation(ambulance, currentLocation)
                        Log.d(
                            "Location update",
                            paramedicViewModel.updateAmbulanceInfoResponse.value.toString()
                        )
                        val roadManager: RoadManager = OSRMRoadManager(requireContext(), "Gary")
                        waypoints.remove(ambulanceMarker.position)
                        Log.d("waypoints after remove", waypoints.toString())
                        map.overlayManager.remove(ambulanceMarker)
                        ambulanceMarker.position = GeoPoint(location.latitude, location.longitude)
                        waypoints.add(ambulanceMarker.position)
                        Log.d("waypoints after add", waypoints.toString())
                        map.overlayManager.add(ambulanceMarker)
                        map.overlays.remove(roadOverlay)
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val road = roadManager.getRoad(waypoints)
                                roadOverlay = RoadManager.buildRoadOverlay(road, color, 12f)
                                Log.d("Road creation", "Road created for $waypoints")
                                map.overlays.add(roadOverlay)
                                map.postInvalidate()
                            }
                        }
                        map.controller.animateTo(ambulanceMarker.position)
                    }

                }
            } else {
                binding.currentEmergencyLabel.text = getString(R.string.no_emergency)
            }
        }
        map.invalidate()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setIncidentFields(response: Response<Incident>) {
        val incidentTypes: Array<String> =
            requireContext().resources.getStringArray(R.array.incidentTypes)
        val incident = response.body()
        val accidentReport = incident!!.accidentReport
        Log.d("AccidentReport", accidentReport.toString())
        binding.currentEmergencyTypeText.text =
            setIncidentTypeFromApi(accidentReport.emergencyType, incidentTypes)
        val accidentTime = LocalDateTime.parse(accidentReport.date)
        val formattedTime = accidentTime.hour.toString() + ":" + accidentTime.minute.toString()
        binding.currentEmergencyTimeText.text = formattedTime
        binding.currentEmergencyTimeText.invalidate()
        val accidentAddress = accidentReport.address
        val splitAddress = accidentAddress.split(", ")
        val formattedAddress = splitAddress[0] + ", " + splitAddress[1] + ", " + splitAddress[2]
        binding.currentEmergencyAddressText.text = formattedAddress
        binding.currentEmergencyVictimsText.text = accidentReport.victimCount.toString()
        var victimState = ""
        if (accidentReport.breathing) {
            victimState += (getString(R.string.breathingVictim))
        } else {
            victimState += (getString(R.string.notBreathingVictim))
            victimState += (", ")
        }
        if (accidentReport.consciousness) {
            victimState += (getString(R.string.conciousVictim))
        } else {
            victimState += (getString(R.string.unconciousVictim))
        }
        binding.currentEmergencyVictimsStateText.text = victimState
        if (accidentReport.description == "") {
            binding.showIncidentDescriptionButton.visibility = View.GONE
        } else {
            binding.showIncidentDescriptionButton.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.description))
                    .setMessage(accidentReport.description)
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }.setPositiveButton("Ok") { dialog, _ ->
                        dialog.cancel()
                    }.show()
            }
        }
        if (accidentReport.bandCode == "" || accidentReport.bandCode == null) {
            binding.showVictimInfoButton.visibility = View.GONE
        } else {
            //TODO("Show victim info from bandcode API")
            paramedicViewModel.getMedicalInfoWithBandCode(accidentReport.bandCode)
        }
        Log.d("AccidentReport", "emergency text updated")
        binding.linearLayout9.invalidate()
    }

    override fun onDetach() {
        super.onDetach()
        map.onDetach()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}