package com.mexmp3.mextv.data

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,       // ms
    val path: String,
    val dateAdded: Long,
    val folderName: String,
    val size: Long
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: String?
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int
)

data class Folder(
    val path: String,
    val name: String,
    val songCount: Int
)

fun Song.albumArtUri(): String =
    "content://media/external/audio/albumart/$albumId"

fun Song.durationFormatted(): String {
    val totalSec = duration / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
