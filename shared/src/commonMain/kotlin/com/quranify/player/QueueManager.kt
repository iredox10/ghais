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

    fun setQueue(items: List<TrackItem>, startIndex: Int = 0) {
        _originalQueue.clear()
        _originalQueue.addAll(items)

        if (isShuffle) {
            val startItem = items.getOrNull(startIndex)
            val shuffled = items.shuffled().toMutableList()
            if (startItem != null) {
                shuffled.remove(startItem)
                shuffled.add(0, startItem)
            }
            _queue = shuffled
            currentIndex = 0
        } else {
            _queue.clear()
            _queue.addAll(items)
            currentIndex = if (items.isNotEmpty() && startIndex in items.indices) startIndex else if (items.isNotEmpty()) 0 else -1
        }
    }

    fun playNext(): TrackItem? {
        if (_queue.isEmpty()) return null

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

    fun playPrevious(): TrackItem? {
        if (_queue.isEmpty()) return null

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

    fun toggleShuffle() {
        isShuffle = !isShuffle
        if (isShuffle) {
            val curr = currentTrack
            val shuffled = _originalQueue.shuffled().toMutableList()
            if (curr != null) {
                shuffled.remove(curr)
                shuffled.add(0, curr)
                currentIndex = 0
            }
            _queue = shuffled
        } else {
            val curr = currentTrack
            _queue.clear()
            _queue.addAll(_originalQueue)
            if (curr != null) {
                currentIndex = _queue.indexOf(curr)
                if (currentIndex == -1) currentIndex = 0
            } else {
                currentIndex = -1
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
    }

    fun addToQueue(item: TrackItem) {
        _originalQueue.add(item)
        if (isShuffle) {
            _queue.add(item)
        } else {
            _queue.add(item)
        }
        if (currentIndex == -1) {
            currentIndex = 0
        }
    }

    fun clear() {
        _queue.clear()
        _originalQueue.clear()
        currentIndex = -1
    }

    val currentTrack: TrackItem?
        get() = if (currentIndex in _queue.indices) _queue[currentIndex] else null
}
