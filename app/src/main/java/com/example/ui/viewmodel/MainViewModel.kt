package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.CineVaultApplication
import com.example.data.downloads.OfflineDownloadManager
import com.example.data.local.CineVaultDatabase
import com.example.data.local.LocalMediaEntity
import com.example.data.local.LocalMediaMetadataService
import com.example.data.local.OfflineDownloadEntity
import com.example.data.model.LibraryMediaItem
import com.example.data.model.PlaybackEngine
import com.example.data.model.StremioManifest
import com.example.data.model.StremioMetaDetail
import com.example.data.model.StremioMetaPreview
import com.example.data.model.StremioStream
import com.example.data.model.StremioSubtitle
import com.example.data.model.StremioVideoEpisode
import com.example.data.model.SyncedAddon
import com.example.data.model.UserPreferences
import com.example.data.preferences.LocalPreferencesStore
import com.example.data.repository.CineVaultSyncPort
import com.example.data.repository.unwrapFirestoreException
import com.example.data.stremio.StremioAddonClient
import com.example.data.stremio.StremioUrlHelper
import com.example.player.PlayerEpisodeItem
import com.example.player.StreamAutoMatcher
import com.example.player.StreamFingerprint
import com.example.player.StreamSourceOption
import com.example.player.UpNextInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CatalogSection(
    val sectionKey: String,
    val addonKey: String = "",
    val addonName: String,
    val addonManifestUrl: String = "",
    val catalogId: String = "",
    val catalogTitle: String,
    val type: String,
    val items: List<StremioMetaPreview>,
    val availableGenres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val supportsSkip: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false
)

data class MediaDetailUiState(
    val isLoading: Boolean = false,
    val preview: StremioMetaPreview? = null,
    val detail: StremioMetaDetail? = null,
    val selectedEpisodeId: String? = null,
    val streams: List<StremioStream> = emptyList(),
    val streamAddonMap: Map<String, Pair<String, String>> = emptyMap(),
    val subtitles: List<StremioSubtitle> = emptyList(),
    val relatedItems: List<StremioMetaPreview> = emptyList(),
    val error: String? = null
)

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedType: String = "all",
    val sections: List<CatalogSection> = emptyList(),
    val hiddenSectionKeys: Set<String> = emptySet(),
    val catalogOrderKeys: List<String> = emptyList(),
    val errorMessage: String? = null
)

class CineVaultViewModel(
    appContext: Context,
    private val syncRepository: CineVaultSyncPort,
    private val currentUserId: String
) : ViewModel() {

    private val sharedClient = (appContext.applicationContext as? CineVaultApplication)?.sharedOkHttpClient
        ?: StremioAddonClient.defaultOkHttpClient()
    private val stremioClient = StremioAddonClient(okHttpClient = sharedClient)
    private val localDb = CineVaultDatabase.getInstance(appContext)
    private val localPreferencesStore = LocalPreferencesStore(appContext)
    private val localMetadataService = LocalMediaMetadataService(appContext, stremioClient)
    private val downloadManager = OfflineDownloadManager(appContext)

    // ---- Synced Firestore Streams ----

    val addonsState: StateFlow<UiState<List<SyncedAddon>>> = syncRepository.observeAddons()
        .map<List<SyncedAddon>, UiState<List<SyncedAddon>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing addons", root)
            emit(UiState.Error(root.message ?: "Failed to load Stremio addons"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val libraryState: StateFlow<UiState<List<LibraryMediaItem>>> = syncRepository.observeLibrary()
        .map<List<LibraryMediaItem>, UiState<List<LibraryMediaItem>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing cloud library", root)
            emit(UiState.Error(root.message ?: "Failed to sync library"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val preferencesState: StateFlow<UserPreferences> = localPreferencesStore.preferencesFlow
        .map { it.copy(userId = currentUserId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences(userId = currentUserId)
        )

    // ---- Room Offline Downloads & Local Library ----

    val downloadsState: StateFlow<List<OfflineDownloadEntity>> = localDb.downloadDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val localMediaState: StateFlow<List<LocalMediaEntity>> = localDb.localMediaDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // ---- Discover / Detail / Status Messages ----

    private val _discoverState = MutableStateFlow(DiscoverUiState())
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _addonCatalogNames = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val addonCatalogNames: StateFlow<Map<String, List<String>>> = _addonCatalogNames.asStateFlow()

    private val _detailState = MutableStateFlow(MediaDetailUiState())
    val detailState: StateFlow<MediaDetailUiState> = _detailState.asStateFlow()

    private val _spotlightMetaMap = MutableStateFlow<Map<String, StremioMetaDetail>>(emptyMap())
    val spotlightMetaMap: StateFlow<Map<String, StremioMetaDetail>> = _spotlightMetaMap.asStateFlow()

    private val _lastStreamPreference = MutableStateFlow<StreamFingerprint?>(null)
    val lastStreamPreference: StateFlow<StreamFingerprint?> = _lastStreamPreference.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var seededDefaults = false
    private var discoverJob: Job? = null

    init {
        viewModelScope.launch {
            localPreferencesStore.hiddenCatalogsFlow.collect { hidden ->
                _discoverState.update { it.copy(hiddenSectionKeys = hidden) }
            }
        }
        viewModelScope.launch {
            localPreferencesStore.catalogOrderFlow.collect { order ->
                _discoverState.update { state ->
                    state.copy(
                        catalogOrderKeys = order,
                        sections = applyCatalogOrdering(state.sections, order)
                    )
                }
            }
        }
        viewModelScope.launch {
            addonsState.collect { state ->
                if (state is UiState.Success) {
                    if (state.data.isEmpty() && !seededDefaults) {
                        seededDefaults = true
                        val hasSeeded = localPreferencesStore.starterAddonsSeededFlow.first()
                        if (!hasSeeded) {
                            localPreferencesStore.setStarterAddonsSeeded(true)
                            seedStarterAddons()
                        }
                    } else if (state.data.isNotEmpty()) {
                        refreshDiscoverCatalogs(state.data, _discoverState.value.searchQuery)
                    }
                }
            }
        }
        viewModelScope.launch {
            syncRepository.observePreferences()
                .catch { error ->
                    val fsError = error.unwrapFirestoreException()
                    if (fsError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing remote preferences, staying on local DataStore", fsError ?: error)
                }
                .collect { remotePrefs ->
                    if (remotePrefs.userId.isNotBlank()) {
                        localPreferencesStore.mergeRemotePreferences(remotePrefs)
                    }
                }
        }
    }

    private suspend fun seedStarterAddons() {
        val starterManifests = listOf(
            StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST,
            StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST,
            StremioUrlHelper.DEFAULT_OPENSUBTITLES_MANIFEST
        )
        starterManifests.forEach { url ->
            installAddonInternal(url, silent = true)
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun updateSearchQuery(query: String) {
        _discoverState.update { it.copy(searchQuery = query) }
        val addons = (addonsState.value as? UiState.Success)?.data.orEmpty()
        refreshDiscoverCatalogs(addons, query, debounceMs = 280L)
    }

    fun selectMediaTypeFilter(type: String) {
        _discoverState.update { it.copy(selectedType = type) }
    }

    fun refreshDiscover() {
        val addons = (addonsState.value as? UiState.Success)?.data.orEmpty()
        refreshDiscoverCatalogs(addons, _discoverState.value.searchQuery, debounceMs = 0L, forceRefresh = true)
    }

    fun toggleCatalogVisibility(sectionKey: String) {
        val currentlyHidden = _discoverState.value.hiddenSectionKeys.contains(sectionKey)
        viewModelScope.launch {
            localPreferencesStore.setCatalogHidden(sectionKey, !currentlyHidden)
        }
    }

    fun resetHiddenCatalogs() {
        val hidden = _discoverState.value.hiddenSectionKeys.toList()
        viewModelScope.launch {
            hidden.forEach { key -> localPreferencesStore.setCatalogHidden(key, false) }
        }
    }

    fun moveCatalogOrder(sectionKey: String, direction: Int) {
        val currentSections = _discoverState.value.sections
        val currentKeys = currentSections.map { it.sectionKey }.toMutableList()
        val idx = currentKeys.indexOf(sectionKey)
        if (idx < 0) return
        val targetIdx = (idx + direction).coerceIn(0, currentKeys.lastIndex)
        if (targetIdx == idx) return
        val moved = currentKeys.removeAt(idx)
        currentKeys.add(targetIdx, moved)
        viewModelScope.launch {
            localPreferencesStore.saveCatalogOrder(currentKeys)
        }
    }

    fun selectCatalogGenre(sectionKey: String, genre: String?) {
        val targetSection = _discoverState.value.sections.firstOrNull { it.sectionKey == sectionKey } ?: return
        viewModelScope.launch {
            val extras = if (!genre.isNullOrBlank()) mapOf("genre" to genre) else emptyMap()
            val metas = withTimeoutOrNull(12_000L) {
                stremioClient.fetchCatalog(
                    manifestUrl = targetSection.addonManifestUrl,
                    type = targetSection.type,
                    catalogId = targetSection.catalogId,
                    searchQuery = _discoverState.value.searchQuery.takeIf { it.isNotBlank() },
                    extraParams = extras
                ).getOrNull()
            } ?: return@launch

            _discoverState.update { state ->
                state.copy(
                    sections = state.sections.map { sec ->
                        if (sec.sectionKey == sectionKey) {
                            sec.copy(
                                items = metas.take(60),
                                selectedGenre = genre,
                                canLoadMore = sec.supportsSkip && metas.size >= 15
                            )
                        } else sec
                    }
                )
            }
        }
    }

    fun loadMoreCatalogItems(sectionKey: String) {
        val targetSection = _discoverState.value.sections.firstOrNull { it.sectionKey == sectionKey } ?: return
        if (targetSection.isLoadingMore) return
        viewModelScope.launch {
            _discoverState.update { state ->
                state.copy(
                    sections = state.sections.map {
                        if (it.sectionKey == sectionKey) it.copy(isLoadingMore = true) else it
                    }
                )
            }
            val extras = linkedMapOf<String, String>()
            extras["skip"] = targetSection.items.size.toString()
            if (!targetSection.selectedGenre.isNullOrBlank()) {
                extras["genre"] = targetSection.selectedGenre
            }
            val moreMetas = withTimeoutOrNull(12_000L) {
                stremioClient.fetchCatalog(
                    manifestUrl = targetSection.addonManifestUrl,
                    type = targetSection.type,
                    catalogId = targetSection.catalogId,
                    searchQuery = _discoverState.value.searchQuery.takeIf { it.isNotBlank() },
                    extraParams = extras
                ).getOrNull()
            }.orEmpty()

            _discoverState.update { state ->
                state.copy(
                    sections = state.sections.map { sec ->
                        if (sec.sectionKey == sectionKey) {
                            val merged = (sec.items + moreMetas).distinctBy { it.id }
                            sec.copy(
                                items = merged,
                                isLoadingMore = false,
                                canLoadMore = sec.supportsSkip && moreMetas.isNotEmpty()
                            )
                        } else sec
                    }
                )
            }
        }
    }

    private fun applyCatalogOrdering(sections: List<CatalogSection>, orderKeys: List<String>): List<CatalogSection> {
        if (orderKeys.isEmpty() || sections.isEmpty()) return sections
        val indexMap = orderKeys.withIndex().associate { it.value to it.index }
        return sections.sortedWith(compareBy { indexMap[it.sectionKey] ?: Int.MAX_VALUE })
    }

    private fun refreshDiscoverCatalogs(
        addons: List<SyncedAddon>,
        query: String,
        debounceMs: Long = 0L,
        forceRefresh: Boolean = false
    ) {
        discoverJob?.cancel()
        discoverJob = viewModelScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            _discoverState.update { it.copy(isLoading = true, errorMessage = null) }

            val enabledAddons = addons.filter { it.enabled }
            val trimmedQuery = query.takeIf { it.isNotBlank() }
            val semaphore = Semaphore(8)
            val discoveredNames = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

            val sections = supervisorScope {
                val jobs = mutableListOf<kotlinx.coroutines.Deferred<List<CatalogSection>>>()

                // Built-in Open Cinema instant playable catalog
                val openCinemaEnabled = enabledAddons.isEmpty() || enabledAddons.any {
                    it.manifestUrl == StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST
                }
                if (openCinemaEnabled) {
                    discoveredNames[StremioUrlHelper.BUILTIN_OPEN_CINEMA_KEY] = listOf("Featured Instant Streams")
                    jobs += async {
                        val openMovies = stremioClient.fetchCatalog(
                            manifestUrl = StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST,
                            type = "movie",
                            catalogId = "open_cinema",
                            searchQuery = trimmedQuery
                        ).getOrNull().orEmpty()
                        if (openMovies.isNotEmpty()) {
                            listOf(
                                CatalogSection(
                                    sectionKey = "${StremioUrlHelper.BUILTIN_OPEN_CINEMA_KEY}::movie::open_cinema",
                                    addonKey = StremioUrlHelper.BUILTIN_OPEN_CINEMA_KEY,
                                    addonName = "Open Cinema Vault",
                                    addonManifestUrl = StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST,
                                    catalogId = "open_cinema",
                                    catalogTitle = if (trimmedQuery == null) "Featured Instant Streams" else "Open Cinema Matches",
                                    type = "movie",
                                    items = openMovies,
                                    availableGenres = listOf("Sci-Fi", "Action", "Animation", "Fantasy", "Comedy")
                                )
                            )
                        } else emptyList()
                    }
                }

                // Query ALL enabled Stremio addons and inspect manifests (so we never drop addons or cap at 4-5 catalogs)
                val remoteAddons = enabledAddons.filter {
                    it.manifestUrl != StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST
                }

                for (addon in remoteAddons) {
                    jobs += async {
                        val manifest = withTimeoutOrNull(12_000L) {
                            stremioClient.fetchManifest(addon.manifestUrl, forceRefresh = forceRefresh).getOrNull()?.second
                        } ?: return@async emptyList()

                        val allCatalogs = manifest.catalogs
                        if (allCatalogs.isEmpty()) return@async emptyList()

                        val aKey = addon.addonKey.ifBlank { addon.id.ifBlank { StremioUrlHelper.sanitizeDocId(addon.manifestUrl) } }
                        discoveredNames[aKey] = allCatalogs.map { c ->
                            val niceTitle = c.name?.takeIf { it.isNotBlank() } ?: c.id.ifBlank { "Catalog" }
                            "$niceTitle (${c.type})"
                        }

                        val selectedCatalogs = if (trimmedQuery != null) {
                            allCatalogs.filter { it.supportsSearch() }.ifEmpty { allCatalogs.take(4) }
                        } else {
                            allCatalogs.filter { !it.hasRequiredExtraForBrowse() }.ifEmpty { allCatalogs }
                        }

                        val catSections = selectedCatalogs.mapIndexed { idx, cat ->
                            async {
                                semaphore.withPermit {
                                    val defaultExtras = if (trimmedQuery == null) cat.defaultBrowseExtras() else emptyMap()
                                    var metas = withTimeoutOrNull(12_000L) {
                                        stremioClient.fetchCatalog(
                                            manifestUrl = addon.manifestUrl,
                                            type = cat.type,
                                            catalogId = cat.id,
                                            searchQuery = trimmedQuery,
                                            extraParams = defaultExtras
                                        ).getOrNull()
                                    }.orEmpty()
                                    if (metas.isEmpty() && defaultExtras.isNotEmpty() && trimmedQuery == null) {
                                        metas = withTimeoutOrNull(10_000L) {
                                            stremioClient.fetchCatalog(
                                                manifestUrl = addon.manifestUrl,
                                                type = cat.type,
                                                catalogId = cat.id
                                            ).getOrNull()
                                        }.orEmpty()
                                    }
                                    if (metas.isNotEmpty()) {
                                        val niceTitle = cat.name?.takeIf { it.isNotBlank() }
                                            ?: cat.id.replaceFirstChar { c -> c.uppercase() }
                                        val niceType = cat.type.replaceFirstChar { c -> c.uppercase() }
                                        val secKey = "$aKey::${cat.type}::${cat.id}::$idx"
                                        CatalogSection(
                                            sectionKey = secKey,
                                            addonKey = aKey,
                                            addonName = addon.name.ifBlank { manifest.name },
                                            addonManifestUrl = addon.manifestUrl,
                                            catalogId = cat.id,
                                            catalogTitle = "$niceTitle ($niceType)",
                                            type = cat.type,
                                            items = metas.take(50),
                                            availableGenres = cat.availableGenres(),
                                            selectedGenre = defaultExtras["genre"],
                                            supportsSkip = cat.supportsSkip(),
                                            canLoadMore = cat.supportsSkip() && metas.size >= 15
                                        )
                                    } else null
                                }
                            }
                        }
                        catSections.awaitAll().filterNotNull()
                    }
                }

                jobs.awaitAll().flatten()
            }

            _addonCatalogNames.value = discoveredNames
            _discoverState.update { current ->
                val ordered = applyCatalogOrdering(sections, current.catalogOrderKeys)
                current.copy(
                    isLoading = false,
                    sections = ordered,
                    errorMessage = if (ordered.isEmpty()) "No items matched your search or active addons." else null
                )
            }
        }
    }

    // ---- Media Detail, Streams, Trailers & Subtitles ----

    fun resolveSpotlightMeta(preview: StremioMetaPreview) {
        val cacheKey = "${preview.type}:${preview.id}"
        if (_spotlightMetaMap.value.containsKey(cacheKey)) return
        viewModelScope.launch {
            val enabledAddons = (addonsState.value as? UiState.Success)?.data
                ?.filter { it.enabled }.orEmpty()
            val metaAddons = enabledAddons.filter {
                it.resources.any { r -> r.equals("meta", ignoreCase = true) } ||
                    it.manifestUrl == StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST
            }.ifEmpty {
                listOf(
                    SyncedAddon(
                        addonKey = "cinemeta",
                        manifestUrl = StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST,
                        name = "Cinemeta"
                    )
                )
            }
            for (addon in metaAddons) {
                val detail = withTimeoutOrNull(8_000L) {
                    stremioClient.fetchMeta(addon.manifestUrl, preview.type, preview.id).getOrNull()
                }
                if (detail != null) {
                    _spotlightMetaMap.update { current -> current + (cacheKey to detail) }
                    break
                }
            }
        }
    }

    fun loadMediaDetail(preview: StremioMetaPreview) {
        viewModelScope.launch {
            val cacheKey = "${preview.type}:${preview.id}"
            val cachedMeta = _spotlightMetaMap.value[cacheKey]
            _detailState.value = MediaDetailUiState(
                isLoading = true,
                preview = preview,
                detail = cachedMeta
            )
            val enabledAddons = (addonsState.value as? UiState.Success)?.data
                ?.filter { it.enabled }.orEmpty()

            // Fetch full metadata across meta addons
            val metaAddons = enabledAddons.filter {
                it.resources.any { r -> r.equals("meta", ignoreCase = true) } ||
                    it.manifestUrl == StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST
            }.ifEmpty {
                listOf(
                    SyncedAddon(
                        addonKey = "cinemeta",
                        manifestUrl = StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST,
                        name = "Cinemeta"
                    )
                )
            }

            var resolvedDetail: StremioMetaDetail? = cachedMeta
            if (resolvedDetail == null) {
                for (addon in metaAddons) {
                    resolvedDetail = stremioClient.fetchMeta(addon.manifestUrl, preview.type, preview.id).getOrNull()
                    if (resolvedDetail != null) {
                        _spotlightMetaMap.update { current -> current + (cacheKey to resolvedDetail) }
                        break
                    }
                }
            }

            val firstEpisodeId = resolvedDetail?.videos?.firstOrNull()?.id
            val streamTargetId = firstEpisodeId ?: preview.id

            val (pairedStreams, subList) = fetchStreamsWithAddonsParallel(
                enabledAddons = enabledAddons,
                type = preview.type,
                mediaId = streamTargetId
            )

            val allCatalogItems = _discoverFeedState.value.flatMap { it.items }
            val currentGenres = (resolvedDetail?.genres ?: preview.genres).toSet()
            val related = allCatalogItems
                .filter { it.id != preview.id && (it.type.equals(preview.type, ignoreCase = true) || preview.type.isBlank()) }
                .distinctBy { it.id }
                .sortedByDescending { item ->
                    val sharedGenreCount = item.genres.count { it in currentGenres }
                    val hasRating = item.imdbRating?.toDoubleOrNull() ?: 0.0
                    (sharedGenreCount * 10.0) + hasRating
                }
                .take(15)

            _detailState.value = MediaDetailUiState(
                isLoading = false,
                preview = preview,
                detail = resolvedDetail,
                selectedEpisodeId = firstEpisodeId,
                streams = pairedStreams.map { it.second },
                streamAddonMap = pairedStreams.mapNotNull { (addon, stream) ->
                    val url = stream.url ?: return@mapNotNull null
                    url to (addon.addonKey to addon.name)
                }.toMap(),
                subtitles = subList,
                relatedItems = related
            )
        }
    }

    fun selectEpisode(episode: StremioVideoEpisode) {
        val current = _detailState.value
        val preview = current.preview ?: return
        viewModelScope.launch {
            _detailState.update {
                it.copy(isLoading = true, selectedEpisodeId = episode.id, streams = emptyList(), streamAddonMap = emptyMap())
            }
            val enabledAddons = (addonsState.value as? UiState.Success)?.data
                ?.filter { it.enabled }.orEmpty()
            val (pairedStreams, subList) = fetchStreamsWithAddonsParallel(
                enabledAddons = enabledAddons,
                type = preview.type,
                mediaId = episode.id
            )
            _detailState.update {
                it.copy(
                    isLoading = false,
                    streams = pairedStreams.map { p -> p.second },
                    streamAddonMap = pairedStreams.mapNotNull { (addon, stream) ->
                        val url = stream.url ?: return@mapNotNull null
                        url to (addon.addonKey to addon.name)
                    }.toMap(),
                    subtitles = if (subList.isNotEmpty()) subList else it.subtitles
                )
            }
        }
    }

    fun saveStreamPreference(fingerprint: StreamFingerprint) {
        _lastStreamPreference.value = fingerprint
    }

    fun saveStreamPreferenceForStream(stream: StremioStream) {
        val url = stream.url ?: return
        val addonPair = _detailState.value.streamAddonMap[url]
        val addonId = addonPair?.first.orEmpty()
        val addonName = addonPair?.second.orEmpty()
        val opt = StreamAutoMatcher.parseOption(
            id = url,
            addonId = addonId,
            addonName = addonName,
            fullFileName = stream.fullFileName(),
            badges = stream.streamingBadges(),
            streamUrl = url
        )
        _lastStreamPreference.value = StreamAutoMatcher.extractFingerprint(opt)
    }

    fun buildDetailSourceOptions(): List<StreamSourceOption> {
        val state = _detailState.value
        return state.streams.mapIndexedNotNull { idx, stream ->
            val url = stream.url ?: return@mapIndexedNotNull null
            val addonPair = state.streamAddonMap[url]
            StreamAutoMatcher.parseOption(
                id = "${idx}_${url.hashCode()}",
                addonId = addonPair?.first.orEmpty(),
                addonName = addonPair?.second.orEmpty(),
                fullFileName = stream.fullFileName(),
                badges = stream.streamingBadges(),
                streamUrl = url
            )
        }
    }

    fun currentSeriesEpisodes(): List<PlayerEpisodeItem> {
        val detail = _detailState.value.detail ?: return emptyList()
        return detail.videos.orEmpty().map { ep ->
            PlayerEpisodeItem(
                id = ep.id,
                title = ep.title?.takeIf { it.isNotBlank() } ?: ep.name?.takeIf { it.isNotBlank() } ?: ep.id,
                season = ep.season,
                episode = ep.episode,
                thumbnail = ep.thumbnail.orEmpty(),
                runtime = detail.runtime.orEmpty()
            )
        }
    }

    private suspend fun fetchStreamsWithAddonsParallel(
        enabledAddons: List<SyncedAddon>,
        type: String,
        mediaId: String
    ): Pair<List<Pair<SyncedAddon, StremioStream>>, List<StremioSubtitle>> = supervisorScope {
        val streamAddons = enabledAddons.filter {
            it.resources.any { r -> r.equals("stream", ignoreCase = true) } ||
                it.manifestUrl == StremioUrlHelper.BUILTIN_OPEN_CINEMA_MANIFEST
        }
        val subAddons = enabledAddons.filter {
            it.resources.any { r -> r.equals("subtitles", ignoreCase = true) } ||
                it.manifestUrl == StremioUrlHelper.DEFAULT_OPENSUBTITLES_MANIFEST
        }

        val streamDeferred = streamAddons.map { addon ->
            async {
                withTimeoutOrNull(8_000L) {
                    stremioClient.fetchStreams(addon.manifestUrl, type, mediaId)
                        .getOrNull().orEmpty()
                        .filter { !it.url.isNullOrBlank() }
                        .map { addon to it }
                }.orEmpty()
            }
        }
        val subDeferred = subAddons.map { addon ->
            async {
                withTimeoutOrNull(8_000L) {
                    stremioClient.fetchSubtitles(addon.manifestUrl, type, mediaId).getOrNull().orEmpty()
                }.orEmpty()
            }
        }
        val streams = streamDeferred.awaitAll().flatten().distinctBy { it.second.url }
        val subs = subDeferred.awaitAll().flatten().distinctBy { it.url }
        streams to subs
    }

    suspend fun fetchStreamsForPlayer(type: String, mediaId: String): List<StreamSourceOption> {
        val enabledAddons = (addonsState.value as? UiState.Success)?.data
            ?.filter { it.enabled }.orEmpty()
        val (pairedStreams, _) = fetchStreamsWithAddonsParallel(enabledAddons, type, mediaId)
        return pairedStreams.mapIndexedNotNull { idx, (addon, stream) ->
            val url = stream.url ?: return@mapIndexedNotNull null
            StreamAutoMatcher.parseOption(
                id = "${idx}_${url.hashCode()}",
                addonId = addon.addonKey,
                addonName = addon.name,
                fullFileName = stream.fullFileName(),
                badges = stream.streamingBadges(),
                streamUrl = url
            )
        }
    }

    fun computeUpNext(currentMediaKey: String, selectedEpisodeId: String?): UpNextInfo? {
        val detail = _detailState.value.detail
        val preview = _detailState.value.preview
        val videos = detail?.videos.orEmpty()
        val matchEpId = selectedEpisodeId?.takeIf { it.isNotBlank() } ?: currentMediaKey
        if (preview != null && preview.type.equals("series", ignoreCase = true) && videos.isNotEmpty()) {
            val idx = videos.indexOfFirst { it.id == matchEpId }
            val nextEp = if (idx >= 0 && idx + 1 < videos.size) videos[idx + 1] else null
            if (nextEp != null) {
                val epCode = if (nextEp.season != null && nextEp.episode != null) {
                    "S${nextEp.season}:E${nextEp.episode}"
                } else "Next Episode"
                val epTitle = nextEp.title?.takeIf { it.isNotBlank() }
                    ?: nextEp.name?.takeIf { it.isNotBlank() }
                    ?: epCode
                return UpNextInfo(
                    mediaKey = nextEp.id,
                    title = "${preview.name} • $epCode",
                    subtitle = epTitle,
                    mediaType = "series",
                    posterUrl = nextEp.thumbnail ?: preview.background ?: preview.poster.orEmpty(),
                    backdropUrl = preview.background.orEmpty(),
                    imdbId = nextEp.id,
                    season = nextEp.season,
                    episode = nextEp.episode
                )
            }
            return null
        }

        // Non-series: never show series UpNext for movies unless queued
        return null
    }

    suspend fun fetchSubtitlesForMedia(type: String, mediaId: String): List<StremioSubtitle> {
        val enabledAddons = (addonsState.value as? UiState.Success)?.data
            ?.filter { it.enabled }.orEmpty()
        val (_, collected) = fetchStreamsWithAddonsParallel(enabledAddons, type, mediaId)
        val extra = if (mediaId.startsWith("cv_open_") && collected.isEmpty()) {
            StremioAddonClient.builtInOpenCinemaSubtitles(mediaId)
        } else emptyList()
        return (collected + extra).distinctBy { it.url }
    }

    // ---- Addon Installation & Management (Synced) ----

    fun installAddon(rawUrl: String) {
        viewModelScope.launch {
            installAddonInternal(rawUrl, silent = false)
        }
    }

    private suspend fun installAddonInternal(rawUrl: String, silent: Boolean) {
        val result = stremioClient.fetchManifest(rawUrl, forceRefresh = !silent)
        result.onSuccess { (normalizedUrl, manifest: StremioManifest) ->
            val key = manifest.id.ifBlank { StremioUrlHelper.sanitizeDocId(normalizedUrl) }
            val resources = manifest.extractedResourceNames().toMutableList()
            if (manifest.catalogs.isNotEmpty() && resources.none { it.equals("catalog", ignoreCase = true) }) {
                resources.add("catalog")
            }
            val entity = SyncedAddon(
                userId = currentUserId,
                addonKey = StremioUrlHelper.sanitizeDocId(key),
                manifestUrl = normalizedUrl,
                name = manifest.name.ifBlank { "Custom Stremio Addon" },
                version = manifest.version,
                description = manifest.description,
                logoUrl = manifest.logo.orEmpty(),
                types = manifest.types.ifEmpty { listOf("movie") },
                resources = resources,
                enabled = true
            )
            syncRepository.upsertAddon(entity)
                .onSuccess {
                    if (!silent) {
                        val catCount = manifest.catalogs.size
                        val msg = if (catCount > 0) {
                            "Installed \"${entity.name}\" ($catCount catalog${if (catCount == 1) "" else "s"})"
                        } else {
                            "Installed & synced \"${entity.name}\""
                        }
                        _statusBanner.value = msg
                    }
                }
                .onFailure { err ->
                    if (!silent) _statusBanner.value = err.localizedMessage ?: "Failed to save addon"
                }
        }.onFailure { err ->
            if (!silent) _statusBanner.value = "Addon error: ${err.localizedMessage ?: "Invalid manifest URL"}"
        }
    }

    fun toggleAddon(addon: SyncedAddon, enabled: Boolean) {
        viewModelScope.launch {
            syncRepository.toggleAddonEnabled(addon.id.ifBlank { addon.addonKey }, enabled)
        }
    }

    fun removeAddon(addon: SyncedAddon) {
        viewModelScope.launch {
            syncRepository.removeAddon(addon.id.ifBlank { addon.addonKey })
            _statusBanner.value = "Removed ${addon.name}"
        }
    }

    // ---- Cloud Library, Personal Cloud Storage & Watch Progress Sync ----

    fun toggleFavorite(preview: StremioMetaPreview) {
        viewModelScope.launch {
            val currentList = (libraryState.value as? UiState.Success)?.data.orEmpty()
            val existing = currentList.firstOrNull { it.mediaKey == StremioUrlHelper.sanitizeDocId(preview.id) }
            if (existing != null && existing.isFavorite && existing.streamUrl.isBlank()) {
                syncRepository.deleteLibraryItem(existing.mediaKey)
                _statusBanner.value = "Removed ${preview.name} from Cloud Vault"
            } else {
                val item = LibraryMediaItem(
                    userId = currentUserId,
                    mediaKey = StremioUrlHelper.sanitizeDocId(preview.id),
                    title = preview.name,
                    mediaType = preview.type,
                    posterUrl = preview.poster.orEmpty(),
                    backdropUrl = preview.background.orEmpty(),
                    overview = preview.description.orEmpty(),
                    year = preview.releaseInfo.orEmpty(),
                    imdbRating = preview.imdbRating.orEmpty(),
                    streamUrl = existing?.streamUrl.orEmpty(),
                    sourceLabel = existing?.sourceLabel ?: "Stremio",
                    isFavorite = true,
                    watchProgressMs = existing?.watchProgressMs ?: 0L,
                    durationMs = existing?.durationMs ?: 0L,
                    lastWatchedAtMs = System.currentTimeMillis()
                )
                syncRepository.upsertLibraryItem(item)
                _statusBanner.value = "Saved ${preview.name} to Cloud Vault"
            }
        }
    }

    fun addPersonalCloudMedia(
        title: String,
        streamUrl: String,
        sourceLabel: String,
        posterUrl: String,
        mediaType: String = "cloud_video"
    ) {
        viewModelScope.launch {
            val key = "cloud_${StremioUrlHelper.sanitizeDocId(title)}_${streamUrl.hashCode().toUInt()}"
            val item = LibraryMediaItem(
                userId = currentUserId,
                mediaKey = key,
                title = title.trim(),
                mediaType = mediaType,
                posterUrl = posterUrl.trim(),
                overview = "Personal cloud media source ($sourceLabel)",
                streamUrl = streamUrl.trim(),
                sourceLabel = sourceLabel.trim().ifBlank { "Personal Cloud" },
                isFavorite = true,
                lastWatchedAtMs = System.currentTimeMillis()
            )
            syncRepository.upsertLibraryItem(item)
                .onSuccess { _statusBanner.value = "Added \"$title\" to Personal Cloud Library" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to save cloud item" }
        }
    }

    fun removeLibraryItem(mediaKey: String) {
        viewModelScope.launch {
            syncRepository.deleteLibraryItem(mediaKey)
        }
    }

    fun saveWatchProgress(
        mediaKey: String,
        title: String,
        mediaType: String,
        posterUrl: String,
        streamUrl: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (mediaKey.isBlank() || positionMs <= 0L) return
        viewModelScope.launch {
            val sanitizedKey = StremioUrlHelper.sanitizeDocId(mediaKey)
            val existing = syncRepository.getLibraryItem(sanitizedKey).getOrNull()
            val item = (existing ?: LibraryMediaItem(
                userId = currentUserId,
                mediaKey = sanitizedKey,
                title = title,
                mediaType = mediaType,
                posterUrl = posterUrl,
                streamUrl = if (streamUrl.startsWith("http")) streamUrl else "",
                sourceLabel = if (streamUrl.startsWith("http")) "Synced Stream" else "Local Device",
                isFavorite = existing?.isFavorite ?: false
            )).copy(
                watchProgressMs = positionMs,
                durationMs = durationMs.coerceAtLeast(existing?.durationMs ?: 0L),
                lastWatchedAtMs = System.currentTimeMillis()
            )
            syncRepository.upsertLibraryItem(item)
        }
    }

    // ---- Offline Downloads ----

    fun startOfflineDownload(
        mediaKey: String,
        title: String,
        mediaType: String,
        posterUrl: String,
        streamUrl: String,
        subtitleUrl: String,
        qualityLabel: String
    ) {
        viewModelScope.launch {
            val wifiOnly = preferencesState.value.downloadWifiOnly
            downloadManager.enqueueDownload(
                mediaKey = mediaKey,
                title = title,
                mediaType = mediaType,
                posterUrl = posterUrl,
                streamUrl = streamUrl,
                subtitleUrl = subtitleUrl,
                qualityLabel = qualityLabel,
                wifiOnly = wifiOnly
            )
            _statusBanner.value = "Downloading \"$title\" for offline viewing"
        }
    }

    fun deleteDownload(downloadKey: String) {
        viewModelScope.launch {
            downloadManager.cancelAndDelete(downloadKey)
            _statusBanner.value = "Deleted offline download"
        }
    }

    // ---- Local Media & Automatic Metadata Retrieval ----

    fun importLocalVideo(uri: Uri) {
        viewModelScope.launch {
            val autoMeta = preferencesState.value.autoFetchLocalMetadata
            val addonUrls = (addonsState.value as? UiState.Success)?.data
                ?.filter { it.enabled && it.resources.contains("catalog") }
                ?.map { it.manifestUrl }.orEmpty()

            _statusBanner.value = if (autoMeta) {
                "Importing video & retrieving metadata..."
            } else {
                "Importing local video..."
            }
            val imported = localMetadataService.importLocalVideo(
                uri = uri,
                autoFetchMetadata = autoMeta,
                metadataAddonManifestUrls = addonUrls.ifEmpty {
                    listOf(StremioUrlHelper.DEFAULT_CINEMETA_MANIFEST)
                }
            )
            _statusBanner.value = if (imported.imdbId.isNotBlank()) {
                "Matched \"${imported.parsedTitle}\" (${imported.parsedYear})"
            } else {
                "Imported \"${imported.parsedTitle}\""
            }
        }
    }

    fun refreshLocalMetadata(uriString: String) {
        viewModelScope.launch {
            val updated = localMetadataService.refreshMetadata(uriString)
            _statusBanner.value = if (updated?.imdbId?.isNotBlank() == true) {
                "Matched metadata for \"${updated.parsedTitle}\""
            } else {
                "No online metadata match found"
            }
        }
    }

    fun attachSubtitleToLocalVideo(uriString: String, subUri: Uri) {
        viewModelScope.launch {
            localMetadataService.attachLocalSubtitle(uriString, subUri)
            _statusBanner.value = "Attached subtitle to local video"
        }
    }

    fun deleteLocalMedia(uriString: String) {
        viewModelScope.launch {
            localMetadataService.deleteLocalMedia(uriString)
        }
    }

    // ---- Preferences Sync ----

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            val updated = transform(preferencesState.value).copy(userId = currentUserId)
            localPreferencesStore.save(updated)
            syncRepository.savePreferences(updated)
        }
    }

    fun setDefaultEngine(engine: PlaybackEngine) {
        updatePreferences { it.copy(preferredEngine = engine.key) }
    }

    private companion object {
        const val TAG = "CineVaultVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
