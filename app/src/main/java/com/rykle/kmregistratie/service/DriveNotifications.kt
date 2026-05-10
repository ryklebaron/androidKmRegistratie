package com.rykle.kmregistratie.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rykle.kmregistratie.KmApplication
import com.rykle.kmregistratie.R

fun postPickerNotification(context: Context, canContinue: Boolean) {
    fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(action).setClass(context, DriveActionReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    val builder = NotificationCompat.Builder(context, KmApplication.CHANNEL_DRIVE_PICKER)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.notif_picker_title))
        .setContentText(context.getString(R.string.notif_picker_text))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(false)
        .setAutoCancel(false)

    // Op Android 14+ vereist USE_FULL_SCREEN_INTENT expliciete gebruikerstoestemming.
    // Alleen instellen als de toestemming is verleend, anders wordt het stilletjes genegeerd.
    val canUseFullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    if (canUseFullScreen) {
        val screenWakeIntent = PendingIntent.getActivity(
            context, 10,
            Intent(context, DrivePickerActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setFullScreenIntent(screenWakeIntent, true)
    }
    builder.addAction(0, context.getString(R.string.drive_type_business),
        actionIntent(DriveActionReceiver.ACTION_BUSINESS, 1))
    builder.addAction(0, context.getString(R.string.drive_type_private),
        actionIntent(DriveActionReceiver.ACTION_PRIVATE, 2))

    if (canContinue) {
        builder.addAction(
            0, context.getString(R.string.drive_type_continue),
            actionIntent(DriveActionReceiver.ACTION_CONTINUE, 3)
        )
    }

    NotificationManagerCompat.from(context)
        .notify(KmApplication.NOTIF_DRIVE_PICKER, builder.build())
}

fun cancelPickerNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(KmApplication.NOTIF_DRIVE_PICKER)
}
