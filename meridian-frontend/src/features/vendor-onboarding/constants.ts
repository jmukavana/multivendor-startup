/**
 * Cache tags shared between readers (Server Components) and writers (Server Actions).
 * These cannot live in actions.ts — a "use server" module may only export async functions.
 */
export const VENDOR_APPLICATION_TAG = "vendor-application";
