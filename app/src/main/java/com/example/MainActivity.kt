package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.ScreenState
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ListScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NoteRepository(database.noteDao())

        // 2. Instantiate MainViewModel cleanly using Factory
        viewModel = ViewModelProvider(this, MainViewModelFactory(repository))[MainViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val screenState by viewModel.screenState.collectAsState()

                    // Premium animated sliding transition between screens
                    AnimatedContent(
                        targetState = screenState,
                        transitionSpec = {
                            if (targetState is ScreenState.EditorScreen) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "ScreenTransition"
                    ) { activeScreen ->
                        when (activeScreen) {
                            is ScreenState.ListScreen -> {
                                ListScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            is ScreenState.EditorScreen -> {
                                EditorScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            is ScreenState.ArchiveScreen -> {
                                ArchiveScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            is ScreenState.TrashScreen -> {
                                com.example.ui.screens.TrashScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::viewModel.isInitialized) {
            viewModel.saveCurrentEditorContentImmediately()
        }
    }
}
