import Link from "next/link";

export const metadata = {
  title: "Application submitted — Meridian",
};

export default async function VendorApplicationSubmittedPage({
  searchParams,
}: PageProps<"/vendor/apply/submitted">) {
  const { id } = await searchParams;

  return (
    <main className="mx-auto w-full max-w-xl px-6 py-12">
      <h1 className="text-2xl font-semibold tracking-tight">
        Application received
      </h1>
      <p className="mt-2 text-sm opacity-70">
        Your application is in the review queue. We will notify you by SMS when
        its status changes.
      </p>
      {typeof id === "string" && (
        <p className="mt-6 text-sm">
          Reference: <span className="font-mono">{id}</span>
        </p>
      )}
      <Link
        href="/"
        className="mt-8 inline-block text-sm underline underline-offset-4"
      >
        Back to Meridian
      </Link>
    </main>
  );
}
