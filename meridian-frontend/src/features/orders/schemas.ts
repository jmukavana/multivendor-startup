import { z } from "zod";

export const subOrderIdSchema = z.object({ subOrderId: z.uuid() });

export type SubOrderIdInput = z.infer<typeof subOrderIdSchema>;
