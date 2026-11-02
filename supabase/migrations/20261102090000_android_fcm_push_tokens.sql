-- Android FCM device tokens, scoped to the signed-in patient account.
CREATE TABLE IF NOT EXISTS public.push_tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    token text NOT NULL UNIQUE,
    platform text NOT NULL DEFAULT 'android' CHECK (platform = 'android'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.push_tokens ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.push_tokens FROM anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.push_tokens TO authenticated;
GRANT USAGE, SELECT ON SEQUENCE public.push_tokens_id_seq TO authenticated;
CREATE POLICY "patients manage own Android push tokens"
ON public.push_tokens FOR ALL TO authenticated
USING ((SELECT auth.uid()) = patient_id)
WITH CHECK ((SELECT auth.uid()) = patient_id);
CREATE INDEX IF NOT EXISTS push_tokens_patient_id_idx ON public.push_tokens(patient_id);
