"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef } from "react";
import {
  SESSION_EXPIRED_EVENT,
  SESSION_EXPIRED_MESSAGE,
  SESSION_NOTICE_KEY,
  type SessionExpiredDetail,
} from "@/app/lib/auth";

export default function SessionExpiredHandler() {
  const pathname = usePathname();
  const router = useRouter();
  const handlingRef = useRef(false);

  useEffect(() => {
    const handleExpired = (event: Event) => {
      if (handlingRef.current) return;
      handlingRef.current = true;

      const detail = (event as CustomEvent<SessionExpiredDetail>).detail;
      window.sessionStorage.setItem(
        SESSION_NOTICE_KEY,
        detail?.message ?? SESSION_EXPIRED_MESSAGE,
      );
      router.replace("/");
      router.refresh();
    };

    window.addEventListener(SESSION_EXPIRED_EVENT, handleExpired);
    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, handleExpired);
    };
  }, [router]);

  useEffect(() => {
    if (pathname !== "/") handlingRef.current = false;
  }, [pathname]);

  return null;
}
