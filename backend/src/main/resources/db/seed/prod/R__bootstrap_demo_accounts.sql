-- Demo accounts for TFG defense (tutor + empresa)

INSERT INTO empresas (
  nombre,
  tipo_identificador_fiscal,
  identificador_fiscal,
  sector,
  descripcion,
  direccion,
  localidad,
  provincia,
  codigo_postal,
  email_contacto,
  telefono_contacto,
  persona_contacto,
  estado
) VALUES (
  'INS Puig Castellar',
  'CIF',
  'Q5856281C',
  'Educacion',
  'Centro educativo publico de Santa Coloma de Gramenet.',
  'Av. Banus, 41-49',
  'Santa Coloma de Gramenet',
  'Barcelona',
  '08924',
  'insPuig@fctnow.com',
  NULL,
  'INS Puig Castellar',
  'ACTIVA'
)
ON CONFLICT (identificador_fiscal) DO UPDATE SET
  nombre = EXCLUDED.nombre,
  sector = EXCLUDED.sector,
  direccion = EXCLUDED.direccion,
  localidad = EXCLUDED.localidad,
  provincia = EXCLUDED.provincia,
  codigo_postal = EXCLUDED.codigo_postal,
  email_contacto = EXCLUDED.email_contacto,
  persona_contacto = EXCLUDED.persona_contacto,
  estado = EXCLUDED.estado,
  updated_at = NOW();

INSERT INTO app_users (
  email,
  password_hash,
  display_name,
  enabled
) VALUES (
  'gmanrubia@fctnow.com',
  '$2a$10$5T.GmdbRi7uCTOtEUH0rE.dauexxTSCjYGOTdmzjU0wt9iymQ7mvi',
  'Gabriel Manrubia Costa',
  TRUE
)
ON CONFLICT (email) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  display_name = EXCLUDED.display_name,
  enabled = TRUE,
  updated_at = NOW();

INSERT INTO user_roles (user_id, role)
SELECT id, 'TUTOR_CENTRO'
FROM app_users
WHERE email = 'gmanrubia@fctnow.com'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO app_users (
  email,
  password_hash,
  display_name,
  enabled,
  empresa_id
) VALUES (
  'insPuig@fctnow.com',
  '$2a$10$9JmyKPSkrAUzx5q2NsXYyeKzulXU6k4b.lObUFv0aJ5uq5geUxRPK',
  'INS Puig Castellar',
  TRUE,
  (SELECT id FROM empresas WHERE identificador_fiscal = 'Q5856281C')
)
ON CONFLICT (email) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  display_name = EXCLUDED.display_name,
  enabled = TRUE,
  empresa_id = EXCLUDED.empresa_id,
  updated_at = NOW();

INSERT INTO user_roles (user_id, role)
SELECT id, 'EMPRESA'
FROM app_users
WHERE email = 'insPuig@fctnow.com'
ON CONFLICT (user_id, role) DO NOTHING;
