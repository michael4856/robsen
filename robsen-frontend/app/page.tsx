"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

type LoginForm = {
  identifier: string;
  password: string;
};

export default function Home() {
  const router = useRouter();
  const [isRegistering, setIsRegistering] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    const formData = new FormData(event.currentTarget);
    const password = String(formData.get("password") ?? "");

    try {
      if (isRegistering) {
        const registrationResponse = await fetch("/api/auth/register", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            fullName: formData.get("fullName"),
            email: formData.get("email"),
            username: formData.get("username"),
            phoneNumber: formData.get("phoneNumber"),
            password,
          }),
        });
        if (!registrationResponse.ok) {
          throw new Error(await readError(registrationResponse));
        }
      }

      const credentials: LoginForm = {
        identifier: String(formData.get(isRegistering ? "email" : "identifier") ?? ""),
        password,
      };
      const loginResponse = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(credentials),
      });
      if (!loginResponse.ok) {
        throw new Error(await readError(loginResponse));
      }
      router.push("/dashboard");
      router.refresh();
    } catch (submissionError) {
      setError(
        submissionError instanceof Error
          ? submissionError.message
          : "Could not complete the request.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main>
      <section className="hero" id="platform">
        <div className="hero-copy">
          <div className="eyebrow">
            <span className="eyebrow-dot" />
            A better way to do business
          </div>
          <h1>
            Good business
            <br />
            moves <span>together.</span>
          </h1>
          <p className="hero-description">
            Bring businesses, products, and distribution opportunities into one
            clear view. Build partnerships that move your business forward.
          </p>

          <div className="hero-proof">
            <div className="proof-avatars" aria-hidden="true">
              <span>J</span>
              <span>M</span>
              <span>A</span>
              <span className="proof-plus">+</span>
            </div>
            <p>
              Built for the people
              <br />
              behind every good partnership.
            </p>
          </div>

          <div className="hero-orbit orbit-one" aria-hidden="true" />
          <div className="hero-orbit orbit-two" aria-hidden="true" />
          <div className="hero-sparkle" aria-hidden="true">
            ✳
          </div>
        </div>

        <div className="login-column" id="login">
          <div className="login-card">
            <div className="card-heading">
              <div className="card-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path
                    d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"
                    stroke="currentColor"
                    strokeWidth="1.7"
                  />
                  <path
                    d="M5 20a7 7 0 0 1 14 0"
                    stroke="currentColor"
                    strokeWidth="1.7"
                    strokeLinecap="round"
                  />
                </svg>
              </div>
              <span className="secure-label">
                {isRegistering ? "CREATE YOUR ACCOUNT" : "SECURE ACCESS"}
              </span>
            </div>

            <h2>{isRegistering ? "Join Robsen" : "Welcome back"}</h2>
            <p className="card-description">
              {isRegistering
                ? "Create an account to start building better business connections."
                : "Sign in to your Robsen account to pick up where you left off."}
            </p>

            <form className="login-form" onSubmit={submit}>
              {isRegistering ? (
                <>
                  <label htmlFor="fullName">Full name</label>
                  <input
                    autoComplete="name"
                    id="fullName"
                    name="fullName"
                    placeholder="Your name"
                    required
                  />
                  <label htmlFor="email">Email address</label>
                  <input
                    autoComplete="email"
                    id="email"
                    name="email"
                    placeholder="you@company.com"
                    required
                    type="email"
                  />
                  <label htmlFor="username">Username</label>
                  <input
                    autoComplete="username"
                    id="username"
                    name="username"
                    placeholder="Choose a username"
                    required
                  />
                  <label htmlFor="phoneNumber">Phone number</label>
                  <input
                    autoComplete="tel"
                    id="phoneNumber"
                    name="phoneNumber"
                    placeholder="Optional"
                    type="tel"
                  />
                </>
              ) : (
                <>
                  <label htmlFor="identifier">Email or username</label>
                  <input
                    autoComplete="username"
                    id="identifier"
                    name="identifier"
                    placeholder="you@company.com"
                    required
                  />
                </>
              )}

              <label htmlFor="password">Password</label>
              <input
                autoComplete={isRegistering ? "new-password" : "current-password"}
                id="password"
                minLength={isRegistering ? 8 : undefined}
                name="password"
                placeholder={isRegistering ? "At least 8 characters" : "Enter your password"}
                required
                type="password"
              />

              <button
                className="sign-in-button"
                disabled={isSubmitting}
                type="submit"
              >
                {isSubmitting
                  ? "Please wait…"
                  : isRegistering
                    ? "Create account"
                    : "Sign in"}
                <span aria-hidden="true">→</span>
              </button>
              <p aria-live="polite" className="form-notice">
                {error}
              </p>
            </form>

            <div className="card-footer">
              Passwords are securely encrypted before they are stored.
            </div>
          </div>
          <p className="login-help">
            {isRegistering ? "Already have an account?" : "New to Robsen?"}{" "}
            <button
              className="text-button"
              onClick={() => {
                setError("");
                setIsRegistering((value) => !value);
              }}
              type="button"
            >
              {isRegistering ? "Sign in" : "Create an account"}
            </button>
          </p>
        </div>
      </section>

      <section aria-label="Robsen platform" className="platform-strip">
        <div className="strip-intro">
          <span className="strip-kicker">THE ROBSEN PLATFORM</span>
          <p>Everything your distribution network needs to move forward.</p>
        </div>
        <article id="businesses">
          <span className="feature-number">01</span>
          <h3>Business connections</h3>
          <p>Manage the relationships and memberships that keep you growing.</p>
        </article>
        <article id="products">
          <span className="feature-number">02</span>
          <h3>Products &amp; opportunity</h3>
          <p>Bring products, territories, and distribution opportunities together.</p>
        </article>
      </section>

      <footer className="site-footer">
        <span>© 2026 Robsen</span>
        <span>Better business, together.</span>
      </footer>
    </main>
  );
}

async function readError(response: Response) {
  const result = (await response.json().catch(() => null)) as
    | { message?: string; error?: string }
    | null;
  return result?.message ?? result?.error ?? `Request failed (${response.status}).`;
}
