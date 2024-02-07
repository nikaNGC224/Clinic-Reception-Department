--
-- PostgreSQL database dump
--

-- Dumped from database version 15.4
-- Dumped by pg_dump version 16.0

-- Started on 2024-02-07 15:58:41

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 246 (class 1255 OID 32803)
-- Name: create_diagnosis_report(character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_diagnosis_report(ch_diagnosis character varying) RETURNS TABLE("Фамилия" character varying, "Имя" character varying, "Отчество" character varying, "Палата" character varying)
    LANGUAGE plpgsql
    AS $$
begin    
    return query
    select first_name,
		last_name,
		pather_name,
		(
			select name from wards
			where id = people.ward_id
		)
	from people
	join diagnosis
	on people.diagnosis_id = (
		select id from diagnosis
		where name = ch_diagnosis
	)
	group by people.id;
end; 
$$;


ALTER FUNCTION public.create_diagnosis_report(ch_diagnosis character varying) OWNER TO postgres;

--
-- TOC entry 226 (class 1255 OID 16389)
-- Name: func_trigger(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.func_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin 
    declare
        ward_max_count integer;
        ward_busy_count bigint;
    begin 
        select max_count into ward_max_count
        from wards
        where wards.id = new.ward_id;

        select count(*) into ward_busy_count
        from people
        where people.ward_id = new.ward_id;

        if ward_max_count <= ward_busy_count  then 
            raise exception 'Палата не имеет свободных мест!';
        end if;

        return new;
    end;
end;
$$;


ALTER FUNCTION public.func_trigger() OWNER TO postgres;

--
-- TOC entry 227 (class 1255 OID 16390)
-- Name: func_trigger2(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.func_trigger2() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    declare 
        people_count bigint;
    begin
        select count(*) into people_count
        from people
        where people.diagnosis_id = new.id;

        if people_count > 0 then
            raise exception 'В данной палате уже есть пациенты с другим диагнозом!';
        end if;

        return new;
    end;
end;
$$;


ALTER FUNCTION public.func_trigger2() OWNER TO postgres;

--
-- TOC entry 240 (class 1255 OID 16391)
-- Name: func_trigger3(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.func_trigger3() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
declare
    people_count integer;
    free_ward_id integer;
    man_to_move integer;
    man_diagnosis_id integer;
begin
    select count(*) into people_count
    from people
    where people.ward_id = old.id;

    while people_count > 0 loop
        select diagnosis_id into man_diagnosis_id
        from people
        where people.ward_id = old.id limit 1;

        select wards.id into free_ward_id
        from wards
        join people on people.ward_id = wards.id
        where people.diagnosis_id = man_diagnosis_id
        and wards.id <> old.id
        and wards.max_count > (
            select count(*) from people
            where people.ward_id = wards.id
        ) limit 1;

        if free_ward_id is null then
            raise exception'Нет свободного места в других палатах';
        end if;

        select id into man_to_move
        from people
        where people.ward_id = old.id limit 1
        for update;

        update people
        set ward_id = free_ward_id
        where id = man_to_move;
        
        people_count := people_count - 1;
    end loop;

    return old;
end;
$$;


ALTER FUNCTION public.func_trigger3() OWNER TO postgres;

--
-- TOC entry 247 (class 1255 OID 32795)
-- Name: insert_patient(character varying, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.insert_patient(name1 character varying, name2 character varying, name3 character varying, chosen_diagnosis character varying, chosen_ward character varying) RETURNS TABLE(my_id integer)
    LANGUAGE plpgsql
    AS $$
declare
	ch_diag_id integer;
	ch_ward_id integer;
begin
	select diagnosis.id into ch_diag_id
	from diagnosis
	where diagnosis.name = chosen_diagnosis;
	
	select wards.id into ch_ward_id
	from wards
	where wards.name = chosen_ward;
	
	return query
	insert into people (first_name, last_name, pather_name, diagnosis_id, ward_id)
    values (name1, name2, name3, ch_diag_id, ch_ward_id)
	returning id;
end;
$$;


ALTER FUNCTION public.insert_patient(name1 character varying, name2 character varying, name3 character varying, chosen_diagnosis character varying, chosen_ward character varying) OWNER TO postgres;

--
-- TOC entry 245 (class 1255 OID 32782)
-- Name: select_people_from_ward(character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.select_people_from_ward(ward character varying) RETURNS TABLE(man_id integer, first_name character varying, last_name character varying, pather_name character varying)
    LANGUAGE plpgsql
    AS $$
declare
	chosen_ward_id integer;
begin
	select wards.id into chosen_ward_id
	from wards
	where name = ward;
	
	return query
	select people.id as Код,
		people.first_name as Фамилия,
		people.last_name as Имя,
		people.pather_name as Отчество
	from people
	inner join wards
	on people.ward_id = chosen_ward_id
	group by people.id;
end;
$$;


ALTER FUNCTION public.select_people_from_ward(ward character varying) OWNER TO postgres;

--
-- TOC entry 248 (class 1255 OID 32800)
-- Name: update_patient(integer, character varying, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_patient(in_id integer, in_first_name character varying, in_last_name character varying, in_pather_name character varying, in_diagnosis character varying, in_ward character varying) RETURNS TABLE(my_id integer)
    LANGUAGE plpgsql
    AS $$
declare
	ch_diag_id integer;
	ch_ward_id integer;
begin
	select diagnosis.id into ch_diag_id
	from diagnosis
	where diagnosis.name = in_diagnosis;
	
	select wards.id into ch_ward_id
	from wards
	where wards.name = in_ward;
	
	return query
	update people
	set first_name = in_first_name, last_name = in_last_name, pather_name = in_pather_name,
		diagnosis_id = ch_diag_id, ward_id = ch_ward_id
	where id = in_id
	returning id;
end;
$$;


ALTER FUNCTION public.update_patient(in_id integer, in_first_name character varying, in_last_name character varying, in_pather_name character varying, in_diagnosis character varying, in_ward character varying) OWNER TO postgres;

--
-- TOC entry 241 (class 1255 OID 16392)
-- Name: ward_occupancy_statistic(numeric, numeric); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.ward_occupancy_statistic(lower_bound numeric, upper_bound numeric) RETURNS TABLE(diagnosis_name character varying, ward_id integer, avg_occupancy_coef numeric)
    LANGUAGE plpgsql
    AS $$

DECLARE
    cur CURSOR FOR -- объявляем курсор-иттератор для цикла
        SELECT d.name AS diagnosis_name, -- по сути, построчно рисуем возвращаемую таблицу
               w.id AS ward_id,
               COUNT(p.id) AS patient_count,
               w.max_count AS max_count
        FROM diagnosis d
        JOIN people p ON p.diagnosis_id = d.id
        JOIN wards w ON p.ward_id = w.id
        GROUP BY d.name, w.max_count, w.id
        HAVING COUNT(p.id) / w.max_count BETWEEN lower_bound AND upper_bound; -- считаем только отношения в заданном диапазоне
    occupancy_coef NUMERIC;
BEGIN
    FOR c IN cur LOOP -- используя курсор, бегаем по циклу
        occupancy_coef := c.patient_count / (c.max_count - c.patient_count); -- присвоение значений в PizdesQL делается с помощью ":="
        RETURN QUERY SELECT c.diagnosis_name, c.ward_id, occupancy_coef; -- return query закидывает результат в возвращаемую таблицу
    END LOOP;
END; $$;


ALTER FUNCTION public.ward_occupancy_statistic(lower_bound numeric, upper_bound numeric) OWNER TO postgres;

--
-- TOC entry 242 (class 1255 OID 16393)
-- Name: ward_statistic(numeric, numeric); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.ward_statistic(lower_bound numeric, upper_bound numeric) RETURNS TABLE(diagnosis_name character varying, ward_id integer, avg_coeff numeric)
    LANGUAGE plpgsql
    AS $$
declare
    cursr cursor for
        select diagnosis.name as diagnosis_name,
               wards.id as ward_id,
               count(people.id) as people_count,
               wards.max_count as max_count
        from diagnosis

        join people on people.diagnosis_id = diagnosis.id
        join wards on people.ward_id = wards.id
        group by diagnosis.name, wards.max_count, wards.id
        having count(people.id) / max_count
        between lower_bound and upper_bound;

        coeff numeric;
begin
    for c in cursr loop
        coeff := c.people_count / (c.max_count - c.people_count);
        
        return query select c.diagnosis_name, c.ward_id, coeff;
    end loop;
end;
$$;


ALTER FUNCTION public.ward_statistic(lower_bound numeric, upper_bound numeric) OWNER TO postgres;

--
-- TOC entry 243 (class 1255 OID 16394)
-- Name: wards_func(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.wards_func() RETURNS TABLE(ward_name character varying, people_count bigint)
    LANGUAGE plpgsql
    AS $$
begin
    return query
    select wards.name,
    count(people.id)
    from wards

    left join people
    on wards.id = people.ward_id
    group by wards.id
    order by wards.name;
end;
$$;


ALTER FUNCTION public.wards_func() OWNER TO postgres;

--
-- TOC entry 244 (class 1255 OID 16395)
-- Name: wards_func_2(character varying, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.wards_func_2(diagnosis1 character varying, diagnosis2 character varying) RETURNS TABLE(ward_name character varying, people_count bigint)
    LANGUAGE plpgsql
    AS $$
declare
    avg_count decimal;
begin
    select avg(count) into avg_count
    from (
        select count(people.id) as count
        from people

        join diagnosis on people.diagnosis_id = diagnosis.id
        join wards on people.ward_id = wards.id

        where diagnosis.name in (diagnosis1, diagnosis2)
        group by wards.id
     ) as subquery;

    
    return query
    select wards.name, count(people.id)
    from people
             join diagnosis on people.diagnosis_id = diagnosis.id
             join wards on people.ward_id = wards.id
    where diagnosis.name in (diagnosis1, diagnosis2)
    group by wards.id
    having count(people.id) < avg_count;
end; 
$$;


ALTER FUNCTION public.wards_func_2(diagnosis1 character varying, diagnosis2 character varying) OWNER TO postgres;

--
-- TOC entry 228 (class 1255 OID 16396)
-- Name: wards_func_3(integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.wards_func_3(in_ward_id integer, OUT res_ward_id integer, OUT min_ratio bigint) RETURNS record
    LANGUAGE plpgsql
    AS $$
declare
    p_diag_id integer;
begin
    select diagnosis_id into p_diag_id
    from people
    where ward_id = in_ward_id limit 1;

    select wards.id into res_ward_id
    from wards
    join people on wards.id = people.ward_id
    where people.diagnosis_id = p_diag_id
    group by wards.id, wards.max_count
    order by count(people.id)
        / (wards.max_count - count(people.id)) limit 1;

    select (count(people.id)) 
        / (wards.max_count - count(people.id))
    into min_ratio from wards
    join people on wards.id = people.ward_id
    where people.diagnosis_id <> p_diag_id
    group by wards.id, wards.max_count
    order by (count(people.id) 
        / wards.max_count - count(people.id)) limit 1;
end;
$$;


ALTER FUNCTION public.wards_func_3(in_ward_id integer, OUT res_ward_id integer, OUT min_ratio bigint) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 225 (class 1259 OID 32790)
-- Name: ch_diag_id; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ch_diag_id (
    id integer
);


ALTER TABLE public.ch_diag_id OWNER TO postgres;

--
-- TOC entry 214 (class 1259 OID 16397)
-- Name: diagnosis; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.diagnosis (
    id integer NOT NULL,
    name character varying(50)
);


ALTER TABLE public.diagnosis OWNER TO postgres;

--
-- TOC entry 215 (class 1259 OID 16400)
-- Name: diagnosis_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.diagnosis_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.diagnosis_id_seq OWNER TO postgres;

--
-- TOC entry 3388 (class 0 OID 0)
-- Dependencies: 215
-- Name: diagnosis_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.diagnosis_id_seq OWNED BY public.diagnosis.id;


--
-- TOC entry 216 (class 1259 OID 16401)
-- Name: people; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.people (
    id integer NOT NULL,
    first_name character varying(20) NOT NULL,
    last_name character varying(20) NOT NULL,
    pather_name character varying(20),
    diagnosis_id integer NOT NULL,
    ward_id integer NOT NULL
);


ALTER TABLE public.people OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 32776)
-- Name: patients; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.patients AS
 SELECT people.id AS "Код",
    people.first_name AS "Фамилия",
    people.last_name AS "Имя",
    people.pather_name AS "Отчество"
   FROM public.people;


ALTER VIEW public.patients OWNER TO postgres;

--
-- TOC entry 217 (class 1259 OID 16408)
-- Name: people_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.people_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.people_id_seq OWNER TO postgres;

--
-- TOC entry 3389 (class 0 OID 0)
-- Dependencies: 217
-- Name: people_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.people_id_seq OWNED BY public.people.id;


--
-- TOC entry 224 (class 1259 OID 32789)
-- Name: people_id_seq1; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.people ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.people_id_seq1
    START WITH 7
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 220 (class 1259 OID 16442)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id integer NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(50) NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 32773)
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.users ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 218 (class 1259 OID 16409)
-- Name: wards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wards (
    id integer NOT NULL,
    name character varying(50),
    max_count integer
);


ALTER TABLE public.wards OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16412)
-- Name: wards_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.wards_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.wards_id_seq OWNER TO postgres;

--
-- TOC entry 3390 (class 0 OID 0)
-- Dependencies: 219
-- Name: wards_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.wards_id_seq OWNED BY public.wards.id;


--
-- TOC entry 223 (class 1259 OID 32783)
-- Name: wards_view; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.wards_view AS
SELECT
    NULL::character varying(50) AS "Палаты",
    NULL::bigint AS "Занято",
    NULL::integer AS "Вместимость",
    NULL::character varying AS "Диагноз";


ALTER VIEW public.wards_view OWNER TO postgres;

--
-- TOC entry 3213 (class 2604 OID 16417)
-- Name: diagnosis id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.diagnosis ALTER COLUMN id SET DEFAULT nextval('public.diagnosis_id_seq'::regclass);


--
-- TOC entry 3214 (class 2604 OID 16419)
-- Name: wards id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wards ALTER COLUMN id SET DEFAULT nextval('public.wards_id_seq'::regclass);


--
-- TOC entry 3381 (class 0 OID 32790)
-- Dependencies: 225
-- Data for Name: ch_diag_id; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.ch_diag_id (id) VALUES (1);


--
-- TOC entry 3372 (class 0 OID 16397)
-- Dependencies: 214
-- Data for Name: diagnosis; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.diagnosis (id, name) VALUES (1, 'ОРВИ');
INSERT INTO public.diagnosis (id, name) VALUES (2, 'Перелом');
INSERT INTO public.diagnosis (id, name) VALUES (3, 'Гастрит');


--
-- TOC entry 3374 (class 0 OID 16401)
-- Dependencies: 216
-- Data for Name: people; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (3, 'Мальцева', 'Ксения', 'Егоровна', 2, 2);
INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (6, 'Богомолова', 'София', 'Львовна', 2, 2);
INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (17, 'Захаров', 'Лев', 'Ярославович', 1, 3);
INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (20, 'Ковалев', 'Александр', 'Тимофеевич', 3, 1);
INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (21, 'Федоров', 'Матвей', 'Федорович', 3, 1);
INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (26, 'Жуков', 'Иван', 'Васильевич', 1, 3);
INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (1, 'Пономарева', 'Виктория', 'Ильинична', 1, 3);
INSERT INTO public.people (id, first_name, last_name, pather_name, diagnosis_id, ward_id) OVERRIDING SYSTEM VALUE VALUES (27, 'Раисов', 'Сергей', 'Ефимович', 1, 3);


--
-- TOC entry 3378 (class 0 OID 16442)
-- Dependencies: 220
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.users (id, username, password) OVERRIDING SYSTEM VALUE VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3');
INSERT INTO public.users (id, username, password) OVERRIDING SYSTEM VALUE VALUES (2, 'user', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO public.users (id, username, password) OVERRIDING SYSTEM VALUE VALUES (3, 'user1', '24c9e15e52afc47c225b757e7bee1f9d');
INSERT INTO public.users (id, username, password) OVERRIDING SYSTEM VALUE VALUES (4, 'aaa', '47bce5c74f589f4867dbd57e9ca9f808');
INSERT INTO public.users (id, username, password) OVERRIDING SYSTEM VALUE VALUES (5, 'ada', '8c8d357b5e872bbacd45197626bd5759');


--
-- TOC entry 3376 (class 0 OID 16409)
-- Dependencies: 218
-- Data for Name: wards; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.wards (id, name, max_count) VALUES (1, 'Палата 1', 4);
INSERT INTO public.wards (id, name, max_count) VALUES (2, 'Палата 2', 3);
INSERT INTO public.wards (id, name, max_count) VALUES (3, 'Палата 3', 5);


--
-- TOC entry 3391 (class 0 OID 0)
-- Dependencies: 215
-- Name: diagnosis_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.diagnosis_id_seq', 13, true);


--
-- TOC entry 3392 (class 0 OID 0)
-- Dependencies: 217
-- Name: people_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.people_id_seq', 54, true);


--
-- TOC entry 3393 (class 0 OID 0)
-- Dependencies: 224
-- Name: people_id_seq1; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.people_id_seq1', 27, true);


--
-- TOC entry 3394 (class 0 OID 0)
-- Dependencies: 221
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 5, true);


--
-- TOC entry 3395 (class 0 OID 0)
-- Dependencies: 219
-- Name: wards_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.wards_id_seq', 51, true);


--
-- TOC entry 3216 (class 2606 OID 16421)
-- Name: diagnosis diagnosis_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.diagnosis
    ADD CONSTRAINT diagnosis_pkey PRIMARY KEY (id);


--
-- TOC entry 3218 (class 2606 OID 16423)
-- Name: people people_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.people
    ADD CONSTRAINT people_pkey PRIMARY KEY (id);


--
-- TOC entry 3222 (class 2606 OID 16446)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 3220 (class 2606 OID 16425)
-- Name: wards wards_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wards
    ADD CONSTRAINT wards_pkey PRIMARY KEY (id);


--
-- TOC entry 3371 (class 2618 OID 32786)
-- Name: wards_view _RETURN; Type: RULE; Schema: public; Owner: postgres
--

CREATE OR REPLACE VIEW public.wards_view AS
 SELECT wards.name AS "Палаты",
    count(people.ward_id) AS "Занято",
    wards.max_count AS "Вместимость",
    COALESCE(diagnosis.name, ''::character varying) AS "Диагноз"
   FROM ((public.wards
     LEFT JOIN public.people ON ((people.ward_id = wards.id)))
     LEFT JOIN public.diagnosis ON ((diagnosis.id = people.diagnosis_id)))
  GROUP BY wards.id, diagnosis.name
  ORDER BY wards.name;


--
-- TOC entry 3226 (class 2620 OID 16427)
-- Name: people trigger_1; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_1 BEFORE INSERT ON public.people FOR EACH ROW EXECUTE FUNCTION public.func_trigger();


--
-- TOC entry 3225 (class 2620 OID 16428)
-- Name: diagnosis trigger_2; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_2 BEFORE UPDATE ON public.diagnosis FOR EACH ROW EXECUTE FUNCTION public.func_trigger2();


--
-- TOC entry 3227 (class 2620 OID 16429)
-- Name: wards trigger_3; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_3 BEFORE DELETE ON public.wards FOR EACH ROW EXECUTE FUNCTION public.func_trigger3();


--
-- TOC entry 3223 (class 2606 OID 16430)
-- Name: people people_diagnosis_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.people
    ADD CONSTRAINT people_diagnosis_id_fkey FOREIGN KEY (diagnosis_id) REFERENCES public.diagnosis(id);


--
-- TOC entry 3224 (class 2606 OID 16435)
-- Name: people people_ward_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.people
    ADD CONSTRAINT people_ward_id_fkey FOREIGN KEY (ward_id) REFERENCES public.wards(id);


--
-- TOC entry 3387 (class 0 OID 0)
-- Dependencies: 5
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

GRANT USAGE ON SCHEMA public TO postgres;


-- Completed on 2024-02-07 15:58:42

--
-- PostgreSQL database dump complete
--

