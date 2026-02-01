-- ==========================================
-- VALIDACIONES DE BASE DE DATOS (POSTGRESQL)
-- ==========================================

-- 1. Restricciones CHECK para fechas y valores numéricos
ALTER TABLE oferta_academica
    ADD CONSTRAINT chk_oferta_fechas CHECK (fecha_fin >= fecha_inicio),
    ADD CONSTRAINT chk_oferta_cupos CHECK (cupos >= 1),
    ADD CONSTRAINT chk_oferta_precio CHECK (costo_inscripcion >= 0);

ALTER TABLE curso
    ADD CONSTRAINT chk_curso_cuotas CHECK (nr_cuotas >= 1),
    ADD CONSTRAINT chk_curso_mora CHECK (costo_mora >= 0);

-- 2. Restricción para evitar solapamiento de horarios (requiere extensión btree_gist)
-- CREATE EXTENSION IF NOT EXISTS btree_gist;
-- ALTER TABLE horario
--    ADD CONSTRAINT exclude_horario_overlap
--    EXCLUDE USING gist (
--        oferta_id WITH =,
--        docente_id WITH =, 
--        tstzrange(fecha_inicio || ' ' || hora_inicio, fecha_inicio || ' ' || hora_fin) WITH &&
--    );

-- 3. Trigger diferido para asegurar al menos un docente en CURSO
CREATE OR REPLACE FUNCTION check_curso_docente() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM curso_docente WHERE curso_id = NEW.id_oferta) THEN
        RAISE EXCEPTION 'El curso % debe tener al menos un docente asignado.', NEW.id_oferta;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_curso_docente ON curso;

CREATE CONSTRAINT TRIGGER trg_check_curso_docente
    AFTER INSERT OR UPDATE ON curso
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_curso_docente();

-- 4. Trigger diferido para asegurar al menos un disertante en CHARLA
CREATE OR REPLACE FUNCTION check_charla_disertante() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM charla_disertantes WHERE charla_id_oferta = NEW.id_oferta) THEN
        RAISE EXCEPTION 'La charla % debe tener al menos un disertante asignado.', NEW.id_oferta;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_charla_disertante ON charla;

CREATE CONSTRAINT TRIGGER trg_check_charla_disertante
    AFTER INSERT OR UPDATE ON charla
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_charla_disertante();
