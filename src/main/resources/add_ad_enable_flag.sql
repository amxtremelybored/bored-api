INSERT INTO public.app_config (config_key, config_value) 
VALUES ('AD_ENABLE', 'true') 
ON CONFLICT (config_key) DO UPDATE 
SET config_value = 'true';
