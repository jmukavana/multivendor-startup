import { VendorApplicationForm } from "@/features/vendor-onboarding";
import { requireSession } from "@/shared/lib/guard";

export const metadata = {
  title: "Vendor application — Meridian",
};

export default async function VendorApplyPage() {
  // UC-01 step 1: the applicant already holds a verified account.
  // Reading the session also keeps this route out of the prerender, so the
  // submission id below is generated per request rather than baked in at build.
  await requireSession("/vendor/apply");

  const submissionId = crypto.randomUUID();

  return (
    <main className="mx-auto w-full max-w-xl px-6 py-12">
      <h1 className="text-2xl font-semibold tracking-tight">
        Apply to sell on Meridian
      </h1>
      <p className="mt-2 mb-8 text-sm opacity-70">
        We review applications within two business days. You will be asked for
        your payout details once your application is approved.
      </p>
      <VendorApplicationForm submissionId={submissionId} />
    </main>
  );
}
