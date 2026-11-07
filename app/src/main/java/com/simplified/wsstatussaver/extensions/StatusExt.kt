/*
 * Copyright (C) 2023 Christians Martínez Alvarado
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
package com.simplified.wsstatussaver.extensions

import android.content.Context
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

val fileDateFormat: DateFormat by lazy {
    SimpleDateFormat("MMM_d_yyyy_HH.mm.ss", Locale.ENGLISH)
}

fun Status.getFormattedDate(context: Context): String {
    val date = Date(dateModified)
    val resLocale = context.resources.configuration.locales[0]
    return DateFormat.getDateInstance(DateFormat.MEDIUM, resLocale).format(date)
}

/**
 * Generates and returns a new save name depending on the
 * given [StatusType] format and the current time.
 */
fun getNewSaveName(type: StatusType? = null, timeMillis: Long, delta: Int): String {
    var saveName = String.format("Status_%s", fileDateFormat.format(Date(timeMillis)))
    if (delta > 0) {
        saveName += "-$delta"
    }
    if (type != null) {
        saveName += type.format
    }
    return saveName
}

fun Status.getState(context: Context): String =
    if (isSaved) context.getString(R.string.status_saved) else context.getString(R.string.status_unsaved)

/** WhatsApp statuses are visible for 24 hours after they are posted. */
private val STATUS_LIFETIME_MILLIS = TimeUnit.HOURS.toMillis(24)

/**
 * Approximate time left before this status disappears from WhatsApp, e.g. "5h left".
 * Measured from the file's modification time, which is when it reached this device and can be
 * later than when it was posted, so it is an upper bound. WhatsApp keeps the file for a while
 * after the status expires; those files are labeled "Expired".
 */
fun Status.getExpiryLabel(context: Context, now: Long = System.currentTimeMillis()): String {
    val remaining = STATUS_LIFETIME_MILLIS - (now - dateModified)
    val hours = TimeUnit.MILLISECONDS.toHours(remaining)
    return when {
        remaining <= 0 -> context.getString(R.string.status_expired)
        hours >= 1 -> context.getString(R.string.status_hours_left, hours)
        else -> context.getString(
            R.string.status_minutes_left,
            TimeUnit.MILLISECONDS.toMinutes(remaining).coerceAtLeast(1)
        )
    }
}

/** Tile label: "Saved", or "Unsaved · 5h left" for statuses that have not been saved yet. */
fun Status.getStateWithExpiry(context: Context): String =
    if (isSaved) getState(context)
    else context.getString(R.string.status_state_with_expiry, getState(context), getExpiryLabel(context))

fun StatusType.acceptFileName(fileName: String): Boolean = !fileName.startsWith(".") && fileName.endsWith(this.format)

fun File.getStatusType() = StatusType.entries.firstOrNull { it.acceptFileName(name) }