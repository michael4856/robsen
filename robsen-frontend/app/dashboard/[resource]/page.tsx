import ResourceManager from "@/app/components/resource-manager";
import { resources } from "@/app/lib/resources";
import { Suspense } from "react";
import { notFound } from "next/navigation";

export function generateStaticParams() {
  return resources.map(({ key }) => ({ resource: key }));
}

type ResourcePageProps = {
  params: Promise<{ resource: string }>;
};

export default function ResourcePage({ params }: ResourcePageProps) {
  return (
    <Suspense fallback={<main className="dashboard-page"><div className="page-state">Loading records…</div></main>}>
      <ResolvedResourcePage params={params} />
    </Suspense>
  );
}

async function ResolvedResourcePage({ params }: ResourcePageProps) {
  const { resource } = await params;
  if (!resources.some((entry) => entry.key === resource)) notFound();
  return <ResourceManager resourceKey={resource} />;
}
