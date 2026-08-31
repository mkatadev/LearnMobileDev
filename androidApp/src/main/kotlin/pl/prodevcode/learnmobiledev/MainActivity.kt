package pl.prodevcode.learnmobiledev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.prodevcode.learnmobiledev.di.DEVICE_LANGUAGE
import pl.prodevcode.learnmobiledev.data.preferences.AndroidKeyValueStore
import pl.prodevcode.learnmobiledev.data.preferences.AndroidLanguageProvider
import pl.prodevcode.learnmobiledev.domain.repository.KeyValueStore
import pl.prodevcode.learnmobiledev.domain.repository.LanguageProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // The platform module is passed explicitly, so Context never leaks into common code.
            App(
                platformModule = module {
                    single<KeyValueStore> { AndroidKeyValueStore(applicationContext) }
                    single<LanguageProvider>(named(DEVICE_LANGUAGE)) {
                        AndroidLanguageProvider()
                    }
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
