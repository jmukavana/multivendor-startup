interface FormAlertProps {
  readonly message: string;
  /** Shown alongside the message so support can find the request in the server logs. */
  readonly traceId?: string;
}

export function FormAlert({ message, traceId }: FormAlertProps) {
  return (
    <output className="block rounded-md border border-red-600/40 bg-red-600/10 px-3 py-2 text-sm text-red-700 dark:text-red-400">
      {message}
      {traceId && (
        <span className="mt-1 block font-mono text-xs opacity-70">
          Reference: {traceId}
        </span>
      )}
    </output>
  );
}
