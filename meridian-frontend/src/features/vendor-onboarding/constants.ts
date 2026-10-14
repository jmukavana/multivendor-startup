/**
 * Re-exported from the shared tag registry so every slice's tags are declared in one place.
 * Kept out of actions.ts: a "use server" module may only export async functions, and adding a
 * constant to one strips every export off the module.
 */
export { VENDOR_APPLICATION_TAG } from "@/shared/cache/tags";
