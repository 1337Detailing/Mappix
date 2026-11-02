package io.github.mappix.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mappix.BuildConfig
import io.github.mappix.R
import io.github.mappix.data.update.GithubUpdateRepository
import io.github.mappix.data.update.UpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: UpdateInfo) : UpdateState
    data class Downloading(val update: UpdateInfo, val progress: Int) : UpdateState
    data class Ready(val update: UpdateInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: GithubUpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val version = BuildConfig.VERSION_NAME
    val repositoryName = BuildConfig.UPDATE_REPOSITORY
    val configured = repository.configured
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = _state.asStateFlow()

    fun check() {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        viewModelScope.launch {
            _state.value = UpdateState.Checking
            try {
                _state.value = repository.check()?.let { UpdateState.Available(it) } ?: UpdateState.UpToDate
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = UpdateState.Error(readableError(e)) }
        }
    }

    fun download() {
        val available = _state.value as? UpdateState.Available ?: return
        viewModelScope.launch {
            _state.value = UpdateState.Downloading(available.update, 0)
            try {
                val file = repository.download(available.update) { progress ->
                    _state.value = UpdateState.Downloading(available.update, progress)
                }
                _state.value = UpdateState.Ready(available.update, file)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = UpdateState.Error(readableError(e)) }
        }
    }

    private fun readableError(error: Exception): String = when (error.message) {
        "update_http_404" -> context.getString(R.string.update_no_releases)
        "update_apk_missing" -> context.getString(R.string.update_no_apk)
        "update_checksum_missing", "update_checksum_invalid", "update_checksum_mismatch" ->
            context.getString(R.string.update_integrity_problem)
        "update_wrong_package" -> context.getString(R.string.update_package_problem)
        else -> error.localizedMessage ?: context.getString(R.string.update_unknown_error)
    }

    fun install() {
        val ready = _state.value as? UpdateState.Ready ?: return
        try {
            val intent = if (repository.canInstall()) repository.installIntent(ready.file)
                else repository.permissionIntent()
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) { _state.value = UpdateState.Error(e.message ?: "update_no_installer") }
        catch (e: SecurityException) { _state.value = UpdateState.Error(e.message ?: "update_install_denied") }
    }
}
