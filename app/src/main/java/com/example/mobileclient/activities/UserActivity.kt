package com.example.mobileclient.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.mobileclient.R
import com.example.mobileclient.databinding.ActivityUserBinding
import com.example.mobileclient.util.Constants.Companion.USER_INFO_PREFS
import com.example.mobileclient.util.hasPermissions


class UserActivity : AppCompatActivity() {
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
        private const val ACCESS_FINE_LOCATION_CODE = 102
    }

    private lateinit var binding: ActivityUserBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        binding.navigationView.setCheckedItem(R.id.nav_tutorials)
        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }
        binding.navigationView.getHeaderView(0).setOnClickListener {
            navController
                .navigate((R.id.userInfo))
            binding.drawerLayout.close()
        }
        binding.navigationView.setNavigationItemSelectedListener {
            it.isChecked = true
            when {
                it.toString() == getString(R.string.log_out) -> {
                    getSharedPreferences(USER_INFO_PREFS , MODE_PRIVATE).edit().clear().apply()
                    val intent = Intent(this, LandingActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                it.toString() == getString(R.string.settings) -> {
                    binding.navigationView.setCheckedItem(R.id.nav_settings)
                    navController.navigate(R.id.user_settings)
                }
                it.toString() == "User Details" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_user_details)
                    navController.navigate((R.id.medicalInfoMain))
                }
                it.toString() == "Reported Incidents" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_user_incidents)
                    navController.navigate((R.id.incidentsBrowse))
                }
                it.toString() == "Map" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_map)
                    navController.navigate((R.id.facilitiesMap))
                }
                it.toString() == "Tutorials" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_tutorials)
                    navController.navigate((R.id.loggedInScreen))
                }
                it.toString() == "Szczegóły użytkownika" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_user_details)
                    navController.navigate((R.id.medicalInfoMain))
                }
                it.toString() == "Historia zgłoszeń" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_user_incidents)
                    navController.navigate((R.id.incidentsBrowse))
                }
                it.toString() == "Mapa" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_map)
                    navController.navigate((R.id.facilitiesMap))
                }
                it.toString() == "Poradniki" -> {
                    binding.navigationView.setCheckedItem(R.id.nav_tutorials)
                    navController.navigate((R.id.loggedInScreen))
                }

            }
            binding.drawerLayout.close()
            true
        }
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        if (!hasPermissions(this, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        }
    }

    override fun recreate() {
        super.recreate()
        binding.navigationView.setCheckedItem(R.id.nav_tutorials)

    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isOpen) {
            binding.drawerLayout.close()
        } else {
            findNavController(R.id.fragmentContainerView).navigateUp()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            ACCESS_FINE_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}