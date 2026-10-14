package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.notification.NotificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationMessageRepository extends JpaRepository<NotificationMessageEntity, UUID> {

    List<NotificationMessageEntity> findByStatus(NotificationStatus status);

    /** Ops/test visibility into delivery history for one recipient — {@code AdminNotificationController}'s lookup. */
    List<NotificationMessageEntity> findByRecipientOrderByCreatedAtDesc(String recipient);
}
