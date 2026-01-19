package template.core.pref

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

internal class PrefServiceMemory : PrefService {
    private val parser by lazy { Json { prettyPrint = true } }

    override fun getUserPreferenceContent(): Flow<String?> = prefFlow.map { parser.encodeToString(it) }

    private val prefFlow: MutableStateFlow<PrefData> by lazy {
        MutableStateFlow(
            PrefData(key = null),
        )
    }

    override fun getKey(): Flow<String?> = prefFlow.map { it.key }

    override suspend fun setKey(key: String?) {
        val newPrefData = prefFlow.first().copy(key = key)
        prefFlow.emit(newPrefData)
    }
}
