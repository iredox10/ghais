package com.quranify.player

import com.quranify.domain.model.RepeatMode
import com.quranify.domain.model.TrackItem

class QueueManager {
    private var _queue = mutableListOf<TrackItem>()
    private var _originalQueue = mutableListOf<TrackItem>()
    var currentIndex: Int = -1
        private set

    var repeatMode: RepeatMode = RepeatMode.OFF
        private set

    var isShuffle: Boolean = false
        private set

    val queue: List<TrackItem>
        get() = _queue.toList()

    val originalQueue: List<TrackItem>
        get() = _originalQueue.toList()

    val currentTrack: TrackItem?
        get() = if (currentIndex in _queue.indices) _queue[currentIndex] else null

    fun setQueue(items: List<TrackItem>, startIndex: Int = 0) {
        _originalQueue.clear()
        _originalQueue.addAll(items)

        if (isShuffle) {
            val startItem = items.getOrNull(startIndex) ?: items.firstOrNull()
            val shuffled = items.shuffled().toMutableList()
            if (startItem != null) {
                shuffled.remove(startItem)
                shuffled.add(0, startItem)
            }
            _queue = shuffled
            currentIndex = if (items.isNotEmpty()) 0 else -1
        } else {
            _queue.clear()
            _queue.addAll(items)
            currentIndex = if (items.isNotEmpty() && startIndex in items.indices) startIndex else if (items.isNotEmpty()) 0 else -1
        }
    }

    fun playNext(forceAdvance: Boolean = false): TrackItem? {
        if (_queue.isEmpty()) return null

        if (forceAdvance && repeatMode == RepeatMode.AYAH) {
            // Force skip forward past current ayah even in AYAH repeat mode
            if (currentIndex + 1 < _queue.size) {
                currentIndex++
                return currentTrack
            } else if (repeatMode == RepeatMode.QUEUE || repeatMode == RepeatMode.SURAH) {
                currentIndex = 0
                return currentTrack
            }
            return null
        }

        when (repeatMode) {
            RepeatMode.AYAH -> {
                // Return same track
                return currentTrack
            }
            RepeatMode.SURAH -> {
                val currentTrackId = currentTrack?.surahId
                if (currentTrackId != null) {
                    val nextIndex = currentIndex + 1
                    if (nextIndex < _queue.size) {
                        val nextTrack = _queue[nextIndex]
                        if (nextTrack.surahId == currentTrackId) {
                            currentIndex = nextIndex
                            return nextTrack
                        } else {
                            // Find first track of this surah
                            val firstIndex = _queue.indexOfFirst { it.surahId == currentTrackId }
                            if (firstIndex != -1) {
                                currentIndex = firstIndex
                                return _queue[currentIndex]
                            }
                        }
                    } else {
                        val firstIndex = _queue.indexOfFirst { it.surahId == currentTrackId }
                        if (firstIndex != -1) {
                            currentIndex = firstIndex
                            return _queue[currentIndex]
                        }
                    }
                }
            }
            RepeatMode.QUEUE -> {
                if (_queue.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % _queue.size
                    return currentTrack
                }
            }
            RepeatMode.OFF -> {
                if (currentIndex + 1 < _queue.size) {
                    currentIndex++
                    return currentTrack
                }
            }
        }
        return null
    }

    fun playPrevious(forceAdvance: Boolean = false): TrackItem? {
        if (_queue.isEmpty()) return null

        if (forceAdvance && repeatMode == RepeatMode.AYAH) {
            // Force skip backward past current ayah even in AYAH repeat mode
            if (currentIndex - 1 >= 0) {
                currentIndex--
                return currentTrack
            } else if (repeatMode == RepeatMode.QUEUE) {
                currentIndex = _queue.size - 1
                return currentTrack
            }
            return null
        }

        when (repeatMode) {
            RepeatMode.AYAH -> {
                return currentTrack
            }
            RepeatMode.SURAH -> {
                val currentTrackId = currentTrack?.surahId
                if (currentTrackId != null) {
                    val prevIndex = currentIndex - 1
                    if (prevIndex >= 0) {
                        val prevTrack = _queue[prevIndex]
                        if (prevTrack.surahId == currentTrackId) {
                            currentIndex = prevIndex
                            return prevTrack
                        }
                    }
                    val lastIndex = _queue.indexOfLast { it.surahId == currentTrackId }
                    if (lastIndex != -1) {
                        currentIndex = lastIndex
                        return _queue[currentIndex]
                    }
                }
            }
            RepeatMode.QUEUE -> {
                if (_queue.isNotEmpty()) {
                    currentIndex = if (currentIndex - 1 < 0) _queue.size - 1 else currentIndex - 1
                    return currentTrack
                }
            }
            RepeatMode.OFF -> {
                if (currentIndex - 1 >= 0) {
                    currentIndex--
                    return currentTrack
                }
            }
        }
        return null
    }

    fun skipToIndex(index: Int): TrackItem? {
        if (index in _queue.indices) {
            currentIndex = index
            return currentTrack
        }
        return null
    }

    fun removeAt(index: Int): TrackItem? {
        if (index !in _queue.indices) return null
        val removed = _queue.removeAt(index)
        _originalQueue.remove(removed)

        if (_queue.isEmpty()) {
            currentIndex = -1
            return null
        }

        when {
            index < currentIndex -> {
                currentIndex--
            }
            index == currentIndex -> {
                if (currentIndex >= _queue.size) {
                    currentIndex = if (repeatMode == RepeatMode.QUEUE) 0 else -1
                }
            }
            // index > currentIndex: currentIndex remains unchanged
        }
        return currentTrack
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
        if (isShuffle) {
            val curr = currentTrack
            val shuffled = _originalQueue.shuffled().toMutableList()
            if (curr != null) {
                shuffled.remove(curr)
                shuffled.add(0, curr)
                currentIndex = 0
            } else {
                currentIndex = if (shuffled.isNotEmpty()) 0 else -1
            }
            _queue = shuffled
        } else {
            val curr = currentTrack
            _queue.clear()
            _queue.addAll(_originalQueue)
            if (curr != null) {
                val foundIndex = _queue.indexOf(curr)
                currentIndex = if (foundIndex != -1) foundIndex else if (_queue.isNotEmpty()) 0 else -1
            } else {
                currentIndex = if (_queue.isNotEmpty()) 0 else -1
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
    }

    fun addToQueue(item: TrackItem) {
        _originalQueue.add(item)
        _queue.add(item)
        if (currentIndex == -1 && _queue.isNotEmpty()) {
            currentIndex = 0
        }
    }

    fun addAllToQueue(items: List<TrackItem>) {
        _originalQueue.addAll(items)
        _queue.addAll(items)
        if (currentIndex == -1 && _queue.isNotEmpty()) {
            currentIndex = 0
        }
    }

    fun move(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in _queue.indices || toIndex !in _queue.indices || fromIndex == toIndex) {
            return false
        }
        val item = _queue.removeAt(fromIndex)
        _queue.add(toIndex, item)
        when {
            currentIndex == fromIndex -> currentIndex = toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex--
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex++
        }
        return true
    }

    fun clear() {
        _queue.clear()
        _originalQueue.clear()
        currentIndex = -1
    }
}
