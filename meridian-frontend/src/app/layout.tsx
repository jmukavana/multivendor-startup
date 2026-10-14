import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";

import { CartProvider } from "@/features/cart";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains-mono",

  display: "swap",
});

export const metadata: Metadata = {
  title: "Meridian — Multi-Vendor Marketplace",
  description:
    "Kenya's trusted multi-vendor marketplace with M-PESA integration.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${inter.variable} ${jetbrainsMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <CartProvider>{children}</CartProvider>
      </body>
    </html>
  );
}
