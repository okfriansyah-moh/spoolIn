\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path = appdata, pg_catalog;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

CREATE TEMP TABLE saved_credit_source (
    title_id uuid NOT NULL,
    expected_title text NOT NULL,
    person_name text NOT NULL,
    role_id uuid NOT NULL,
    character_names text[] NOT NULL,
    source_url text NOT NULL,
    PRIMARY KEY (title_id, person_name, role_id)
) ON COMMIT DROP;

INSERT INTO saved_credit_source VALUES
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Austin Abrams', 'b0000001-0000-4000-8000-000000000001', ARRAY['Jay Gardiner']::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Josh Brolin', 'b0000001-0000-4000-8000-000000000001', ARRAY['Mitt Gardiner']::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Elisabeth Shue', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'John Ortiz', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Jane Levy', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Emily Rudd', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Brian Duffield', 'b0000001-0000-4000-8000-000000000002', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Brian Duffield', 'b0000001-0000-4000-8000-000000000003', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Daniel Kraus', 'b0000001-0000-4000-8000-000000000003', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Brian Grazer', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Brian Duffield', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Jeb Brody', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('474c56db-778a-4c0d-90bc-148f423588fe', 'Whalefall', 'Allan Mandelbaum', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.20thcenturystudios.com/movies/whalefall'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Tom Holland', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Zendaya', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Sadie Sink', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Jacob Batalon', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Jon Bernthal', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Tramell Tillman', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Michael Mando', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Mark Ruffalo', 'b0000001-0000-4000-8000-000000000001', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Destin Daniel Cretton', 'b0000001-0000-4000-8000-000000000002', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Chris McKenna', 'b0000001-0000-4000-8000-000000000003', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Erik Sommers', 'b0000001-0000-4000-8000-000000000003', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Kevin Feige', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Amy Pascal', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Avi Arad', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday'),
    ('88c44f70-9154-46dd-84b3-3e54c1aadfca', 'Spider-Man: Brand New Day', 'Rachel O''Connor', 'b0000001-0000-4000-8000-000000000004', '{}'::text[], 'https://www.sonypictures.com/movies/spidermanbrandnewday');

DO $repair$
DECLARE
    entry record;
    selected_person uuid;
    matching_people uuid[];
    credit_id uuid;
    inserted integer;
    created_people integer := 0;
    created_credits integer := 0;
    repair_actor constant uuid := '00000000-0000-4000-8000-000000000002';
    repair_correlation constant uuid := '7dff73d9-406f-4258-aa3d-ce0d352c3a7c';
BEGIN
    IF (SELECT version::integer FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1) IS DISTINCT FROM 25 THEN
        RAISE EXCEPTION 'Expected Catalog schema 25';
    END IF;
    IF (SELECT count(*) FROM saved_credit_source) <> 28 THEN
        RAISE EXCEPTION 'Expected exactly 28 sourced credits';
    END IF;
    PERFORM t.id FROM title t JOIN (SELECT DISTINCT title_id FROM saved_credit_source) s ON s.title_id=t.id ORDER BY t.id FOR UPDATE OF t;
    IF (SELECT count(*) FROM title t JOIN (SELECT DISTINCT title_id,expected_title FROM saved_credit_source) s
        ON t.id=s.title_id AND t.display_title=s.expected_title WHERE t.kind='MOVIE' AND t.status='ACTIVE') <> 2 THEN
        RAISE EXCEPTION 'Expected the two existing active movies with exact names';
    END IF;
    IF (SELECT count(*) FROM movie_proposal p WHERE p.state='CREATED' AND p.approval_consumed_at IS NOT NULL
        AND p.actor_id=repair_actor AND p.approval_actor_id=repair_actor
        AND ((p.id='6aa15931-2154-4e5e-bb5f-623346b538ac' AND p.movie_id='474c56db-778a-4c0d-90bc-148f423588fe')
          OR (p.id='cf391cac-a1e9-49c7-90ba-5c15bc337b10' AND p.movie_id='88c44f70-9154-46dd-84b3-3e54c1aadfca'))) <> 2 THEN
        RAISE EXCEPTION 'Expected existing consumed approvals for the two saved movies';
    END IF;
    IF (SELECT count(*) FROM credit_role WHERE
        (id='b0000001-0000-4000-8000-000000000001' AND label='Actor' AND category='CAST' AND allows_characters)
        OR (id='b0000001-0000-4000-8000-000000000002' AND label='Director' AND category='CREATOR' AND NOT allows_characters)
        OR (id='b0000001-0000-4000-8000-000000000003' AND label='Writer' AND category='CREATOR' AND NOT allows_characters)
        OR (id='b0000001-0000-4000-8000-000000000004' AND label='Producer' AND category='CREATOR' AND NOT allows_characters)) <> 4 THEN
        RAISE EXCEPTION 'Expected unchanged Actor, Director, Writer and Producer roles';
    END IF;

    -- Briefly serialize person/credit writes, including writers that do not use name advisory locks.
    LOCK TABLE person, credit IN SHARE ROW EXCLUSIVE MODE;
    FOR entry IN SELECT DISTINCT person_name,source_url FROM saved_credit_source ORDER BY person_name,source_url LOOP
        SELECT array_agg(id ORDER BY id) INTO matching_people FROM person WHERE name=entry.person_name AND status='ACTIVE';
        IF cardinality(matching_people) > 1 THEN
            RAISE EXCEPTION 'Ambiguous active person name: %',entry.person_name;
        END IF;
        IF cardinality(matching_people) IS NULL THEN
            selected_person := md5('saved-movie-credits-person:' || entry.person_name)::uuid;
            INSERT INTO person(id,name) VALUES(selected_person,entry.person_name);
            created_people := created_people + 1;
            INSERT INTO operational_event(id,actor_id,actor_kind,action,subject_kind,subject_id,outcome,changes,correlation_id,event_type,operator_id)
            VALUES(md5('saved-movie-credits-person-audit:' || selected_person)::uuid,repair_actor,'HUMAN','CREATE_PERSON','PERSON',selected_person,'SUCCEEDED',
                jsonb_build_object('repair','add-saved-movie-credits','personName',entry.person_name,'sourceUrl',entry.source_url,
                    'authorization','User requested missing cast and crew on the two existing movies'),repair_correlation,'ACTIVITY','codex-user-authorized-repair');
        END IF;
    END LOOP;

    FOR entry IN SELECT * FROM saved_credit_source ORDER BY title_id,person_name,role_id LOOP
        SELECT id INTO STRICT selected_person FROM person WHERE name=entry.person_name AND status='ACTIVE';
        credit_id := md5('saved-movie-credits:' || entry.title_id || ':' || selected_person || ':' || entry.role_id)::uuid;
        INSERT INTO credit(id,title_id,person_id,role_id,characters)
        SELECT credit_id,entry.title_id,selected_person,entry.role_id,entry.character_names
        WHERE NOT EXISTS(SELECT 1 FROM credit WHERE title_id=entry.title_id AND person_id=selected_person AND role_id=entry.role_id);
        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted=1 THEN
            UPDATE title SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=entry.title_id;
            UPDATE person SET impact_version=impact_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=selected_person;
            INSERT INTO operational_event(id,actor_id,actor_kind,action,subject_kind,subject_id,outcome,changes,correlation_id,event_type,operator_id)
            VALUES(md5('saved-movie-credits-audit:' || credit_id)::uuid,repair_actor,'HUMAN','ADD_CREDIT','CREDIT',credit_id,'SUCCEEDED',
                jsonb_build_object('repair','add-saved-movie-credits','titleId',entry.title_id,'personId',selected_person,
                    'personName',entry.person_name,'roleId',entry.role_id,'characters',entry.character_names,'sourceUrl',entry.source_url,
                    'reuseBasis','Official studio cast and filmmaker information; names recorded for catalog attribution'),
                repair_correlation,'ACTIVITY','codex-user-authorized-repair');
            created_credits := created_credits+1;
        END IF;
    END LOOP;
    INSERT INTO catalog_audit_event(id,actor_id,actor_kind,action,subject_kind,subject_id,outcome,changes,correlation_id,created_at)
    SELECT e.id,e.actor_id,e.actor_kind,e.action,e.subject_kind,e.subject_id,e.outcome,e.changes,e.correlation_id,e.created_at
    FROM operational_event e
    WHERE e.correlation_id=repair_correlation AND e.changes->>'repair'='add-saved-movie-credits'
      AND e.actor_id=repair_actor AND e.action IN ('CREATE_PERSON','ADD_CREDIT')
    ON CONFLICT (id) DO NOTHING;
    RAISE NOTICE 'Saved-movie credit repair inserted % people and % credits',created_people,created_credits;
END
$repair$;
COMMIT;
