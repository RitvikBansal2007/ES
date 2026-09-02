package ac.iiit.rtltutor

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import ac.iiit.rtltutor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Destinations where the bottom nav should be HIDDEN
    private val noNavDestinations = setOf(
        R.id.loginFragment,
        R.id.registerFragment,
        R.id.adminFragment,
        R.id.rtlLiveViewFragment,
        R.id.settingsFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in noNavDestinations) View.GONE else View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()
}
