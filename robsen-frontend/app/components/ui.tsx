import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from "react";

type Tone = "neutral" | "success" | "warning" | "danger";

const buttonVariantClasses = {
  primary: "ui-button",
  secondary: "ui-button-secondary",
  danger: "ui-button-danger",
  quiet: "ui-button-quiet",
} as const;

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: keyof typeof buttonVariantClasses;
};

export function Button({ variant = "primary", className = "", ...props }: ButtonProps) {
  return <button className={[buttonVariantClasses[variant], className].filter(Boolean).join(" ")} {...props} />;
}

export function Panel({ className = "", ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={["ui-panel", className].filter(Boolean).join(" ")} {...props} />;
}

export function Alert({ tone = "neutral", className = "", children, ...props }: HTMLAttributes<HTMLDivElement> & { tone?: Tone; children: ReactNode }) {
  return (
    <div role={tone === "danger" ? "alert" : "status"} data-tone={tone} className={["ui-alert", className].filter(Boolean).join(" ")} {...props}>
      {children}
    </div>
  );
}

export function Badge({ className = "", ...props }: HTMLAttributes<HTMLSpanElement>) {
  return <span className={["ui-badge", className].filter(Boolean).join(" ")} {...props} />;
}

export function PageHeading({ title, description, level = 1, className = "" }: { title: ReactNode; description?: ReactNode; level?: 1 | 2; className?: string }) {
  const Heading = level === 1 ? "h1" : "h2";
  return (
    <header className={["ui-page-heading", className].filter(Boolean).join(" ")}>
      <Heading>{title}</Heading>
      {description ? <p>{description}</p> : null}
    </header>
  );
}
