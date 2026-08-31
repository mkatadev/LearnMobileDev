package pl.prodevcode.learnmobiledev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.data.preferences.AndroidKeyValueStore
import pl.prodevcode.learnmobiledev.data.preferences.AndroidPlatformLocale
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.PlatformLocale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before anything reads a resource: Compose Resources resolves strings against
        // Locale.getDefault(), and a fresh process always starts from the system locale.
        AndroidPlatformLocale.restore(this)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // The platform module is passed explicitly, so Context never leaks into common code.
            App(
                platformModule = module {
                    single<KeyValueStore> { AndroidKeyValueStore(applicationContext) }
                    single<PlatformLocale> { AndroidPlatformLocale(applicationContext) }
                },
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
