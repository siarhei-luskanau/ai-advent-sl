package template.core.pref

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.FileSystem

internal class PrefServiceDataStore(
    private val prefPathProvider: PrefPathProvider,
) : PrefService {
    private val parser by lazy { Json { prettyPrint = true } }

    override fun getUserPreferenceContent(): Flow<String?> = getFlowFromDataStore { parser.encodeToString(it) }

    private val dataStore: DataStore<PrefData> by lazy {
        DataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PrefSerializer(),
                    producePath = { prefPathProvider.get() },
                ),
            corruptionHandler = null,
            migrations = emptyList(),
        )
    }

    override fun getKey(): Flow<String?> = getFlowFromDataStore { it.key }

    override suspend fun setKey(key: String?) {
        updateDataStore { it.copy(key = key) }
    }

    private fun <T : Any> getFlowFromDataStore(mapData: (PrefData) -> T?): Flow<T?> = dataStore.data.map { mapData(it) }

    private suspend fun updateDataStore(update: (PrefData) -> PrefData) {
        dataStore.updateData { update(it) }
    }
}
