"use client";

import { useEffect, useState, type FormEvent } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";

type ProfileForm = {
  fullName: string;
  email: string;
  username: string;
  phoneNumber: string;
  profilePicture: string;
};

const emptyProfile: ProfileForm = {
  fullName: "",
  email: "",
  username: "",
  phoneNumber: "",
  profilePicture: "",
};

export default function ProfilePage() {
  return <ProtectedArea><ProfileSettings /></ProtectedArea>;
}

function ProfileSettings() {
  const profile = useAuthProfile();
  const [values, setValues] = useState<ProfileForm>(emptyProfile);
  const [profileError, setProfileError] = useState("");
  const [profileNotice, setProfileNotice] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [passwordNotice, setPasswordNotice] = useState("");
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  useEffect(() => {
    if (!profile) return;
    const timeout = window.setTimeout(() => setValues({
      fullName: profile.fullName ?? "",
      email: profile.email ?? "",
      username: profile.username ?? "",
      phoneNumber: profile.phoneNumber ?? "",
      profilePicture: profile.profilePicture ?? "",
    }), 0);
    return () => window.clearTimeout(timeout);
  }, [profile]);

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingProfile(true);
    setProfileError("");
    setProfileNotice("");
    try {
      const response = await fetch("/api/auth/profile", {
        method: "PATCH",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(values),
      });
      const updated = await response.json();
      if (!response.ok) throw new Error(updated?.message ?? "Could not save your profile.");
      setValues({
        fullName: updated?.fullName ?? values.fullName,
        email: updated?.email ?? values.email,
        username: updated?.username ?? values.username,
        phoneNumber: updated?.phoneNumber ?? "",
        profilePicture: updated?.profilePicture ?? "",
      });
      window.dispatchEvent(new Event("robsen:profile-updated"));
      setProfileNotice("Your profile has been updated.");
    } catch (error) {
      setProfileError(error instanceof Error ? error.message : "Could not save your profile.");
    } finally {
      setSavingProfile(false);
    }
  }

  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const currentPassword = String(data.get("currentPassword") ?? "");
    const newPassword = String(data.get("newPassword") ?? "");
    const confirmPassword = String(data.get("confirmPassword") ?? "");
    setPasswordError("");
    setPasswordNotice("");
    if (newPassword !== confirmPassword) {
      setPasswordError("The new passwords do not match.");
      return;
    }
    setSavingPassword(true);
    try {
      await apiFetch<void>("auth/change-password", {
        method: "POST",
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      form.reset();
      setPasswordNotice("Your password has been changed.");
    } catch (error) {
      setPasswordError(error instanceof Error ? error.message : "Could not change your password.");
    } finally {
      setSavingPassword(false);
    }
  }

  async function choosePicture(file?: File) {
    setProfileError("");
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setProfileError("Choose an image file.");
      return;
    }
    if (file.size > 8 * 1024 * 1024) {
      setProfileError("Choose an image smaller than 8 MB.");
      return;
    }
    try {
      const dataUrl = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onerror = () => reject(new Error("Could not read this image."));
        reader.onload = () => resolve(String(reader.result));
        reader.readAsDataURL(file);
      });
      const image = await new Promise<HTMLImageElement>((resolve, reject) => {
        const loadedImage = new Image();
        loadedImage.onload = () => resolve(loadedImage);
        loadedImage.onerror = () => reject(new Error("This image could not be opened."));
        loadedImage.src = dataUrl;
      });
      const scale = Math.min(1, 512 / Math.max(image.width, image.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(image.width * scale));
      canvas.height = Math.max(1, Math.round(image.height * scale));
      const context = canvas.getContext("2d");
      if (!context) throw new Error("Could not process this image.");
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      setValues((current) => ({ ...current, profilePicture: canvas.toDataURL("image/jpeg", 0.84) }));
    } catch (error) {
      setProfileError(error instanceof Error ? error.message : "Could not process this image.");
    }
  }

  return (
    <main className="dashboard-page">
      <div className="dashboard-content profile-content">
        <div className="dashboard-heading">
          <div><p className="dashboard-eyebrow">ACCOUNT SETTINGS</p><h1>Your profile</h1><p className="dashboard-subtitle">Manage the information and sign-in details for your Robsen account.</p></div>
        </div>
        <section className="profile-panel">
          <div className="profile-panel-heading"><div><h2>Personal information</h2><p>These details identify you across your business network.</p></div></div>
          {profileError ? <p className="form-error">{profileError}</p> : null}
          {profileNotice ? <p className="dashboard-success">{profileNotice}</p> : null}
          <form className="profile-form" onSubmit={saveProfile}>
            <div className="profile-picture-field">
              <div className="profile-avatar" style={values.profilePicture ? { backgroundImage: `url(${values.profilePicture})` } : undefined} aria-label="Profile picture preview">
                {values.profilePicture ? null : (values.fullName || values.email || "U").slice(0, 1).toUpperCase()}
              </div>
              <div><label className="field-control"><span>Profile picture</span><input accept="image/png,image/jpeg,image/webp" type="file" onChange={(event) => void choosePicture(event.target.files?.[0])} /></label><small>PNG, JPEG, or WebP. Images are resized before saving.</small>{values.profilePicture ? <button className="text-button profile-remove-picture" onClick={() => setValues((current) => ({ ...current, profilePicture: "" }))} type="button">Remove picture</button> : null}</div>
            </div>
            <div className="profile-fields">
              <label className="field-control"><span>Full name</span><input autoComplete="name" required value={values.fullName} onChange={(event) => setValues({ ...values, fullName: event.target.value })} /></label>
              <label className="field-control"><span>Email address</span><input autoComplete="email" required type="email" value={values.email} onChange={(event) => setValues({ ...values, email: event.target.value })} /></label>
              <label className="field-control"><span>Username</span><input autoComplete="username" required value={values.username} onChange={(event) => setValues({ ...values, username: event.target.value })} /></label>
              <label className="field-control"><span>Phone number</span><input autoComplete="tel" type="tel" value={values.phoneNumber} onChange={(event) => setValues({ ...values, phoneNumber: event.target.value })} /></label>
            </div>
            <div className="profile-form-actions"><button className="primary-button" disabled={savingProfile} type="submit">{savingProfile ? "Saving…" : "Save profile"}</button></div>
          </form>
        </section>
        <section className="profile-panel password-panel">
          <div className="profile-panel-heading"><div><h2>Change password</h2><p>Use your current password to set a new one. It must be at least 8 characters.</p></div></div>
          {passwordError ? <p className="form-error">{passwordError}</p> : null}
          {passwordNotice ? <p className="dashboard-success">{passwordNotice}</p> : null}
          <form className="profile-fields password-fields" onSubmit={savePassword}>
            <label className="field-control"><span>Current password</span><input autoComplete="current-password" minLength={1} name="currentPassword" required type="password" /></label>
            <label className="field-control"><span>New password</span><input autoComplete="new-password" minLength={8} name="newPassword" required type="password" /></label>
            <label className="field-control"><span>Confirm new password</span><input autoComplete="new-password" minLength={8} name="confirmPassword" required type="password" /></label>
            <div className="profile-form-actions"><button className="primary-button" disabled={savingPassword} type="submit">{savingPassword ? "Updating…" : "Update password"}</button></div>
          </form>
        </section>
      </div>
    </main>
  );
}
