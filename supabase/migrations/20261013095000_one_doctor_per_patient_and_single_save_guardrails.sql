begin;

-- Enforce at most one active doctor for any patient, and at most one pending
-- request per patient. These indexes are the final concurrency-safe guardrails.
create unique index if not exists doctor_patients_one_doctor_per_patient_idx
  on public.doctor_patients (patient_id);
create unique index if not exists doctor_requests_one_pending_per_patient_idx
  on public.doctor_requests (patient_id)
  where status = 'pending';

create or replace function public.request_connection(p_target_code text)
returns jsonb language plpgsql security definer set search_path to ''
as $function$
declare
  v_uid uuid := auth.uid();
  v_role text;
  v_target_id uuid;
  v_target_role text;
  v_patient_id uuid;
  v_doctor_id uuid;
  v_request_id bigint;
  v_status text;
  v_initiated_by uuid;
begin
  if v_uid is null then raise exception 'Please sign in first.' using errcode = '42501'; end if;
  select p.user_type into v_role from public.profiles p where p.id = v_uid;
  if v_role is null or v_role not in ('Patient', 'Doctor') then
    raise exception 'Complete your Patient or Doctor profile first.' using errcode = '42501';
  end if;

  select p.id, p.user_type into v_target_id, v_target_role
  from public.profiles p
  where p.user_code = pg_catalog.upper(pg_catalog.btrim(coalesce(p_target_code, '')))
    and p.user_type in ('Patient', 'Doctor');
  if v_target_id is null then raise exception 'No Patient or Doctor was found for that code.' using errcode = 'P0001'; end if;
  if v_target_id = v_uid then raise exception 'You cannot connect an account to itself.' using errcode = '22023'; end if;
  if v_target_role = v_role then raise exception 'Connections must be between one Patient and one Doctor.' using errcode = '22023'; end if;

  if v_role = 'Patient' then v_patient_id := v_uid; v_doctor_id := v_target_id;
  else v_patient_id := v_target_id; v_doctor_id := v_uid; end if;

  -- Serialize requests for a patient across all potential doctors.
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_patient_id::text)::bigint);

  if exists (select 1 from public.doctor_patients dp where dp.patient_id = v_patient_id) then
    if exists (select 1 from public.doctor_patients dp where dp.patient_id = v_patient_id and dp.doctor_id = v_doctor_id) then
      raise exception 'You are already connected to this doctor.' using errcode = 'P0001';
    end if;
    raise exception 'This patient is already connected to another doctor. Disconnect first before requesting a different doctor.' using errcode = 'P0001';
  end if;

  if exists (select 1 from public.doctor_requests r
    where r.patient_id = v_patient_id and r.status = 'pending' and r.doctor_id <> v_doctor_id) then
    raise exception 'This patient already has a pending request to another doctor. Cancel or resolve it before sending another request.' using errcode = 'P0001';
  end if;

  select r.id, r.status, r.initiated_by into v_request_id, v_status, v_initiated_by
  from public.doctor_requests r
  where r.patient_id = v_patient_id and r.doctor_id = v_doctor_id
  for update;

  if found then
    if v_status = 'pending' then
      if v_initiated_by = v_uid then raise exception 'A connection request is already pending.' using errcode = 'P0001';
      else raise exception 'This person has already requested a connection. Review the request in your dashboard.' using errcode = 'P0001'; end if;
    end if;
    update public.doctor_requests set status = 'pending', initiated_by = v_uid, created_at = pg_catalog.now()
    where id = v_request_id;
  else
    insert into public.doctor_requests(patient_id, doctor_id, status, initiated_by)
    values (v_patient_id, v_doctor_id, 'pending', v_uid) returning id into v_request_id;
  end if;

  return pg_catalog.jsonb_build_object('request_id', v_request_id, 'patient_id', v_patient_id,
    'doctor_id', v_doctor_id, 'status', 'pending', 'initiated_by', v_uid);
end;
$function$;

create or replace function public.review_doctor_request(p_request_id bigint, p_decision text)
returns jsonb language plpgsql security definer set search_path to ''
as $function$
declare
  v_uid uuid := auth.uid();
  v_doctor_id uuid;
  v_patient_id uuid;
  v_status text;
  v_initiated_by uuid;
  v_patient_role text;
  v_doctor_role text;
  v_role text;
begin
  if v_uid is null then raise exception 'Please sign in first.' using errcode = '42501'; end if;
  if p_decision is null or p_decision not in ('approved','rejected') then
    raise exception 'Decision must be approved or rejected.' using errcode = '22023';
  end if;

  -- Lock order is patient first, request row second, matching request creation.
  select r.doctor_id, r.patient_id into v_doctor_id, v_patient_id
  from public.doctor_requests r where r.id = p_request_id;
  if not found then raise exception 'Connection request not found for this account.' using errcode = '42501'; end if;
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_patient_id::text)::bigint);

  select r.status, r.initiated_by into v_status, v_initiated_by
  from public.doctor_requests r where r.id = p_request_id for update;
  if v_uid not in (v_doctor_id, v_patient_id) then raise exception 'Connection request not found for this account.' using errcode = '42501'; end if;
  if v_status <> 'pending' then raise exception 'This request has already been reviewed.' using errcode = 'P0001'; end if;
  if v_initiated_by = v_uid then raise exception 'You cannot review your own request.' using errcode = '42501'; end if;

  select p.user_type into v_patient_role from public.profiles p where p.id = v_patient_id;
  select p.user_type into v_doctor_role from public.profiles p where p.id = v_doctor_id;
  select p.user_type into v_role from public.profiles p where p.id = v_uid;
  if v_patient_role is distinct from 'Patient' or v_doctor_role is distinct from 'Doctor' then
    raise exception 'The request does not connect a valid Patient and Doctor.' using errcode = '23514';
  end if;
  if (v_initiated_by = v_patient_id and not (v_uid = v_doctor_id and v_role = 'Doctor'))
     or (v_initiated_by = v_doctor_id and not (v_uid = v_patient_id and v_role = 'Patient')) then
    raise exception 'Only the recipient can review a connection request.' using errcode = '42501';
  end if;

  if p_decision = 'approved' then
    if exists (select 1 from public.doctor_patients dp
      where dp.patient_id = v_patient_id and dp.doctor_id <> v_doctor_id) then
      raise exception 'This patient is already connected to another doctor. Disconnect that relationship before approving this request.' using errcode = 'P0001';
    end if;
    insert into public.doctor_patients(doctor_id, patient_id) values (v_doctor_id, v_patient_id)
    on conflict (doctor_id, patient_id) do nothing;
  elsif exists (select 1 from public.doctor_patients dp
    where dp.doctor_id = v_doctor_id and dp.patient_id = v_patient_id) then
    raise exception 'This account is already connected. Disconnect the relationship instead.' using errcode = 'P0001';
  end if;

  update public.doctor_requests set status = p_decision where id = p_request_id;
  return pg_catalog.jsonb_build_object('request_id', p_request_id, 'status', p_decision);
end;
$function$;

create or replace function public.unlink_doctor_patient(p_other_user_id uuid)
returns jsonb language plpgsql security definer set search_path to ''
as $function$
declare
  v_uid uuid := auth.uid();
  v_role text;
  v_doctor_id uuid;
  v_patient_id uuid;
begin
  if v_uid is null then raise exception 'Please sign in first.' using errcode = '42501'; end if;
  select p.user_type into v_role from public.profiles p where p.id = v_uid;
  if v_role = 'Doctor' then v_doctor_id := v_uid; v_patient_id := p_other_user_id;
  elsif v_role = 'Patient' then v_doctor_id := p_other_user_id; v_patient_id := v_uid;
  else raise exception 'Only patient or doctor accounts can manage connections.' using errcode = '42501'; end if;

  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_patient_id::text)::bigint);
  delete from public.doctor_patients dp where dp.doctor_id = v_doctor_id and dp.patient_id = v_patient_id;
  if not found then raise exception 'Active connection not found.' using errcode = 'P0001'; end if;

  update public.doctor_requests r set status = 'revoked'
  where r.doctor_id = v_doctor_id and r.patient_id = v_patient_id and r.status = 'approved';
  return pg_catalog.jsonb_build_object('doctor_id', v_doctor_id, 'patient_id', v_patient_id, 'status', 'revoked');
end;
$function$;

commit;
