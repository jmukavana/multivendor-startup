interface SubmitButtonProps {
  readonly children: React.ReactNode;
  readonly pending: boolean;
  readonly pendingLabel: string;
}

export function SubmitButton({
  children,
  pending,
  pendingLabel,
}: SubmitButtonProps) {
  return (
    <button
      type="submit"
      disabled={pending}
      className="mt-2 rounded-md bg-foreground px-4 py-2.5 text-sm font-medium text-background disabled:opacity-50"
    >
      {pending ? pendingLabel : children}
    </button>
  );
}
