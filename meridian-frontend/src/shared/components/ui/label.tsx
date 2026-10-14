"use client";

import * as React from "react";
import { cn } from "@/shared/lib/utils";

const Label = React.forwardRef<
  HTMLLabelElement,
  React.LabelHTMLAttributes<HTMLLabelElement>
>(({ className, ...props }, ref) => (
  // biome-ignore lint/a11y/noLabelWithoutControl: htmlFor arrives through ...props at every call site
  <label
    ref={ref}
    className={cn(
      "text-label-md font-medium text-on-surface mb-1.5 block",
      className,
    )}
    {...props}
  />
));
Label.displayName = "Label";

export { Label };
