package com.example.convoassistant

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.convoassistant.databinding.ActivityMainBinding
import com.example.convoassistant.ui.rta.RTAFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.android.*


val gsonParser = Gson()
val httpClient = HttpClient(Android)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_rta, R.id.navigation_practice_mode, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            // Get the current view.
            val navHostFragment = supportFragmentManager.primaryNavigationFragment as NavHostFragment;
            val fragmentManager = navHostFragment.childFragmentManager;
            val currentFragment: Fragment? = fragmentManager.primaryNavigationFragment;

            // Error check.
            if(currentFragment == null) {
                Log.e("Error","Could not find fragment.");
                return false;
            }

            // Toggle recording on the RTA mode screen.
            if(currentFragment is RTAFragment) currentFragment.recordingButtonCallback();

            return true;
        }
        return false;
    }
}