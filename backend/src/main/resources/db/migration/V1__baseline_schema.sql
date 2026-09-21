-- Baseline schema, taken from the database Hibernate had built with
-- ddl-auto=update, so it is exactly what the entities expect.
--
-- From here the schema is owned by these migration files rather than by
-- Hibernate: every profile runs with ddl-auto=validate, which fails fast if an
-- entity and the schema have drifted apart instead of silently altering tables
-- underneath a running system.
--
-- An existing database is baselined rather than rebuilt
-- (spring.flyway.baseline-on-migrate=true), so this never runs against one that
-- already has these tables.

--
--

--
-- Name: appointments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.appointments (
    id bigint NOT NULL,
    appointment_date date NOT NULL,
    appointment_time time(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    is_active boolean NOT NULL,
    notes character varying(500),
    reason character varying(255) NOT NULL,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    doctor_id bigint NOT NULL,
    patient_id bigint NOT NULL
);

--
-- Name: appointments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.appointments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: appointments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.appointments_id_seq OWNED BY public.appointments.id;

--
-- Name: bills; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bills (
    id bigint NOT NULL,
    bill_date date NOT NULL,
    consultation_fee numeric(10,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(500),
    is_active boolean NOT NULL,
    medications_fee numeric(10,2),
    other_charges numeric(10,2),
    payment_method character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    tests_fee numeric(10,2),
    total_amount numeric(10,2) NOT NULL,
    updated_at timestamp(6) without time zone,
    doctor_id bigint NOT NULL,
    patient_id bigint NOT NULL
);

--
-- Name: bills_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bills_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: bills_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bills_id_seq OWNED BY public.bills.id;

--
-- Name: departments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.departments (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(500),
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(20),
    updated_at timestamp(6) without time zone
);

--
-- Name: departments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.departments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: departments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.departments_id_seq OWNED BY public.departments.id;

--
-- Name: doctors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doctors (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(100) NOT NULL,
    first_name character varying(50) NOT NULL,
    is_active boolean NOT NULL,
    last_name character varying(50) NOT NULL,
    license_number character varying(50) NOT NULL,
    phone character varying(20) NOT NULL,
    specialization character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone,
    department_id bigint NOT NULL,
    user_id bigint
);

--
-- Name: doctors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.doctors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: doctors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.doctors_id_seq OWNED BY public.doctors.id;

--
-- Name: laboratory_tests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.laboratory_tests (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    is_active boolean NOT NULL,
    notes character varying(500),
    reference_max character varying(50),
    reference_min character varying(50),
    result_unit character varying(50) NOT NULL,
    result_value character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    test_date date NOT NULL,
    test_name character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone,
    patient_id bigint NOT NULL
);

--
-- Name: laboratory_tests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.laboratory_tests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: laboratory_tests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.laboratory_tests_id_seq OWNED BY public.laboratory_tests.id;

--
-- Name: medical_histories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.medical_histories (
    id bigint NOT NULL,
    condition_name character varying(100) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(500),
    diagnosis_date date NOT NULL,
    doctor_notes character varying(500),
    is_active boolean NOT NULL,
    status character varying(20) NOT NULL,
    treatment character varying(500),
    updated_at timestamp(6) without time zone,
    patient_id bigint NOT NULL
);

--
-- Name: medical_histories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.medical_histories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: medical_histories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.medical_histories_id_seq OWNED BY public.medical_histories.id;

--
-- Name: patients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.patients (
    id bigint NOT NULL,
    address character varying(255) NOT NULL,
    blood_group character varying(5),
    city character varying(50) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    date_of_birth date NOT NULL,
    email character varying(100),
    emergency_contact character varying(100),
    emergency_phone character varying(20),
    first_name character varying(50) NOT NULL,
    gender character varying(10) NOT NULL,
    is_active boolean NOT NULL,
    last_name character varying(50) NOT NULL,
    phone character varying(20) NOT NULL,
    state character varying(50) NOT NULL,
    updated_at timestamp(6) without time zone,
    zip_code character varying(10) NOT NULL,
    room_id bigint,
    user_id bigint
);

--
-- Name: patients_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.patients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: patients_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.patients_id_seq OWNED BY public.patients.id;

--
-- Name: prescriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prescriptions (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    dosage character varying(50) NOT NULL,
    duration character varying(50) NOT NULL,
    frequency character varying(50) NOT NULL,
    instructions character varying(255),
    is_active boolean NOT NULL,
    medicine_name character varying(100) NOT NULL,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    appointment_id bigint NOT NULL,
    doctor_id bigint NOT NULL,
    patient_id bigint NOT NULL
);

--
-- Name: prescriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.prescriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: prescriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.prescriptions_id_seq OWNED BY public.prescriptions.id;

--
-- Name: rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rooms (
    id bigint NOT NULL,
    amenities character varying(100),
    capacity integer NOT NULL,
    cost_per_day double precision NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(500),
    is_active boolean NOT NULL,
    occupied_beds integer NOT NULL,
    room_number character varying(20) NOT NULL,
    room_type character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    ward character varying(50) NOT NULL
);

--
-- Name: rooms_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rooms_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: rooms_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rooms_id_seq OWNED BY public.rooms.id;

--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(100) NOT NULL,
    first_name character varying(50),
    is_active boolean NOT NULL,
    last_name character varying(50),
    password character varying(255) NOT NULL,
    phone character varying(20),
    role character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    username character varying(50) NOT NULL
);

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;

--
-- Name: appointments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointments ALTER COLUMN id SET DEFAULT nextval('public.appointments_id_seq'::regclass);

--
-- Name: bills id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills ALTER COLUMN id SET DEFAULT nextval('public.bills_id_seq'::regclass);

--
-- Name: departments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments ALTER COLUMN id SET DEFAULT nextval('public.departments_id_seq'::regclass);

--
-- Name: doctors id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors ALTER COLUMN id SET DEFAULT nextval('public.doctors_id_seq'::regclass);

--
-- Name: laboratory_tests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.laboratory_tests ALTER COLUMN id SET DEFAULT nextval('public.laboratory_tests_id_seq'::regclass);

--
-- Name: medical_histories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_histories ALTER COLUMN id SET DEFAULT nextval('public.medical_histories_id_seq'::regclass);

--
-- Name: patients id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.patients ALTER COLUMN id SET DEFAULT nextval('public.patients_id_seq'::regclass);

--
-- Name: prescriptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescriptions ALTER COLUMN id SET DEFAULT nextval('public.prescriptions_id_seq'::regclass);

--
-- Name: rooms id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms ALTER COLUMN id SET DEFAULT nextval('public.rooms_id_seq'::regclass);

--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);

--
-- Name: appointments appointments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);

--
-- Name: bills bills_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_pkey PRIMARY KEY (id);

--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);

--
-- Name: doctors doctors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT doctors_pkey PRIMARY KEY (id);

--
-- Name: doctors idx_doctor_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT idx_doctor_email UNIQUE (email);

--
-- Name: patients idx_patient_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT idx_patient_email UNIQUE (email);

--
-- Name: laboratory_tests laboratory_tests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.laboratory_tests
    ADD CONSTRAINT laboratory_tests_pkey PRIMARY KEY (id);

--
-- Name: medical_histories medical_histories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_histories
    ADD CONSTRAINT medical_histories_pkey PRIMARY KEY (id);

--
-- Name: patients patients_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT patients_pkey PRIMARY KEY (id);

--
-- Name: prescriptions prescriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescriptions
    ADD CONSTRAINT prescriptions_pkey PRIMARY KEY (id);

--
-- Name: rooms rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (id);

--
-- Name: doctors uk_1xu5x0jae737xae254t4rgcd1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT uk_1xu5x0jae737xae254t4rgcd1 UNIQUE (license_number);

--
-- Name: users uk_6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

--
-- Name: rooms uk_7ljglxlj90ln3lbas4kl983m2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT uk_7ljglxlj90ln3lbas4kl983m2 UNIQUE (room_number);

--
-- Name: patients uk_a370hmxgv0l5c9panryr1ji7d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT uk_a370hmxgv0l5c9panryr1ji7d UNIQUE (email);

--
-- Name: doctors uk_caifv0va46t2mu85cg5afmayf; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT uk_caifv0va46t2mu85cg5afmayf UNIQUE (email);

--
-- Name: departments uk_j6cwks7xecs5jov19ro8ge3qk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT uk_j6cwks7xecs5jov19ro8ge3qk UNIQUE (name);

--
-- Name: users uk_r43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_r43af9ap4edm43mmtq01oddj6 UNIQUE (username);

--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

--
-- Name: idx_appointment_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appointment_created_at ON public.appointments USING btree (created_at);

--
-- Name: idx_appointment_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appointment_date ON public.appointments USING btree (appointment_date);

--
-- Name: idx_appointment_doctor_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appointment_doctor_id ON public.appointments USING btree (doctor_id);

--
-- Name: idx_appointment_patient_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appointment_patient_id ON public.appointments USING btree (patient_id);

--
-- Name: idx_appointment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appointment_status ON public.appointments USING btree (status);

--
-- Name: idx_bill_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bill_date ON public.bills USING btree (bill_date);

--
-- Name: idx_bill_doctor_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bill_doctor_id ON public.bills USING btree (doctor_id);

--
-- Name: idx_bill_patient_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bill_patient_id ON public.bills USING btree (patient_id);

--
-- Name: idx_bill_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bill_status ON public.bills USING btree (status);

--
-- Name: idx_doctor_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doctor_department_id ON public.doctors USING btree (department_id);

--
-- Name: idx_doctor_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doctor_is_active ON public.doctors USING btree (is_active);

--
-- Name: idx_doctor_specialization; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doctor_specialization ON public.doctors USING btree (specialization);

--
-- Name: idx_doctor_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doctor_user_id ON public.doctors USING btree (user_id);

--
-- Name: idx_patient_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_patient_created_at ON public.patients USING btree (created_at);

--
-- Name: idx_patient_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_patient_is_active ON public.patients USING btree (is_active);

--
-- Name: idx_patient_phone; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_patient_phone ON public.patients USING btree (phone);

--
-- Name: idx_patient_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_patient_user_id ON public.patients USING btree (user_id);

--
-- Name: prescriptions fk24chc88e4so7cd6melh11rv6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescriptions
    ADD CONSTRAINT fk24chc88e4so7cd6melh11rv6 FOREIGN KEY (doctor_id) REFERENCES public.doctors(id);

--
-- Name: laboratory_tests fk31rs02qqtfke4kh98fhac77d2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.laboratory_tests
    ADD CONSTRAINT fk31rs02qqtfke4kh98fhac77d2 FOREIGN KEY (patient_id) REFERENCES public.patients(id);

--
-- Name: medical_histories fk6c8jc8e1umxkpnrj7osnoqfe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.medical_histories
    ADD CONSTRAINT fk6c8jc8e1umxkpnrj7osnoqfe FOREIGN KEY (patient_id) REFERENCES public.patients(id);

--
-- Name: appointments fk8exap5wmg8kmb1g1rx3by21yt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk8exap5wmg8kmb1g1rx3by21yt FOREIGN KEY (patient_id) REFERENCES public.patients(id);

--
-- Name: prescriptions fke2fpvlkkcgcd40k4ufyyju2al; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescriptions
    ADD CONSTRAINT fke2fpvlkkcgcd40k4ufyyju2al FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);

--
-- Name: doctors fke9pf5qtxxkdyrwibaevo9frtk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT fke9pf5qtxxkdyrwibaevo9frtk FOREIGN KEY (user_id) REFERENCES public.users(id);

--
-- Name: patients fkehw4x5ovd8uekmlurrw8h9x7s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT fkehw4x5ovd8uekmlurrw8h9x7s FOREIGN KEY (room_id) REFERENCES public.rooms(id);

--
-- Name: bills fkiklkhnj1odoll0m9otela7gb9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkiklkhnj1odoll0m9otela7gb9 FOREIGN KEY (patient_id) REFERENCES public.patients(id);

--
-- Name: doctors fkl2mro81neln9topymd898urh1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT fkl2mro81neln9topymd898urh1 FOREIGN KEY (department_id) REFERENCES public.departments(id);

--
-- Name: bills fklon8kmudvr9gkvqe7ki38ewar; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fklon8kmudvr9gkvqe7ki38ewar FOREIGN KEY (doctor_id) REFERENCES public.doctors(id);

--
-- Name: appointments fkmujeo4tymoo98cmf7uj3vsv76; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fkmujeo4tymoo98cmf7uj3vsv76 FOREIGN KEY (doctor_id) REFERENCES public.doctors(id);

--
-- Name: prescriptions fkqydyol76jn1o37k1bdbkjgq74; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prescriptions
    ADD CONSTRAINT fkqydyol76jn1o37k1bdbkjgq74 FOREIGN KEY (patient_id) REFERENCES public.patients(id);

--
-- Name: patients fkuwca24wcd1tg6pjex8lmc0y7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.patients
    ADD CONSTRAINT fkuwca24wcd1tg6pjex8lmc0y7 FOREIGN KEY (user_id) REFERENCES public.users(id);

--
--
