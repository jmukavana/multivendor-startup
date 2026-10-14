"use client";

import { useState, useTransition } from "react";

import {
  acceptSubOrder,
  cancelSubOrder,
  markSubOrderDelivered,
} from "@/features/orders/actions";
import { Button } from "@/shared/components/ui/button";

interface SubOrderActionsProps {
  readonly subOrderId: string;
  readonly status: string;
  readonly cancellable: boolean;
}

/** The vendor's own transitions on one of its sub-orders — see each action's own doc for who may call it. */
export function SubOrderActions({
  subOrderId,
  status,
  cancellable,
}: SubOrderActionsProps) {
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function run(
    action: (input: unknown) => Promise<{ status: string; message?: string }>,
  ) {
    setError(null);
    startTransition(async () => {
      const result = await action({ subOrderId });
      if (result.status === "error")
        setError(result.message ?? "That did not work.");
    });
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <div className="flex gap-2">
        {status === "CONFIRMED" && (
          <Button
            size="sm"
            disabled={pending}
            onClick={() => run(acceptSubOrder)}
          >
            Accept
          </Button>
        )}
        {status === "ACCEPTED" && (
          <Button
            size="sm"
            disabled={pending}
            onClick={() => run(markSubOrderDelivered)}
          >
            Mark delivered
          </Button>
        )}
        {cancellable && (
          <Button
            size="sm"
            variant="outline"
            disabled={pending}
            onClick={() => run(cancelSubOrder)}
          >
            Cancel
          </Button>
        )}
      </div>
      {error && <p className="text-label-sm text-error">{error}</p>}
    </div>
  );
}
