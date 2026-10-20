import { ChangeEvent, FormEvent, ReactNode, useCallback, useEffect, useState } from "react";
import { allowedNextStatuses, ApiError, assignComplaintDepartment, Complaint, ComplaintStatus, createComplaint, Department, displayStatus, listComplaints, listDepartments, trackComplaint, updateComplaintStatus, uploadComplaintAttachment } from "./complaintsApi";

type Status = Complaint["status"];
const categories = [["Garbage Collection", "Waste collection, overflowing bins"], ["Roads & Potholes", "Damaged roads and unsafe potholes"], ["Drainage Problems", "Blocked drains and waterlogging"], ["Streetlight Issues", "Faulty or missing streetlights"], ["Water Supply", "Low pressure or irregular supply"]];

const Icon = ({ name, size = 20 }: { name: string; size?: number }) => {
  const paths: Record<string, string> = { pin: "M12 21s7-5.2 7-12a7 7 0 1 0-14 0c0 6.8 7 12 7 12Zm0-9.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z", report: "M12 3 2.7 19.1a1 1 0 0 0 .9 1.4h16.8a1 1 0 0 0 .9-1.4L12 3Zm0 5v5m0 3h.01", search: "m20 20-4.2-4.2m2.2-5.3a7.5 7.5 0 1 1-15 0 7.5 7.5 0 0 1 15 0Z", plus: "M12 5v14M5 12h14", arrow: "m9 18 6-6-6-6", home: "m3 11 9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1v-9Z", check: "m5 12 4 4L19 6", camera: "M4 7h3l1.5-2h7L17 7h3a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2Zm8 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z", menu: "M4 7h16M4 12h16M4 17h16", close: "m6 6 12 12M18 6 6 18", building: "M4 21V5a1 1 0 0 1 1-1h6v17M15 9h4a1 1 0 0 1 1 1v11M2 21h20M8 8h2m-2 4h2m-2 4h2m7-2h1" };
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={paths[name] || paths.report} /></svg>;
};
function StatusBadge({ status }: { status: Status }) { return <span className={`status status-${status.toLowerCase().replace(" ", "-")}`}>{status}</span>; }
function Brand({ compact = false }: { compact?: boolean }) { return <div className="brand"><span className="brand-mark"><Icon name="building" size={19} /></span>{!compact && <span>Civic<span>Connect</span></span>}</div>; }
function ErrorNotice({ message, retry }: { message: string; retry?: () => void }) { return <div className="empty-state"><h2>We couldn&apos;t reach CivicConnect</h2><p>{message}</p>{retry && <button className="btn btn-secondary" onClick={retry}>Try again</button>}</div>; }

const PAGE_PATHS: Record<string, string> = {
  home: "/home",
  report: "/report",
  track: "/track",
  my: "/my-complaints",
  staff: "/staff",
  entry: "/entry",
};

function getPageFromPath(path: string): string {
  const normalized = path.split("?")[0].replace(/\/+$/, "") || "/";
  if (normalized === "/") return "entry";
  for (const [page, pagePath] of Object.entries(PAGE_PATHS)) {
    if (pagePath === normalized) return page;
  }
  if (normalized.startsWith("/track/")) return "track";
  return "home";
}

function pushPage(page: string, complaintId?: string) {
  const basePath = PAGE_PATHS[page] || "/";
  const fullPath = complaintId && page === "track" ? `${basePath}/${encodeURIComponent(complaintId)}` : basePath;
  if (window.location.pathname !== fullPath) {
    window.history.pushState({ page, complaintId }, "", fullPath);
  }
}

function replacePage(page: string, complaintId?: string) {
  const basePath = PAGE_PATHS[page] || "/";
  const fullPath = complaintId && page === "track" ? `${basePath}/${encodeURIComponent(complaintId)}` : basePath;
  window.history.replaceState({ page, complaintId }, "", fullPath);
}

export default function App() {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [page, setPage] = useState(() => getPageFromPath(window.location.pathname));
  const [selected, setSelected] = useState<Complaint | null>(null);
  const [menu, setMenu] = useState(false);
  const [reportCategory, setReportCategory] = useState("");

  const reload = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      setComplaints(await listComplaints());
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : "The API is unavailable.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void reload(); }, [reload]);

  const handlePopState = useCallback((event: PopStateEvent) => {
    if (event.state?.page) {
      setPage(event.state.page);
      setSelected(event.state.complaintId ? null : null);
    } else {
      const pageFromPath = getPageFromPath(window.location.pathname);
      setPage(pageFromPath);
    }
    setMenu(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, []);

  useEffect(() => {
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [handlePopState]);

  const go = useCallback((next: string, complaint?: Complaint) => {
    setPage(next);
    setSelected(complaint || null);
    setMenu(false);
    pushPage(next, complaint?.id);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, []);

  const addComplaint = async (input: { title: string; category: string; description: string; locality: string; landmark?: string }) => {
    const created = await createComplaint(input);
    setComplaints((all) => [created, ...all.filter((item) => item.apiId !== created.apiId)]);
    return created;
  };

  const replaceComplaint = (updated: Complaint) => {
    setComplaints((all) => all.map((item) => item.apiId === updated.apiId ? updated : item));
    setSelected(updated);
  };

  return (
    <div className="app-shell">
      {page !== "entry" && (page === "staff" ? <OfficerHeader go={go} /> : <Header page={page} go={go} menu={menu} setMenu={setMenu} />)}
      <main>
        {page === "entry" && <RoleEntry go={go} />}
        {page === "home" && <Home go={go} chooseCategory={(category) => { setReportCategory(category); go("report"); }} />}
        {page === "report" && <Report go={go} create={addComplaint} initialCategory={reportCategory} refresh={reload} />}
        {page === "track" && <Track go={go} initial={selected?.id} />}
        {page === "my" && <Resident complaints={complaints} go={go} loading={loading} error={loadError} retry={reload} />}
        {page === "staff" && <Staff complaints={complaints} go={go} update={replaceComplaint} loading={loading} error={loadError} retry={reload} />}
      </main>
      {page !== "entry" && <Footer go={go} refresh={reload} />}
    </div>
  );
}

function Header({ page, go, menu, setMenu }: { page: string; go: (p: string) => void; menu: boolean; setMenu: (v: boolean) => void }) { const links = [["home", "Home"], ["report", "Report an Issue"], ["track", "Track Complaint"], ["my", "My Complaints"]]; return <header className="site-header"><div className="nav-wrap"><button className="brand-button" onClick={() => go("home")} aria-label="CivicConnect home"><Brand /></button><nav className={menu ? "nav-links open" : "nav-links"} aria-label="Citizen navigation">{links.map(([key, label]) => <button key={key} className={page === key ? "active" : ""} onClick={() => go(key)}>{label}</button>)}<button className="role-switch" onClick={() => go("entry")}>Switch workspace</button></nav><button className="menu-button" onClick={() => setMenu(!menu)} aria-label="Toggle menu"><Icon name={menu ? "close" : "menu"} /></button></div></header>; }
function OfficerHeader({ go }: { go: (p: string) => void }) { return <header className="site-header officer-header"><div className="nav-wrap"><button className="brand-button" onClick={() => go("staff")} aria-label="CivicConnect officer dashboard"><Brand /></button><div className="officer-label"><Icon name="building" size={16} /> Officer workspace</div><button className="text-button" onClick={() => go("entry")}>Switch role</button></div></header>; }
function RoleEntry({ go }: { go: (p: string) => void }) { return <section className="entry-page"><div className="entry-card"><Brand /><p className="eyebrow">CivicConnect demonstration</p><h1>Choose your workspace</h1><p className="page-intro">Select how you&apos;re using CivicConnect today. This chooses the demonstration view only; it is not a secure sign-in.</p><div className="role-options"><button className="role-option" onClick={() => go("home")}><span className="role-icon"><Icon name="home" size={25} /></span><span><strong>Citizen</strong><small>Report local issues, track updates, and view complaint records.</small></span><Icon name="arrow" size={19} /></button><button className="role-option officer" onClick={() => go("staff")}><span className="role-icon"><Icon name="building" size={25} /></span><span><strong>Officer</strong><small>Review complaints, assign departments, and update case progress.</small></span><Icon name="arrow" size={19} /></button></div><p className="entry-note">Authentication and role-based access control are not implemented in this demonstration build.</p></div></section>; }

function Home({ go, chooseCategory }: { go: (p: string) => void; chooseCategory: (category: string) => void }) { return <><section className="hero"><div className="container hero-grid"><div><p className="eyebrow">A better way to care for your locality</p><h1>Let&apos;s Make Our Neighbourhood Better.</h1><p className="hero-copy">Report local problems, follow every update, and help make your community cleaner, safer, and better connected.</p><div className="action-row"><button className="btn btn-primary" onClick={() => go("report")}><Icon name="report" />Report a Complaint</button><button className="btn btn-secondary" onClick={() => go("track")}><Icon name="search" />Track a Complaint</button></div></div><div className="civic-art" aria-label="Illustration of a civic neighbourhood"><div className="sun" /><div className="art-road" /><div className="art-building b1" /><div className="art-building b2" /><div className="art-building b3" /><div className="art-tree t1" /><div className="art-tree t2" /><div className="art-pin"><Icon name="pin" size={25} /></div></div></div></section><section className="section container"><div className="section-heading"><div><p className="eyebrow">Quick reporting</p><h2>What needs attention?</h2><p>Choose a category to begin your report with the right details selected.</p></div></div><div className="category-grid">{categories.map(([name, desc], i) => <button className="category-tile" key={name} onClick={() => chooseCategory(name)}><span className={`category-icon icon-${i}`}><Icon name={["report", "arrow", "pin", "home", "camera"][i]} /></span><span><strong>{name}</strong><small>{desc}</small></span><Icon name="arrow" size={18} /></button>)}</div></section><section className="how-section"><div className="container"><p className="eyebrow">Simple and transparent</p><h2>How CivicConnect works</h2><div className="steps">{[["01", "Report", "Tell us about the problem with clear location details."], ["02", "Track", "Follow your complaint using its unique reference number."], ["03", "Resolve", "See progress and resolution updates from the team."]].map(([n, title, copy]) => <div className="step" key={n}><span>{n}</span><h3>{title}</h3><p>{copy}</p></div>)}</div></div></section><section className="container transparency"><div><span className="transparency-icon"><Icon name="check" /></span><div><p className="eyebrow">Built for clarity</p><h2>Every report has a reference.</h2><p>When you submit a complaint, you receive a unique reference number to check its latest status whenever you need to.</p></div></div><button className="text-button" onClick={() => go("track")}>Track a complaint <Icon name="arrow" size={17} /></button></section></>; }

function Report({ go, create, initialCategory, refresh }: { go: (p: string, c?: Complaint) => void; create: (input: { title: string; category: string; description: string; locality: string; landmark?: string }) => Promise<Complaint>; initialCategory: string; refresh: () => Promise<void> }) {
  const [form, setForm] = useState({ title: "", category: initialCategory, description: "", locality: "", landmark: "" }); const [errors, setErrors] = useState<Record<string, string>>({}); const [saving, setSaving] = useState(false); const [submitError, setSubmitError] = useState(""); const [done, setDone] = useState<Complaint | null>(null); const [file, setFile] = useState<File | null>(null); const [preview, setPreview] = useState(""); const [fileError, setFileError] = useState(""); const [uploadError, setUploadError] = useState("");
  const change = (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => setForm({ ...form, [event.target.name]: event.target.value });
  const clearFile = () => { if (preview) URL.revokeObjectURL(preview); setFile(null); setPreview(""); setFileError(""); };
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview); }, [preview]);
  const chooseFile = (event: ChangeEvent<HTMLInputElement>) => { const selected = event.target.files?.[0]; event.target.value = ""; if (!selected) return; if (!["image/jpeg", "image/png", "image/webp"].includes(selected.type)) { setFileError("Choose a JPEG, PNG, or WebP image."); return; } if (selected.size > 5 * 1024 * 1024) { setFileError("Each image must be 5 MB or smaller."); return; } clearFile(); setFile(selected); setPreview(URL.createObjectURL(selected)); };
  const sendAttachment = async (complaint: Complaint, image: File) => { setSaving(true); setUploadError(""); try { await uploadComplaintAttachment(complaint.apiId, image); await refresh(); } catch (error) { setUploadError(error instanceof Error ? error.message : "Your complaint was created, but the image upload failed."); } finally { setSaving(false); } };
  const submit = async (event: FormEvent) => { event.preventDefault(); if (saving) return; const next: Record<string, string> = {}; ["title", "category", "description", "locality"].forEach((key) => { if (!form[key as keyof typeof form].trim()) next[key] = "This field is required."; }); setErrors(next); if (Object.keys(next).length || fileError) return; setSaving(true); setSubmitError(""); try { const complaint = await create(form); setDone(complaint); if (file) await sendAttachment(complaint, file); } catch (error) { setSubmitError(error instanceof Error ? error.message : "Unable to submit the complaint."); } finally { setSaving(false); } };
  if (done) return <section className="container narrow page-section"><div className="success-panel"><span className="success-check"><Icon name="check" size={29} /></span><p className="eyebrow">Complaint submitted</p><h1>Thank you for reporting this.</h1><p>Your complaint reference number is</p><strong className="reference">{done.id}</strong>{uploadError ? <div className="upload-warning"><strong>Your complaint was saved, but its image was not uploaded.</strong><p>{uploadError}</p>{file && <button className="btn btn-secondary" disabled={saving} onClick={() => void sendAttachment(done, file)}>{saving ? "Retrying image upload..." : "Retry image upload"}</button>}</div> : <p className="muted">{file ? "Your image was attached. " : ""}Please save this number to follow the latest updates on your report.</p>}<div className="action-row centred"><button className="btn btn-primary" onClick={() => go("track", done)}>Track This Complaint</button><button className="btn btn-secondary" onClick={() => go("home")}>Return Home</button></div></div></section>;
  return <section className="container narrow page-section"><p className="eyebrow">Resident services</p><h1>Report a Local Issue</h1><p className="page-intro">Share a few details so the appropriate municipal team can review the problem.</p><form className="report-form" onSubmit={submit} noValidate><Field label="Complaint title" error={errors.title}><input name="title" value={form.title} onChange={change} placeholder="e.g. Pothole near the school entrance" /></Field><Field label="Complaint category" error={errors.category}><select name="category" value={form.category} onChange={change}><option value="">Select a category</option>{categories.map(([category]) => <option key={category}>{category}</option>)}</select></Field><Field label="Description" error={errors.description} hint="Include what you noticed and how it affects the area."><textarea name="description" value={form.description} onChange={change} placeholder="Describe the problem clearly..." rows={5} /></Field><div className="two-cols"><Field label="Locality or ward" error={errors.locality}><input name="locality" value={form.locality} onChange={change} placeholder="e.g. Ward 5" /></Field><Field label="Landmark (optional)"><input name="landmark" value={form.landmark} onChange={change} placeholder="e.g. Near Central Bus Stop" /></Field></div><Field label="Photo (optional)" error={fileError} hint="JPEG, PNG, or WebP — maximum 5 MB per image.">{preview ? <div className="photo-preview"><img src={preview} alt="Selected complaint attachment preview" /><button type="button" onClick={clearFile}>Remove photo</button></div> : <div className="upload-box"><Icon name="camera" size={25} /><span><strong>Choose a photo</strong><small>Add a clear image of the issue.</small></span><input type="file" accept="image/jpeg,image/png,image/webp" onChange={chooseFile} /></div>}</Field><div className="privacy-note"><Icon name="report" size={18} /><p><strong>Privacy note:</strong> Please do not include sensitive personal information in your description.</p></div>{submitError && <p className="error">{submitError}</p>}<button className="btn btn-primary submit" disabled={saving}>{saving ? (file ? "Submitting complaint and image..." : "Submitting complaint...") : "Submit Complaint"} <Icon name="arrow" /></button></form></section>;
}

function Field({ label, hint, error, children }: { label: string; hint?: string; error?: string; children: ReactNode }) { return <label className="field"><span>{label}</span>{children}{error ? <small className="error">{error}</small> : hint ? <small>{hint}</small> : null}</label>; }
function Track({ go, initial }: { go: (p: string, c?: Complaint) => void; initial?: string }) { const [query, setQuery] = useState(initial || ""); const [result, setResult] = useState<Complaint | null>(null); const [searched, setSearched] = useState(false); const [loading, setLoading] = useState(false); const [error, setError] = useState(""); const find = async (reference = query) => { if (!reference.trim()) return; setLoading(true); setError(""); setResult(null); setSearched(true); try { setResult(await trackComplaint(reference.trim())); } catch (reason) { if (!(reason instanceof ApiError) || !reason.message.includes("404")) setError(reason instanceof Error ? reason.message : "Unable to track that complaint."); } finally { setLoading(false); } }; useEffect(() => { if (initial) void find(initial); }, [initial]); return <section className="container narrow page-section"><p className="eyebrow">Follow progress</p><h1>Track Your Complaint</h1><p className="page-intro">Enter your reference number to see the latest progress.</p><form className="track-form" onSubmit={(event) => { event.preventDefault(); void find(); }}><label><span className="sr-only">Complaint reference number</span><Icon name="search" size={19} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="e.g. CC-2026-0001" /></label><button className="btn btn-primary" disabled={loading}>{loading ? "Tracking..." : "Track Complaint"}</button></form>{error && <ErrorNotice message={error} />}{result && <ComplaintDetail complaint={result} />}{searched && !loading && !result && !error && <div className="empty-state"><span className="empty-icon"><Icon name="search" size={25} /></span><h2>We couldn&apos;t find that reference</h2><p>Check the number and try again. You can also submit a new complaint if you have not reported it yet.</p><button className="btn btn-secondary" onClick={() => go("report")}><Icon name="plus" />Report an Issue</button></div>}</section>; }
function ComplaintDetail({ complaint }: { complaint: Complaint }) { return <article className="complaint-detail"><div className="detail-top"><div><p className="detail-id">{complaint.id}</p><h2>{complaint.title}</h2><p>{complaint.category} · Submitted {complaint.date}</p></div><StatusBadge status={complaint.status} /></div><div className="timeline">{complaint.history.map((history, index) => <div className={`timeline-step completed ${index === complaint.history.length - 1 ? "current" : ""}`} key={`${history.status}-${index}`}><span>{index < complaint.history.length - 1 ? <Icon name="check" size={14} /> : index + 1}</span><div><strong>{history.status}</strong><small>{history.date}</small></div></div>)}</div><div className="detail-grid"><div><span>Location</span><p>{complaint.locality}{complaint.landmark ? ` · ${complaint.landmark}` : ""}</p></div>{complaint.department && <div><span>Assigned department</span><p>{complaint.department}</p></div>}</div>{complaint.attachments.length > 0 && <div className="attachment-gallery"><span>Attached photos</span><div>{complaint.attachments.map((attachment) => <a href={attachment.url} target="_blank" rel="noreferrer" key={attachment.id}><img src={attachment.url} alt={attachment.fileName} /><small>{attachment.fileName}</small></a>)}</div></div>}{complaint.remarks && <div className={`remarks ${complaint.status === "Rejected" ? "rejection" : ""}`}><strong>{complaint.status === "Rejected" ? "Reason for rejection" : "Latest update"}</strong><p>{complaint.remarks}</p></div>}</article>; }
function Resident({ complaints, go, loading, error, retry }: { complaints: Complaint[]; go: (p: string, c?: Complaint) => void; loading: boolean; error: string; retry: () => void }) { const [filter, setFilter] = useState("All"); const [query, setQuery] = useState(""); const statuses: (Status | "All")[] = ["All", "Submitted", "Under Review", "In Progress", "Resolved", "Rejected", "Reopened"]; const rows = complaints.filter((complaint) => (filter === "All" || complaint.status === filter) && `${complaint.id} ${complaint.title}`.toLowerCase().includes(query.toLowerCase())); return <section className="container page-section"><div className="title-with-action"><div><h1>My Complaints</h1><p className="page-intro">Current complaint records. Authentication is not implemented in this version.</p></div><button className="btn btn-primary" onClick={() => go("report")}><Icon name="plus" />Report New Issue</button></div><div className="summary-strip">{statuses.slice(1).map((status) => <div key={status}><span>{complaints.filter((complaint) => complaint.status === status).length}</span><p>{status}</p></div>)}</div><div className="list-controls"><div className="filter-tabs">{statuses.map((status) => <button key={status} className={filter === status ? "selected" : ""} onClick={() => setFilter(status)}>{status}</button>)}</div><label className="search-field"><Icon name="search" size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search reference or title" /></label></div>{loading ? <div className="empty-state"><p>Loading complaints…</p></div> : error ? <ErrorNotice message={error} retry={retry} /> : <ComplaintList rows={rows} go={go} />}</section>; }
function ComplaintList({ rows, go }: { rows: Complaint[]; go: (p: string, c?: Complaint) => void }) { return <div className="complaint-list">{rows.length ? rows.map((complaint) => <article className="list-row" key={complaint.apiId}><div><strong className="detail-id">{complaint.id}</strong><h3>{complaint.title}</h3><p>{complaint.category} · {complaint.date}</p></div><StatusBadge status={complaint.status} /><button className="text-button" onClick={() => go("track", complaint)}>View details <Icon name="arrow" size={16} /></button></article>) : <div className="empty-state"><h2>No complaints found</h2><p>Try a different filter or search term.</p></div>}</div>; }
function Staff({ complaints, go, update, loading, error, retry }: { complaints: Complaint[]; go: (p: string, c?: Complaint) => void; update: (complaint: Complaint) => void; loading: boolean; error: string; retry: () => void }) {
  const [query, setQuery] = useState(""); const [status, setStatus] = useState("All"); const [category, setCategory] = useState("All"); const [locality, setLocality] = useState("All"); const [editing, setEditing] = useState<Complaint | null>(null);
  const statuses: (Status | "All")[] = ["All", "Submitted", "Under Review", "In Progress", "Resolved", "Rejected", "Reopened"];
  const filtered = complaints.filter((complaint) => (status === "All" || complaint.status === status) && (category === "All" || complaint.category === category) && (locality === "All" || complaint.locality === locality) && `${complaint.id} ${complaint.title}`.toLowerCase().includes(query.toLowerCase()));
  const clear = () => { setQuery(""); setStatus("All"); setCategory("All"); setLocality("All"); };
  return <section className="container staff-page page-section">
    <div className="staff-intro"><div><p className="eyebrow">Officer workspace</p><h1>Complaint operations</h1><p className="page-intro">Review incoming reports, coordinate departments, and keep residents informed with clear case updates.</p></div><button className="text-button" onClick={() => go("entry")}>Switch workspace <Icon name="arrow" size={17} /></button></div>
    {!loading && !error && <DashboardOverview complaints={complaints} />}
    <div className="management-panel"><div className="panel-heading"><div><h2>Complaint management</h2><p>{loading ? "Loading live complaint records…" : error ? "Complaint records are unavailable." : `${filtered.length} of ${complaints.length} records shown`}</p></div></div><div className="filters"><label className="search-field"><Icon name="search" size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search reference or title" /></label><select value={status} onChange={(event) => setStatus(event.target.value)} aria-label="Filter by status">{statuses.map((value) => <option key={value}>{value}</option>)}</select><select value={category} onChange={(event) => setCategory(event.target.value)} aria-label="Filter by category"><option>All</option>{categories.map(([value]) => <option key={value}>{value}</option>)}</select><select value={locality} onChange={(event) => setLocality(event.target.value)} aria-label="Filter by locality"><option>All</option>{[...new Set(complaints.map((complaint) => complaint.locality))].map((value) => <option key={value}>{value}</option>)}</select><button className="clear-button" onClick={clear}>Clear filters</button></div>{loading ? <div className="empty-state"><p>Loading complaints…</p></div> : error ? <ErrorNotice message={error} retry={retry} /> : <div className="staff-table"><div className="table-head"><span>Reference & complaint</span><span>Category</span><span>Locality</span><span>Submitted</span><span>Department</span><span>Status</span><span>Action</span></div>{filtered.length ? filtered.map((complaint) => <div className="table-row" key={complaint.apiId}><span><strong>{complaint.id}</strong><small>{complaint.title}</small></span><span data-label="Category">{complaint.category}</span><span data-label="Locality">{complaint.locality}</span><span data-label="Submitted">{complaint.date}</span><span data-label="Department">{complaint.department || "Unassigned"}</span><span><StatusBadge status={complaint.status} /></span><button className="table-action" onClick={() => setEditing(complaint)}>Manage</button></div>) : <div className="empty-state"><h2>No matching records</h2><p>Adjust or clear your filters to see complaints.</p></div>}</div>}</div>{editing && <ManagePanel complaint={complaints.find((item) => item.apiId === editing.apiId) || editing} close={() => setEditing(null)} update={(complaint) => { update(complaint); setEditing(complaint); }} go={go} />}</section>;
}

const statusChartColors: Record<Status, string> = { Submitted: "#4f7c91", "Under Review": "#7257ab", "In Progress": "#c48628", Resolved: "#2d815a", Rejected: "#b65552", Reopened: "#26756d" };
function DashboardOverview({ complaints }: { complaints: Complaint[] }) { const stat = (value?: Status) => value ? complaints.filter((complaint) => complaint.status === value).length : complaints.length; return <><div className="staff-stats"><Stat n={stat()} label="Total complaints" icon="report" /><Stat n={stat("Submitted")} label="Pending review" icon="search" /><Stat n={stat("In Progress")} label="In progress" icon="arrow" /><Stat n={stat("Resolved")} label="Resolved" icon="check" /></div><div className="dashboard-charts"><StatusDonut complaints={complaints} /><CategoryBars complaints={complaints} /></div></>; }
function Stat({ n, label, icon }: { n: number; label: string; icon: string }) { return <div className="stat"><span className="stat-icon"><Icon name={icon} size={18} /></span><div><strong>{n}</strong><span>{label}</span></div></div>; }
function ChartEmpty({ message }: { message: string }) { return <div className="chart-empty"><Icon name="report" size={22} /><p>{message}</p></div>; }
function StatusDonut({ complaints }: { complaints: Complaint[] }) { const order: Status[] = ["Submitted", "Under Review", "In Progress", "Resolved", "Rejected", "Reopened"]; const values = order.map((status) => ({ status, count: complaints.filter((complaint) => complaint.status === status).length })); const total = complaints.length; let progress = 0; const segments = values.filter((item) => item.count > 0).map((item) => { const start = progress; progress += (item.count / total) * 100; return `${statusChartColors[item.status]} ${start}% ${progress}%`; }).join(", "); return <article className="chart-card"><div className="chart-heading"><div><p className="eyebrow">Live distribution</p><h2>Complaints by status</h2></div></div>{total ? <div className="donut-layout"><div className="donut" style={{ background: `conic-gradient(${segments})` }}><div><strong>{total}</strong><span>total</span></div></div><ul className="chart-legend">{values.filter((item) => item.count > 0).map((item) => <li key={item.status} title={`${item.status}: ${item.count}`}><i style={{ background: statusChartColors[item.status] }} /><span>{item.status}</span><strong>{item.count}</strong></li>)}</ul></div> : <ChartEmpty message="Status distribution will appear when complaint records are available." />}</article>; }
function CategoryBars({ complaints }: { complaints: Complaint[] }) { const values = categories.map(([category]) => ({ category, count: complaints.filter((complaint) => complaint.category === category).length })); const maximum = Math.max(...values.map((item) => item.count), 0); return <article className="chart-card"><div className="chart-heading"><div><p className="eyebrow">Service demand</p><h2>Complaints by category</h2></div></div>{maximum ? <div className="bar-chart">{values.map((item) => <div className="bar-row" key={item.category} title={`${item.category}: ${item.count}`}><span>{item.category}</span><div className="bar-track"><i style={{ width: `${(item.count / maximum) * 100}%` }} /></div><strong>{item.count}</strong></div>)}</div> : <ChartEmpty message="Category activity will appear when complaint records are available." />}</article>; }
function ManagePanel({ complaint, close, update, go }: { complaint: Complaint; close: () => void; update: (complaint: Complaint) => void; go: (p: string, c?: Complaint) => void }) {
  const [status, setStatus] = useState<ComplaintStatus | "">("");
  const [remarks, setRemarks] = useState("");
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentsLoading, setDepartmentsLoading] = useState(true);
  const [departmentsError, setDepartmentsError] = useState("");
  const [departmentId, setDepartmentId] = useState(complaint.departmentId || "");
  const [departmentSaving, setDepartmentSaving] = useState(false);
  const [departmentSaved, setDepartmentSaved] = useState(false);
  const nextStatuses = allowedNextStatuses(complaint.apiStatus);

  useEffect(() => {
    let active = true;
    const loadDepartments = async () => {
      setDepartmentsLoading(true);
      setDepartmentsError("");
      try {
        const availableDepartments = await listDepartments();
        if (active) setDepartments(availableDepartments);
      } catch (reason) {
        if (active) setDepartmentsError(reason instanceof Error ? reason.message : "Unable to load departments.");
      } finally {
        if (active) setDepartmentsLoading(false);
      }
    };
    void loadDepartments();
    return () => { active = false; };
  }, [complaint.apiId]);

  useEffect(() => {
    setDepartmentId(complaint.departmentId || "");
    setDepartmentSaved(false);
  }, [complaint.departmentId]);

  const saveStatus = async () => {
    if (!status) return;
    setSaving(true);
    setSaved(false);
    setError("");
    try {
      const updated = await updateComplaintStatus(complaint.apiId, status, remarks);
      update(updated);
      setStatus("");
      setRemarks("");
      setSaved(true);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unable to update the complaint.");
    } finally {
      setSaving(false);
    }
  };

  const saveDepartment = async () => {
    if (!departmentId) return;
    setDepartmentSaving(true);
    setDepartmentSaved(false);
    setError("");
    try {
      const updated = await assignComplaintDepartment(complaint.apiId, departmentId);
      update(updated);
      setDepartmentSaved(true);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unable to assign the department.");
    } finally {
      setDepartmentSaving(false);
    }
  };

  const departmentUnavailable = departmentsLoading || Boolean(departmentsError) || departments.length === 0;

  return <aside className="manage-panel" aria-label="Complaint details">
    <div className="manage-header"><div><p className="detail-id">{complaint.id}</p><h2>{complaint.title}</h2></div><button onClick={close} aria-label="Close complaint details"><Icon name="close" /></button></div>
    <p className="description">{complaint.description}</p>
    <div className="detail-grid"><div><span>Location</span><p>{complaint.locality}{complaint.landmark ? ` · ${complaint.landmark}` : ""}</p></div><div><span>Submitted</span><p>{complaint.date}</p></div></div>
    {complaint.attachments.length > 0 && <div className="attachment-gallery"><span>Attached photos</span><div>{complaint.attachments.map((attachment) => <a href={attachment.url} target="_blank" rel="noreferrer" key={attachment.id}><img src={attachment.url} alt={attachment.fileName} /><small>{attachment.fileName}</small></a>)}</div></div>}
    <div className="manage-fields">
      <Field label="Assigned department" hint={departmentsLoading ? "Loading active municipal departments…" : departments.length === 0 && !departmentsError ? "No active departments are available." : undefined} error={departmentsError}>
        <select value={departmentId} onChange={(event) => { setDepartmentId(event.target.value); setDepartmentSaved(false); }} disabled={departmentUnavailable}>
          <option value="">{departmentsLoading ? "Loading departments…" : departmentsError ? "Departments unavailable" : "Select a department"}</option>
          {departments.map((department) => <option value={department.id} key={department.id}>{department.name}</option>)}
        </select>
      </Field>
      <button className="btn btn-secondary submit" onClick={() => void saveDepartment()} disabled={departmentUnavailable || !departmentId || departmentId === complaint.departmentId || departmentSaving}>{departmentSaving ? "Saving department..." : "Save department assignment"}</button>
      {departmentSaved && <p className="saved"><Icon name="check" size={16} /> Department assignment saved and refreshed.</p>}
      <Field label={`Advance status (currently ${complaint.status})`}><select value={status} onChange={(event) => setStatus(event.target.value as ComplaintStatus | "")}><option value="">Select a valid next status</option>{nextStatuses.map((value) => <option value={value} key={value}>{displayStatus(value)}</option>)}</select></Field>
      <Field label="Resident-visible update"><textarea rows={3} value={remarks} onChange={(event) => setRemarks(event.target.value)} placeholder="Add a clear update for the resident..." /></Field>
    </div>
    {error && <p className="error">{error}</p>}
    {saved && <p className="saved"><Icon name="check" size={16} /> Status update saved and visible in tracking.</p>}
    <button className="btn btn-primary submit" onClick={() => void saveStatus()} disabled={!status || saving}>{saving ? "Saving update..." : "Save status update"}</button>
    <div className="history"><h3>Previous status updates</h3>{complaint.history.map((history, index) => <div key={`${history.status}-${index}`}><span /><p><strong>{history.status}</strong> · {history.date}<small>{history.note}</small></p></div>)}</div>
    <button className="text-button" onClick={() => go("track", complaint)}>View resident tracking <Icon name="arrow" size={16} /></button>
  </aside>;
}
function Footer({ go, refresh }: { go: (p: string) => void; refresh: () => Promise<void> }) { return <footer><div className="container footer-grid"><div><Brand /><p>Your Voice. Our Community. Better Together.</p></div><div className="footer-links"><button onClick={() => go("home")}>About CivicConnect</button><button onClick={() => go("report")}>Help</button><button onClick={() => go("track")}>Contact</button><button onClick={() => go("home")}>Privacy</button></div><div className="demo-note"><p>This is a local development demonstration, not an official government portal.</p><button onClick={() => void refresh()}>Refresh API data</button></div></div></footer>; }
