export type ComplaintStatus = "SUBMITTED" | "UNDER_REVIEW" | "IN_PROGRESS" | "RESOLVED" | "REJECTED" | "REOPENED";
export type DisplayStatus = "Submitted" | "Under Review" | "In Progress" | "Resolved" | "Rejected" | "Reopened";

type ApiHistory = {
  id: string;
  fromStatus: ComplaintStatus | null;
  toStatus: ComplaintStatus;
  note?: string | null;
  changedAt: string;
};

type ApiComplaint = {
  id: string;
  referenceNumber: string;
  trackingCode: string;
  title: string;
  category: ComplaintCategory;
  description: string;
  locality: string;
  landmark?: string | null;
  status: ComplaintStatus;
  assignedDepartment?: { id: string; name: string; code: string } | null;
  remarks?: string | null;
  statusHistory?: ApiHistory[] | null;
  attachments?: ApiAttachment[] | null;
  createdAt: string;
  updatedAt: string;
};

type ApiAttachment = {
  id: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  url: string;
};

type PageResponse = { content: ApiComplaint[] };

export type Department = {
  id: string;
  name: string;
  code: string;
  description: string;
};

export type ComplaintCategory =
  | "GARBAGE_COLLECTION"
  | "ROADS_AND_POTHOLES"
  | "DRAINAGE_PROBLEMS"
  | "STREETLIGHT_ISSUES"
  | "WATER_SUPPLY";

export type Complaint = {
  id: string;
  apiId: string;
  title: string;
  category: string;
  description: string;
  locality: string;
  landmark?: string;
  date: string;
  status: DisplayStatus;
  apiStatus: ComplaintStatus;
  department?: string;
  departmentId?: string;
  remarks?: string;
  history: { status: DisplayStatus; date: string; note?: string }[];
  attachments: Attachment[];
};

export type Attachment = {
  id: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  url: string;
};

export type CreateComplaintInput = {
  title: string;
  category: string;
  description: string;
  locality: string;
  landmark?: string;
};

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api/v1";

const categoryByLabel: Record<string, ComplaintCategory> = {
  "Garbage Collection": "GARBAGE_COLLECTION",
  "Roads & Potholes": "ROADS_AND_POTHOLES",
  "Drainage Problems": "DRAINAGE_PROBLEMS",
  "Streetlight Issues": "STREETLIGHT_ISSUES",
  "Water Supply": "WATER_SUPPLY",
};

const categoryLabel: Record<ComplaintCategory, string> = {
  GARBAGE_COLLECTION: "Garbage Collection",
  ROADS_AND_POTHOLES: "Roads & Potholes",
  DRAINAGE_PROBLEMS: "Drainage Problems",
  STREETLIGHT_ISSUES: "Streetlight Issues",
  WATER_SUPPLY: "Water Supply",
};

const statusLabel: Record<ComplaintStatus, DisplayStatus> = {
  SUBMITTED: "Submitted",
  UNDER_REVIEW: "Under Review",
  IN_PROGRESS: "In Progress",
  RESOLVED: "Resolved",
  REJECTED: "Rejected",
  REOPENED: "Reopened",
};

const validTransitions: Record<ComplaintStatus, ComplaintStatus[]> = {
  SUBMITTED: ["UNDER_REVIEW", "REJECTED"],
  UNDER_REVIEW: ["IN_PROGRESS", "REJECTED"],
  IN_PROGRESS: ["RESOLVED", "REJECTED"],
  RESOLVED: ["REOPENED"],
  REJECTED: ["REOPENED"],
  REOPENED: ["UNDER_REVIEW"],
};

export class ApiError extends Error {}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const isFormData = options?.body instanceof FormData;
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { ...(isFormData ? {} : { "Content-Type": "application/json" }), ...options?.headers },
  });
  if (!response.ok) {
    const body = await response.text();
    let message = body;
    try {
      const parsed = JSON.parse(body) as { message?: string; detail?: string };
      message = parsed.message || parsed.detail || body;
    } catch {
      // Non-JSON responses (such as a proxy error) are still useful to surface.
    }
    throw new ApiError(message || `Request failed with status ${response.status}.`);
  }
  return response.json() as Promise<T>;
}

function formatDate(value?: string): string {
  if (!value) return "Pending";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Pending" : date.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function mapComplaint(complaint: ApiComplaint): Complaint {
  const history = (complaint.statusHistory || []).map((item) => ({
    status: statusLabel[item.toStatus],
    date: formatDate(item.changedAt),
    note: item.note || undefined,
  }));
  return {
    id: complaint.referenceNumber,
    apiId: complaint.id,
    title: complaint.title,
    category: categoryLabel[complaint.category],
    description: complaint.description,
    locality: complaint.locality,
    landmark: complaint.landmark || undefined,
    date: formatDate(complaint.createdAt),
    status: statusLabel[complaint.status],
    apiStatus: complaint.status,
    department: complaint.assignedDepartment?.name,
    departmentId: complaint.assignedDepartment?.id,
    remarks: complaint.remarks || undefined,
    history: history.length ? history : [{ status: statusLabel[complaint.status], date: formatDate(complaint.updatedAt) }],
    attachments: (complaint.attachments || []).map((attachment) => ({
      id: attachment.id,
      fileName: attachment.fileName,
      mimeType: attachment.mimeType,
      sizeBytes: attachment.sizeBytes,
      uploadedAt: attachment.uploadedAt,
      url: attachment.url,
    })),
  };
}

export function allowedNextStatuses(status: ComplaintStatus): ComplaintStatus[] {
  return validTransitions[status];
}

export function displayStatus(status: ComplaintStatus): DisplayStatus {
  return statusLabel[status];
}

export async function listComplaints(): Promise<Complaint[]> {
  const page = await request<PageResponse>("/complaints?size=100&sort=createdAt,desc");
  return page.content.map(mapComplaint);
}

export async function listDepartments(): Promise<Department[]> {
  return request<Department[]>("/departments");
}

export async function createComplaint(input: CreateComplaintInput): Promise<Complaint> {
  const category = categoryByLabel[input.category];
  if (!category) throw new ApiError("Choose a supported complaint category.");
  const complaint = await request<ApiComplaint>("/complaints", {
    method: "POST",
    body: JSON.stringify({ ...input, category, landmark: input.landmark || null }),
  });
  return mapComplaint(complaint);
}

export async function uploadComplaintAttachment(complaintId: string, file: File): Promise<Attachment> {
  const formData = new FormData();
  formData.append("file", file);
  const attachment = await request<ApiAttachment>(`/complaints/${encodeURIComponent(complaintId)}/attachments`, {
    method: "POST",
    body: formData,
  });
  return attachment;
}

export async function trackComplaint(referenceNumber: string): Promise<Complaint> {
  const complaint = await request<ApiComplaint>(`/complaints/ref/${encodeURIComponent(referenceNumber)}`);
  return mapComplaint(complaint);
}

export async function updateComplaintStatus(id: string, status: ComplaintStatus, note?: string): Promise<Complaint> {
  const complaint = await request<ApiComplaint>(`/complaints/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status, note: note || null }),
  });
  return mapComplaint(complaint);
}

export async function assignComplaintDepartment(id: string, departmentId: string): Promise<Complaint> {
  const complaint = await request<ApiComplaint>(`/complaints/${id}/department?departmentId=${encodeURIComponent(departmentId)}`, {
    method: "PATCH",
  });
  return mapComplaint(complaint);
}
