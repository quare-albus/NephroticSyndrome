-- NephroTrack: unique doctor codes and transactional, bidirectional connection lifecycle.
-- Applied to Supabase project nsjnkhcmlnshyjblzolh on 2026-10-13.

CREATE OR REPLACE FUNCTION public.generate_doctor_code(p_name text, p_exclude_id uuid DEFAULT NULL)
RETURNS text LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
  v_letters text; v_prefix text; v_start integer; v_suffix integer; v_offset integer; v_code text;
BEGIN
  v_letters := pg_catalog.upper(pg_catalog.regexp_replace(pg_catalog.coalesce(p_name, ''), '[^A-Za-z]', '', 'g'));
  v_prefix := pg_catalog.left(v_letters || 'XXX', 3);
  PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_prefix)::bigint);
  v_start := pg_catalog.floor(pg_catalog.random() * 100)::integer;
  FOR v_offset IN 0..99 LOOP
    v_suffix := (v_start + v_offset) % 100;
    v_code := v_prefix || pg_catalog.lpad(v_suffix::text, 2, '0');
    IF NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.user_code=v_code AND (p_exclude_id IS NULL OR p.id<>p_exclude_id)) THEN RETURN v_code; END IF;
  END LOOP;
  RAISE EXCEPTION 'All 100 doctor codes for prefix % are in use. Choose a username with different first three letters.',v_prefix USING ERRCODE='23505';
END; $$;
REVOKE ALL ON FUNCTION public.generate_doctor_code(text,uuid) FROM PUBLIC,anon,authenticated;

CREATE OR REPLACE FUNCTION public.enforce_profile_identity_and_doctor_code()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
  IF NEW.user_type NOT IN ('Patient','Doctor','Unknown') THEN RAISE EXCEPTION 'Invalid account type.' USING ERRCODE='23514'; END IF;
  IF TG_OP='UPDATE' THEN
    IF NEW.user_type IS DISTINCT FROM OLD.user_type AND NOT (OLD.user_type='Unknown' AND NEW.user_type IN ('Patient','Doctor')) THEN
      RAISE EXCEPTION 'Account type cannot be changed after setup.' USING ERRCODE='42501';
    END IF;
    IF NEW.user_type='Doctor' THEN
      IF OLD.user_type IS DISTINCT FROM 'Doctor' OR NEW.name IS DISTINCT FROM OLD.name OR NEW.user_code IS DISTINCT FROM OLD.user_code THEN
        NEW.user_code:=public.generate_doctor_code(NEW.name,OLD.id);
      ELSE NEW.user_code:=OLD.user_code; END IF;
    ELSE NEW.user_code:=OLD.user_code; END IF;
  ELSIF NEW.user_type='Doctor' THEN
    NEW.user_code:=public.generate_doctor_code(NEW.name,NEW.id);
  END IF;
  RETURN NEW;
END; $$;
REVOKE ALL ON FUNCTION public.enforce_profile_identity_and_doctor_code() FROM PUBLIC,anon,authenticated;
DROP TRIGGER IF EXISTS profiles_doctor_code_guard ON public.profiles;
CREATE TRIGGER profiles_doctor_code_guard BEFORE INSERT OR UPDATE OF name,user_type,user_code ON public.profiles
FOR EACH ROW EXECUTE FUNCTION public.enforce_profile_identity_and_doctor_code();

-- Existing doctors are backfilled through the same trigger/generator.
UPDATE public.profiles SET user_code='MIGRATING_'||pg_catalog.replace(id::text,'-','') WHERE user_type='Doctor';

ALTER TABLE public.doctor_requests DROP CONSTRAINT IF EXISTS doctor_requests_status_check;
ALTER TABLE public.doctor_requests ADD CONSTRAINT doctor_requests_status_check CHECK(status IN ('pending','approved','rejected','revoked','cancelled'));
CREATE INDEX IF NOT EXISTS doctor_requests_doctor_status_created_idx ON public.doctor_requests(doctor_id,status,created_at);
CREATE INDEX IF NOT EXISTS doctor_requests_patient_status_created_idx ON public.doctor_requests(patient_id,status,created_at);

CREATE OR REPLACE FUNCTION public.request_doctor_connection(p_code text)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_uid uuid:=auth.uid(); v_role text; v_doctor_id uuid; v_request_id bigint; v_status text;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'Please sign in first.' USING ERRCODE='42501'; END IF;
  SELECT p.user_type INTO v_role FROM public.profiles p WHERE p.id=v_uid;
  IF v_role IS DISTINCT FROM 'Patient' THEN RAISE EXCEPTION 'Only patient accounts can request a doctor connection.' USING ERRCODE='42501'; END IF;
  SELECT p.id INTO v_doctor_id FROM public.profiles p WHERE p.user_code=pg_catalog.upper(pg_catalog.btrim(pg_catalog.coalesce(p_code,''))) AND p.user_type='Doctor';
  IF v_doctor_id IS NULL THEN RAISE EXCEPTION 'Doctor code not found. Check the five-character code.' USING ERRCODE='P0001'; END IF;
  IF v_doctor_id=v_uid THEN RAISE EXCEPTION 'You cannot connect an account to itself.' USING ERRCODE='22023'; END IF;
  PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_uid::text||':'||v_doctor_id::text)::bigint);
  SELECT r.id,r.status INTO v_request_id,v_status FROM public.doctor_requests r WHERE r.patient_id=v_uid AND r.doctor_id=v_doctor_id FOR UPDATE;
  IF FOUND THEN
    IF v_status='pending' THEN RAISE EXCEPTION 'A connection request is already pending.' USING ERRCODE='P0001'; END IF;
    IF v_status='approved' AND EXISTS(SELECT 1 FROM public.doctor_patients dp WHERE dp.doctor_id=v_doctor_id AND dp.patient_id=v_uid) THEN
      RAISE EXCEPTION 'You are already connected to this doctor.' USING ERRCODE='P0001';
    END IF;
    UPDATE public.doctor_requests SET status='pending',created_at=pg_catalog.now() WHERE id=v_request_id;
  ELSE
    INSERT INTO public.doctor_requests(patient_id,doctor_id,status) VALUES(v_uid,v_doctor_id,'pending') RETURNING id INTO v_request_id;
  END IF;
  RETURN pg_catalog.jsonb_build_object('request_id',v_request_id,'status','pending');
END; $$;

CREATE OR REPLACE FUNCTION public.review_doctor_request(p_request_id bigint,p_decision text)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_uid uuid:=auth.uid(); v_role text; v_doctor_id uuid; v_patient_id uuid; v_status text; v_patient_role text;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'Please sign in first.' USING ERRCODE='42501'; END IF;
  IF p_decision NOT IN ('approved','rejected') THEN RAISE EXCEPTION 'Decision must be approved or rejected.' USING ERRCODE='22023'; END IF;
  SELECT p.user_type INTO v_role FROM public.profiles p WHERE p.id=v_uid;
  IF v_role IS DISTINCT FROM 'Doctor' THEN RAISE EXCEPTION 'Only doctor accounts can review requests.' USING ERRCODE='42501'; END IF;
  SELECT r.doctor_id,r.patient_id,r.status INTO v_doctor_id,v_patient_id,v_status FROM public.doctor_requests r WHERE r.id=p_request_id FOR UPDATE;
  IF NOT FOUND OR v_doctor_id IS DISTINCT FROM v_uid THEN RAISE EXCEPTION 'Connection request not found for this doctor.' USING ERRCODE='42501'; END IF;
  IF v_status<>'pending' THEN RAISE EXCEPTION 'This request has already been reviewed.' USING ERRCODE='P0001'; END IF;
  IF p_decision='approved' THEN
    SELECT p.user_type INTO v_patient_role FROM public.profiles p WHERE p.id=v_patient_id;
    IF v_patient_role IS DISTINCT FROM 'Patient' THEN RAISE EXCEPTION 'The requesting account is not a patient.' USING ERRCODE='23514'; END IF;
    INSERT INTO public.doctor_patients(doctor_id,patient_id) VALUES(v_uid,v_patient_id) ON CONFLICT(doctor_id,patient_id) DO NOTHING;
  ELSIF EXISTS(SELECT 1 FROM public.doctor_patients dp WHERE dp.doctor_id=v_uid AND dp.patient_id=v_patient_id) THEN
    RAISE EXCEPTION 'This patient is already connected. Disconnect the relationship instead.' USING ERRCODE='P0001';
  END IF;
  UPDATE public.doctor_requests SET status=p_decision WHERE id=p_request_id;
  RETURN pg_catalog.jsonb_build_object('request_id',p_request_id,'status',p_decision);
END; $$;

CREATE OR REPLACE FUNCTION public.cancel_doctor_request(p_request_id bigint)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_uid uuid:=auth.uid(); v_status text;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'Please sign in first.' USING ERRCODE='42501'; END IF;
  SELECT r.status INTO v_status FROM public.doctor_requests r WHERE r.id=p_request_id AND r.patient_id=v_uid FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Connection request not found for this patient.' USING ERRCODE='42501'; END IF;
  IF v_status<>'pending' THEN RAISE EXCEPTION 'Only pending requests can be cancelled.' USING ERRCODE='P0001'; END IF;
  UPDATE public.doctor_requests SET status='cancelled' WHERE id=p_request_id;
  RETURN pg_catalog.jsonb_build_object('request_id',p_request_id,'status','cancelled');
END; $$;

CREATE OR REPLACE FUNCTION public.unlink_doctor_patient(p_other_user_id uuid)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_uid uuid:=auth.uid(); v_role text; v_doctor_id uuid; v_patient_id uuid;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'Please sign in first.' USING ERRCODE='42501'; END IF;
  SELECT p.user_type INTO v_role FROM public.profiles p WHERE p.id=v_uid;
  IF v_role='Doctor' THEN v_doctor_id:=v_uid;v_patient_id:=p_other_user_id;
  ELSIF v_role='Patient' THEN v_doctor_id:=p_other_user_id;v_patient_id:=v_uid;
  ELSE RAISE EXCEPTION 'Only patient or doctor accounts can manage connections.' USING ERRCODE='42501'; END IF;
  DELETE FROM public.doctor_patients dp WHERE dp.doctor_id=v_doctor_id AND dp.patient_id=v_patient_id;
  IF NOT FOUND THEN RAISE EXCEPTION 'Active connection not found.' USING ERRCODE='P0001'; END IF;
  UPDATE public.doctor_requests r SET status='revoked' WHERE r.doctor_id=v_doctor_id AND r.patient_id=v_patient_id AND r.status='approved';
  RETURN pg_catalog.jsonb_build_object('doctor_id',v_doctor_id,'patient_id',v_patient_id,'status','revoked');
END; $$;

REVOKE ALL ON FUNCTION public.request_doctor_connection(text) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.review_doctor_request(bigint,text) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.cancel_doctor_request(bigint) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.unlink_doctor_patient(uuid) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.request_doctor_connection(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.review_doctor_request(bigint,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.cancel_doctor_request(bigint) TO authenticated;
GRANT EXECUTE ON FUNCTION public.unlink_doctor_patient(uuid) TO authenticated;

DROP POLICY IF EXISTS "doctors create relationships" ON public.doctor_patients;
DROP POLICY IF EXISTS "doctors create relationships for approved patients" ON public.doctor_patients;
DROP POLICY IF EXISTS "doctors delete relationships" ON public.doctor_patients;
DROP POLICY IF EXISTS "doctors update relationships" ON public.doctor_patients;
DROP POLICY IF EXISTS "doctors update own requests" ON public.doctor_requests;
DROP POLICY IF EXISTS "patients create requests" ON public.doctor_requests;
DROP POLICY IF EXISTS "doctors read own requests" ON public.doctor_requests;
DROP POLICY IF EXISTS "patients read own requests" ON public.doctor_requests;
CREATE POLICY "participants read connection requests" ON public.doctor_requests
FOR SELECT TO authenticated
USING ((SELECT auth.uid()) = patient_id OR (SELECT auth.uid()) = doctor_id);
REVOKE ALL ON public.doctor_patients,public.doctor_requests FROM anon,authenticated;
GRANT SELECT ON public.doctor_patients,public.doctor_requests TO authenticated;

-- Disable stale SECURITY DEFINER endpoints so no alternate connection path bypasses this lifecycle.
REVOKE ALL ON FUNCTION public.request_connection(text) FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.respond_to_connection(bigint,text) FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.disconnect_connection(uuid) FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.handle_new_user() FROM PUBLIC,anon,authenticated;
