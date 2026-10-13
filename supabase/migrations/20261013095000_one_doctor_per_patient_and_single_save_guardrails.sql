-- Enforce one active doctor per patient and serialize relationship operations.
-- The live Supabase migration also replaces request_connection(),
-- review_doctor_request(), and unlink_doctor_patient() to enforce this rule
-- during requests, approvals, and disconnects.
-- Record entry is guarded against repeated submits in docs/dashboard.html.
-- Intentionally no date/time uniqueness constraint on clinical records.
create unique index if not exists doctor_patients_one_doctor_per_patient_idx
  on public.doctor_patients (patient_id);
create unique index if not exists doctor_requests_one_pending_per_patient_idx
  on public.doctor_requests (patient_id)
  where status = 'pending';
