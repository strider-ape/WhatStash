/*
 * Copyright (C) 2026 WhatStash contributors
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 * the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.simplified.wsstatussaver.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import com.simplified.wsstatussaver.extensions.IsScopedStorageRequired
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.saveLocation
import com.simplified.wsstatussaver.model.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Saves a single frame of a video status as a JPEG, in the same folder as saved image statuses,
 * so it also shows up in the Saved tab.
 */
object FrameSaver {

    private const val TAG = "FrameSaver"
    private const val JPEG_QUALITY = 95

    /** Returns true when the frame at [positionMs] of [videoUri] was saved. */
    suspend fun saveFrame(context: Context, videoUri: Uri, positionMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            val frame = extractFrame(context, videoUri, positionMs) ?: return@withContext false
            try {
                val name = "Frame_" + SimpleDateFormat("MMM_d_yyyy_HH.mm.ss", Locale.ENGLISH)
                    .format(Date()) + StatusType.IMAGE.format
                if (IsScopedStorageRequired) saveScoped(context, frame, name) else saveLegacy(context, frame, name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save video frame", e)
                false
            } finally {
                frame.recycle()
            }
        }

    private fun extractFrame(context: Context, videoUri: Uri, positionMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            retriever.getFrameAtTime(
                TimeUnit.MILLISECONDS.toMicros(positionMs.coerceAtLeast(0)),
                MediaMetadataRetriever.OPTION_CLOSEST
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read video frame", e)
            null
        } finally {
            retriever.release()
        }
    }

    private fun saveScoped(context: Context, frame: Bitmap, name: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, name)
            put(MediaColumns.RELATIVE_PATH, StatusType.IMAGE.getRelativePath(context.preferences().saveLocation))
            put(MediaColumns.MIME_TYPE, StatusType.IMAGE.mimeType)
        }
        val uri = resolver.insert(StatusType.IMAGE.contentUri, values) ?: return false
        val written = resolver.openOutputStream(uri)?.use { it.writeJpeg(frame) } ?: false
        if (!written) {
            resolver.delete(uri, null, null)
            return false
        }
        resolver.notifyChange(StatusType.IMAGE.contentUri, null)
        return true
    }

    private fun saveLegacy(context: Context, frame: Bitmap, name: String): Boolean {
        val directory = StatusType.IMAGE.getSavesDirectory(context.preferences().saveLocation)
        if (!directory.exists() && !directory.mkdirs()) return false
        val file = File(directory, name)
        val written = file.outputStream().use { it.writeJpeg(frame) }
        if (written) {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(StatusType.IMAGE.mimeType), null)
        }
        return written
    }

    private fun OutputStream.writeJpeg(frame: Bitmap): Boolean =
        frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, this)
}
