package com.thekeeperofpie.artistalleydatabase.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.thekeeperofpie.artistalleydatabase.MainActivity
import com.thekeeperofpie.artistalleydatabase.R
import com.thekeeperofpie.artistalleydatabase.android_utils.notification.NotificationChannels
import com.thekeeperofpie.artistalleydatabase.android_utils.notification.NotificationIds
import com.thekeeperofpie.artistalleydatabase.android_utils.notification.NotificationProgressWorker
import com.thekeeperofpie.artistalleydatabase.android_utils.persistence.DatabaseSyncer
import com.thekeeperofpie.artistalleydatabase.navigation.NavDrawerItems
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class DatabaseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncers: Set<@JvmSuppressWildcards DatabaseSyncer>,
) : NotificationProgressWorker(
    appContext = appContext,
    params = params,
    progressKey = KEY_PROGRESS,
    notificationChannel = NotificationChannels.SYNC,
    notificationIdOngoing = NotificationIds.SYNC_ONGOING,
    notificationIdFinished = NotificationIds.SYNC_FINISHED,
    smallIcon = R.drawable.baseline_sync_24,
    ongoingTitle = R.string.notification_sync_ongoing_title,
    successTitle = R.string.notification_sync_finished_title,
    failureTitle = R.string.notification_sync_failed_title,
    notificationContentIntent = {
        PendingIntent.getActivity(
            appContext,
            PendingIntentRequestCodes.SYNC_MAIN_ACTIVITY_OPEN.code,
            Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setClass(appContext, MainActivity::class.java)
                putExtra(
                    MainActivity.STARTING_NAV_DESTINATION,
                    NavDrawerItems.BROWSE.id,
                )
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    },
) {

    companion object {
        const val UNIQUE_WORK_NAME = "database_fetch"

        private const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWorkInternal(): Result {
        withContext(Dispatchers.IO) {
            val maxProgressValues = syncers.map { it.getMaxProgress() }
            var initialProgress = 0
            val maxProgress = maxProgressValues.sum()
            syncers.forEachIndexed { index, syncer ->
                val nextProgress = initialProgress + maxProgressValues[index]
                syncer.sync(
                    initialProgress = initialProgress,
                    maxProgress = nextProgress,
                    setProgress = ::setProgress,
                )
                initialProgress = nextProgress
                setProgress(initialProgress, maxProgress)
            }
        }

        return Result.success()
    }
}
