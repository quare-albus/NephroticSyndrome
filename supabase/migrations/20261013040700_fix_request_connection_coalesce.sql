create or replace function public.request_connection(p_target_code text)
returns jsonb language plpgsql security definer set search_path = ''
as $function$
declare
  v_uid uuid:=auth.uid(); v_role text; v_target_id uuid; v_target_role text;
  v_patient_id uuid; v_doctor_id uuid; v_request_id bigint; v_status text; v_initiated_by uuid;
begin
  if v_uid is null then raise exception 'Please sign in first.' using errcode='42501'; end if;
  select p.user_type into v_role from public.profiles p where p.id=v_uid;
  if v_role is null or v_role not in ('Patient','Doctor') then
    raise exception 'Complete your Patient or Doctor profile first.' using errcode='42501';
  end if;
  select p.id,p.user_type into v_target_id,v_target_role
  from public.profiles p
  where p.user_code=pg_catalog.upper(pg_catalog.btrim(coalesce(p_target_code,'')))
    and p.user_type in ('Patient','Doctor');
  if v_target_id is null then raise exception 'No Patient or Doctor was found for that code.' using errcode='P0001'; end if;
  if v_target_id=v_uid then raise exception 'You cannot connect an account to itself.' using errcode='22023'; end if;
  if v_target_role=v_role then raise exception 'Connections must be between one Patient and one Doctor.' using errcode='22023'; end if;
  if v_role='Patient' then v_patient_id:=v_uid;v_doctor_id:=v_target_id;
  else v_patient_id:=v_target_id;v_doctor_id:=v_uid; end if;
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_patient_id::text||':'||v_doctor_id::text)::bigint);
  select r.id,r.status,r.initiated_by into v_request_id,v_status,v_initiated_by
  from public.doctor_requests r where r.patient_id=v_patient_id and r.doctor_id=v_doctor_id for update;
  if found then
    if v_status='pending' then
      if v_initiated_by=v_uid then raise exception 'A connection request is already pending.' using errcode='P0001';
      else raise exception 'This person has already requested a connection. Review the request in your dashboard.' using errcode='P0001'; end if;
    end if;
    if v_status='approved' and exists(select 1 from public.doctor_patients dp where dp.doctor_id=v_doctor_id and dp.patient_id=v_patient_id) then
      raise exception 'You are already connected.' using errcode='P0001';
    end if;
    update public.doctor_requests set status='pending',initiated_by=v_uid,created_at=pg_catalog.now() where id=v_request_id;
  else
    insert into public.doctor_requests(patient_id,doctor_id,status,initiated_by) values(v_patient_id,v_doctor_id,'pending',v_uid) returning id into v_request_id;
  end if;
  return pg_catalog.jsonb_build_object('request_id',v_request_id,'patient_id',v_patient_id,'doctor_id',v_doctor_id,'status','pending','initiated_by',v_uid);
end;
$function$;
revoke all on function public.request_connection(text) from public, anon;
grant execute on function public.request_connection(text) to authenticated;
