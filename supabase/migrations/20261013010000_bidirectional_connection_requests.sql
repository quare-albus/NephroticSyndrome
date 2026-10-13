-- Bidirectional request lifecycle: either party can initiate, only the recipient can decide.
CREATE OR REPLACE FUNCTION public.request_connection(p_target_code text)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
  v_uid uuid:=auth.uid(); v_role text; v_target_id uuid; v_target_role text;
  v_patient_id uuid; v_doctor_id uuid; v_request_id bigint; v_status text; v_initiated_by uuid;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'Please sign in first.' USING ERRCODE='42501'; END IF;
  SELECT p.user_type INTO v_role FROM public.profiles p WHERE p.id=v_uid;
  IF v_role IS NULL OR v_role NOT IN ('Patient','Doctor') THEN
    RAISE EXCEPTION 'Complete your Patient or Doctor profile first.' USING ERRCODE='42501';
  END IF;
  SELECT p.id,p.user_type INTO v_target_id,v_target_role FROM public.profiles p
  WHERE p.user_code=pg_catalog.upper(pg_catalog.btrim(pg_catalog.coalesce(p_target_code,'')))
    AND p.user_type IN ('Patient','Doctor');
  IF v_target_id IS NULL THEN RAISE EXCEPTION 'No Patient or Doctor was found for that code.' USING ERRCODE='P0001'; END IF;
  IF v_target_id=v_uid THEN RAISE EXCEPTION 'You cannot connect an account to itself.' USING ERRCODE='22023'; END IF;
  IF v_target_role=v_role THEN RAISE EXCEPTION 'Connections must be between one Patient and one Doctor.' USING ERRCODE='22023'; END IF;
  IF v_role='Patient' THEN v_patient_id:=v_uid;v_doctor_id:=v_target_id;
  ELSE v_patient_id:=v_target_id;v_doctor_id:=v_uid; END IF;
  PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext(v_patient_id::text||':'||v_doctor_id::text)::bigint);
  SELECT r.id,r.status,r.initiated_by INTO v_request_id,v_status,v_initiated_by
  FROM public.doctor_requests r WHERE r.patient_id=v_patient_id AND r.doctor_id=v_doctor_id FOR UPDATE;
  IF FOUND THEN
    IF v_status='pending' THEN
      IF v_initiated_by=v_uid THEN RAISE EXCEPTION 'A connection request is already pending.' USING ERRCODE='P0001';
      ELSE RAISE EXCEPTION 'This person has already requested a connection. Review the request in your dashboard.' USING ERRCODE='P0001'; END IF;
    END IF;
    IF v_status='approved' AND EXISTS(SELECT 1 FROM public.doctor_patients dp WHERE dp.doctor_id=v_doctor_id AND dp.patient_id=v_patient_id) THEN
      RAISE EXCEPTION 'You are already connected.' USING ERRCODE='P0001';
    END IF;
    UPDATE public.doctor_requests SET status='pending',initiated_by=v_uid,created_at=pg_catalog.now() WHERE id=v_request_id;
  ELSE
    INSERT INTO public.doctor_requests(patient_id,doctor_id,status,initiated_by)
    VALUES(v_patient_id,v_doctor_id,'pending',v_uid) RETURNING id INTO v_request_id;
  END IF;
  RETURN pg_catalog.jsonb_build_object('request_id',v_request_id,'patient_id',v_patient_id,'doctor_id',v_doctor_id,'status','pending','initiated_by',v_uid);
END; $$;
REVOKE ALL ON FUNCTION public.request_connection(text) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.request_connection(text) TO authenticated;

-- Compatibility alias for the older patient-only frontend during rollout.
CREATE OR REPLACE FUNCTION public.request_doctor_connection(p_code text)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_uid uuid:=auth.uid(); v_role text;
BEGIN
  SELECT p.user_type INTO v_role FROM public.profiles p WHERE p.id=v_uid;
  IF v_role IS DISTINCT FROM 'Patient' THEN RAISE EXCEPTION 'Only patient accounts can request a doctor connection.' USING ERRCODE='42501'; END IF;
  RETURN public.request_connection(p_code);
END; $$;
REVOKE ALL ON FUNCTION public.request_doctor_connection(text) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.request_doctor_connection(text) TO authenticated;

CREATE OR REPLACE FUNCTION public.review_doctor_request(p_request_id bigint,p_decision text)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
  v_uid uuid:=auth.uid(); v_role text; v_doctor_id uuid; v_patient_id uuid; v_status text;
  v_initiated_by uuid; v_patient_role text; v_doctor_role text;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'Please sign in first.' USING ERRCODE='42501'; END IF;
  IF p_decision IS NULL OR p_decision NOT IN ('approved','rejected') THEN RAISE EXCEPTION 'Decision must be approved or rejected.' USING ERRCODE='22023'; END IF;
  SELECT r.doctor_id,r.patient_id,r.status,r.initiated_by INTO v_doctor_id,v_patient_id,v_status,v_initiated_by
  FROM public.doctor_requests r WHERE r.id=p_request_id FOR UPDATE;
  IF NOT FOUND OR v_uid NOT IN (v_doctor_id,v_patient_id) THEN RAISE EXCEPTION 'Connection request not found for this account.' USING ERRCODE='42501'; END IF;
  IF v_status<>'pending' THEN RAISE EXCEPTION 'This request has already been reviewed.' USING ERRCODE='P0001'; END IF;
  IF v_initiated_by=v_uid THEN RAISE EXCEPTION 'You cannot review your own request.' USING ERRCODE='42501'; END IF;
  SELECT p.user_type INTO v_patient_role FROM public.profiles p WHERE p.id=v_patient_id;
  SELECT p.user_type INTO v_doctor_role FROM public.profiles p WHERE p.id=v_doctor_id;
  SELECT p.user_type INTO v_role FROM public.profiles p WHERE p.id=v_uid;
  IF v_patient_role IS DISTINCT FROM 'Patient' OR v_doctor_role IS DISTINCT FROM 'Doctor' THEN
    RAISE EXCEPTION 'The request does not connect a valid Patient and Doctor.' USING ERRCODE='23514';
  END IF;
  IF (v_initiated_by=v_patient_id AND NOT (v_uid=v_doctor_id AND v_role='Doctor'))
     OR (v_initiated_by=v_doctor_id AND NOT (v_uid=v_patient_id AND v_role='Patient')) THEN
    RAISE EXCEPTION 'Only the recipient can review a connection request.' USING ERRCODE='42501';
  END IF;
  IF p_decision='approved' THEN
    INSERT INTO public.doctor_patients(doctor_id,patient_id) VALUES(v_doctor_id,v_patient_id)
    ON CONFLICT(doctor_id,patient_id) DO NOTHING;
  ELSIF EXISTS(SELECT 1 FROM public.doctor_patients dp WHERE dp.doctor_id=v_doctor_id AND dp.patient_id=v_patient_id) THEN
    RAISE EXCEPTION 'This account is already connected. Disconnect the relationship instead.' USING ERRCODE='P0001';
  END IF;
  UPDATE public.doctor_requests SET status=p_decision WHERE id=p_request_id;
  RETURN pg_catalog.jsonb_build_object('request_id',p_request_id,'status',p_decision);
END; $$;
REVOKE ALL ON FUNCTION public.review_doctor_request(bigint,text) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.review_doctor_request(bigint,text) TO authenticated;

CREATE OR REPLACE FUNCTION public.cancel_doctor_request(p_request_id bigint)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_uid uuid:=auth.uid(); v_status text; v_initiated_by uuid;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'Please sign in first.' USING ERRCODE='42501'; END IF;
  SELECT r.status,r.initiated_by INTO v_status,v_initiated_by FROM public.doctor_requests r
  WHERE r.id=p_request_id AND v_uid IN (r.patient_id,r.doctor_id) FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Connection request not found for this account.' USING ERRCODE='42501'; END IF;
  IF v_status<>'pending' THEN RAISE EXCEPTION 'Only pending requests can be cancelled.' USING ERRCODE='P0001'; END IF;
  IF v_initiated_by IS DISTINCT FROM v_uid THEN RAISE EXCEPTION 'Only the person who sent the request can cancel it.' USING ERRCODE='42501'; END IF;
  UPDATE public.doctor_requests SET status='cancelled' WHERE id=p_request_id;
  RETURN pg_catalog.jsonb_build_object('request_id',p_request_id,'status','cancelled');
END; $$;
REVOKE ALL ON FUNCTION public.cancel_doctor_request(bigint) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.cancel_doctor_request(bigint) TO authenticated;
