import { redirect } from "next/navigation";

import { SignInFlow, safeNextPath } from "@/features/auth";
import { getAccessToken } from "@/shared/lib/session";

export const metadata = {
  title: "Sign in — Meridian",
};

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { next } = await searchParams;
  const destination = safeNextPath(next);

  if (await getAccessToken()) {
    redirect(destination);
  }

  return (
    <main className="mx-auto w-full max-w-sm px-6 py-16">
      <SignInFlow next={destination} />
    </main>
  );
}
