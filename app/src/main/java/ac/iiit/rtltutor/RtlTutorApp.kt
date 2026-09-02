package ac.iiit.rtltutor

import android.app.Application
import ac.iiit.rtltutor.data.UserRepository

/**
 * Application class — runs before any Activity.
 * Used to initialize UserRepository with application context so it can
 * access EncryptedSharedPreferences throughout the app lifecycle.
 */
class RtlTutorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize persistent encrypted user store on a background thread.
        // BCrypt hashing during admin bootstrap can take ~300ms — keep main thread free.
        Thread {
            UserRepository.init(this)
        }.start()
    }
}
