import type { IsoInstant, Uuid } from "@/shared/api";

export type NotificationChannel = "SMS" | "EMAIL";

export type NotificationStatus =
  | "PENDING"
  | "SENT"
  | "FAILED"
  | "DEAD_LETTERED";

/** Mirrors `notification.NotificationMessage`. */
export interface NotificationMessage {
  readonly id: Uuid;
  readonly channel: NotificationChannel;
  readonly recipient: string;
  readonly subject: string | null;
  readonly body: string;
  readonly status: NotificationStatus;
  readonly attemptCount: number;
  readonly createdAt: IsoInstant;
  readonly sentAt: IsoInstant | null;
}

/**
 * Mirrors `NotificationPreferenceController.MarketingPreference`.
 *
 * Marketing is the only opt-out: transactional messages about an order someone placed are not
 * something they can switch off and still be told what happened to it.
 */
export interface MarketingPreference {
  readonly marketingOptOut: boolean;
}
