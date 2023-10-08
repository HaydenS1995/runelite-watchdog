package com.adamk33n3r.runelite.watchdog.ui.panels;

import com.adamk33n3r.runelite.watchdog.AlertManager;
import com.adamk33n3r.runelite.watchdog.NotificationType;
import com.adamk33n3r.runelite.watchdog.WatchdogPlugin;
import com.adamk33n3r.runelite.watchdog.alerts.Alert;
import com.adamk33n3r.runelite.watchdog.notifications.GameMessage;
import com.adamk33n3r.runelite.watchdog.notifications.Notification;
import com.adamk33n3r.runelite.watchdog.notifications.NotificationEvent;
import com.adamk33n3r.runelite.watchdog.notifications.Overhead;
import com.adamk33n3r.runelite.watchdog.notifications.Overlay;
import com.adamk33n3r.runelite.watchdog.notifications.ScreenFlash;
import com.adamk33n3r.runelite.watchdog.notifications.Sound;
import com.adamk33n3r.runelite.watchdog.notifications.SoundEffect;
import com.adamk33n3r.runelite.watchdog.notifications.TextToSpeech;
import com.adamk33n3r.runelite.watchdog.notifications.TrayNotification;
import com.adamk33n3r.runelite.watchdog.ui.StretchedStackedLayout;
import com.adamk33n3r.runelite.watchdog.ui.dropdownbutton.DropDownButtonFactory;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.MessageNotificationPanel;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.NotificationPanel;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.OverheadNotificationPanel;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.OverlayNotificationPanel;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.ScreenFlashNotificationPanel;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.SoundEffectNotificationPanel;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.SoundNotificationPanel;
import com.adamk33n3r.runelite.watchdog.ui.notifications.panels.TextToSpeechNotificationPanel;

import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.components.DragAndDropReorderPane;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

import com.google.inject.Injector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;

import static com.adamk33n3r.runelite.watchdog.WatchdogPanel.ADD_ICON;

@Slf4j
public class NotificationsPanel extends JPanel {
    private final Alert alert;

    @Inject
    private ColorPickerManager colorPickerManager;

    @Inject
    private AlertManager alertManager;

    @Getter
    private final DragAndDropReorderPane notificationContainer;

    public NotificationsPanel(Alert alert) {
        this.alert = alert;
        this.setLayout(new BorderLayout(0, 5));
        this.notificationContainer = new DragAndDropReorderPane();
        this.notificationContainer.addDragListener((c) -> {
            int pos = this.notificationContainer.getPosition(c);
            NotificationPanel notificationPanel = (NotificationPanel) c;
            Notification notification = notificationPanel.getNotification();
//            log.debug("drag listener: " + notification.getType().getName() + " to " + pos);
            notification.getAlert().moveNotificationTo(notification, pos);
            this.alertManager.saveAlerts();
        });

        JPopupMenu popupMenu = new JPopupMenu();
        ActionListener actionListener = e -> {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            NotificationType nType = (NotificationType) menuItem.getClientProperty(NotificationType.class);
            this.addPanel(this.createNotification(nType));
            this.notificationContainer.revalidate();
            this.alertManager.saveAlerts();
        };
        Arrays.stream(NotificationType.values()).sorted().forEach(nType -> {
            JMenuItem c = new JMenuItem(nType.getName());
            c.setToolTipText(nType.getTooltip());
            c.putClientProperty(NotificationType.class, nType);
            c.addActionListener(actionListener);
            popupMenu.add(c);
        });
        JButton addDropDownButton = DropDownButtonFactory.createDropDownButton(ADD_ICON, popupMenu);
        addDropDownButton.setPreferredSize(new Dimension(40, addDropDownButton.getPreferredSize().height));
        addDropDownButton.setToolTipText("Create New Notification");
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(new JLabel("Notifications"), BorderLayout.WEST);
        buttonPanel.add(addDropDownButton, BorderLayout.EAST);

        this.add(buttonPanel, BorderLayout.NORTH);

        ScrollablePanel scrollablePanel = new ScrollablePanel(new StretchedStackedLayout(3, 3));
        scrollablePanel.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
        scrollablePanel.setScrollableHeight(ScrollablePanel.ScrollableSizeHint.STRETCH);
        scrollablePanel.setScrollableBlockIncrement(ScrollablePanel.VERTICAL, ScrollablePanel.IncrementType.PERCENT, 10);
        scrollablePanel.add(this.notificationContainer);
        JScrollPane scrollPane = new JScrollPane(scrollablePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    // After inject, build
    @Inject
    public void rebuild() {
        this.notificationContainer.removeAll();

        for (Notification notification : this.alert.getNotifications()) {
            this.addPanel(notification);
        }

        this.notificationContainer.revalidate();
        this.notificationContainer.repaint();
    }

    private void addPanel(Notification notification) {
        PanelUtils.OnRemove removeNotification = (removedPanel) -> {
            this.alert.getNotifications().remove(notification);
            this.notificationContainer.remove(removedPanel);
            this.notificationContainer.revalidate();
            this.alertManager.saveAlerts();
        };

        NotificationPanel notificationPanel = null;
        if (notification instanceof GameMessage) {
            notificationPanel = new MessageNotificationPanel((GameMessage) notification, this, this.alertManager::saveAlerts, removeNotification);
        } else if (notification instanceof TextToSpeech)
            notificationPanel = new TextToSpeechNotificationPanel((TextToSpeech) notification, this, this.alertManager::saveAlerts, removeNotification);
        else if (notification instanceof Sound)
            notificationPanel = new SoundNotificationPanel((Sound)notification, this, this.alertManager::saveAlerts, removeNotification);
        else if (notification instanceof SoundEffect)
            notificationPanel = new SoundEffectNotificationPanel((SoundEffect)notification, this, this.alertManager::saveAlerts, removeNotification);
        else if (notification instanceof TrayNotification)
            notificationPanel = new MessageNotificationPanel((TrayNotification)notification, this, this.alertManager::saveAlerts, removeNotification);
        else if (notification instanceof ScreenFlash)
            notificationPanel = new ScreenFlashNotificationPanel((ScreenFlash) notification, this, this.colorPickerManager, this.alertManager::saveAlerts, removeNotification);
        else if (notification instanceof Overhead)
            notificationPanel = new OverheadNotificationPanel((Overhead) notification, this, this.alertManager::saveAlerts, removeNotification);
        else if (notification instanceof Overlay)
            notificationPanel = new OverlayNotificationPanel((Overlay) notification, this, this.colorPickerManager, this.alertManager::saveAlerts, removeNotification);
        else if (notification instanceof NotificationEvent)
            notificationPanel = new MessageNotificationPanel((NotificationEvent) notification, this, this.alertManager::saveAlerts, removeNotification);

        if (notificationPanel != null)
            this.notificationContainer.add(notificationPanel);
    }

    private Notification createNotification(NotificationType notificationType) {
        Injector injector = WatchdogPlugin.getInstance().getInjector();
        Notification notification = injector.getInstance(notificationType.getImplClass());
        notification.setAlert(this.alert);
        this.alert.getNotifications().add(notification);
        return notification;
    }
}
