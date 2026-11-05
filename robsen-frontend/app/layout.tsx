import type { Metadata } from "next";
import { Suspense } from "react";
import Navigation from "./components/navigation";
import SessionExpiredHandler from "./components/session-expired-handler";
import "./globals.css";

export const metadata: Metadata = {
  title: "Robsen | Distribution, connected",
  description: "A clearer way to manage your distribution network.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <SessionExpiredHandler />
        <Suspense fallback={<div aria-hidden="true" className="navigation-loading" />}>
          <Navigation />
        </Suspense>
        {children}
      </body>
    </html>
  );
}
