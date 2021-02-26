package com.mysticwind.linenotificationsupport.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.DismissNotificationBroadcastReceiver;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import timber.log.Timber;

public class DismissActionInjectorNotificationPublisherDecorator implements NotificationPublisher {

    private final NotificationPublisher notificationPublisher;
    private final Context context;

    public DismissActionInjectorNotificationPublisherDecorator(NotificationPublisher notificationPublisher, Context context) {
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void publishNotification(LineNotification lineNotification, int notificationId) {
        if (hasDismissAction(lineNotification)) {
            Timber.i("Notification [%d] [%s] already has dismiss action",
                    notificationId, lineNotification.getMessage());
            this.notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        }
        final LineNotification dismissActionInjectedLineNotification = lineNotification.toBuilder()
                .action(buildDismissAction(notificationId))
                .build();
        this.notificationPublisher.publishNotification(dismissActionInjectedLineNotification, notificationId);
    }

    private boolean hasDismissAction(final LineNotification lineNotification) {
        final String dismissButtonText = context.getString(R.string.dismiss_button_text);

        return lineNotification.getActions().stream()
                .map(action -> action.title)
                .filter(title -> StringUtils.equals(title, dismissButtonText))
                .findAny()
                .isPresent();
    }

    private Notification.Action buildDismissAction(final int notificationId) {
        final Intent buttonIntent = new Intent(context, DismissNotificationBroadcastReceiver.class);
        buttonIntent.setAction("dismiss-" + notificationId);
        buttonIntent.putExtra(DismissNotificationBroadcastReceiver.NOTIFICATION_ID, notificationId);

        final PendingIntent actionIntent =
                PendingIntent.getBroadcast(context,
                        0,
                        buttonIntent,
                        PendingIntent.FLAG_ONE_SHOT);
        final String dismissButtonText = context.getString(R.string.dismiss_button_text);
        return new Notification.Action.Builder(android.R.drawable.btn_default, dismissButtonText, actionIntent)
                .build();
    }

    @Override
    public void republishNotification(LineNotification lineNotification, int notificationId) {
        // do nothing as the action should have been injected previously through publishNotification()
        this.notificationPublisher.republishNotification(lineNotification, notificationId);
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
        this.notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }
}