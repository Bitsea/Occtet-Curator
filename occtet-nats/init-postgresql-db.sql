
create extension if not exists vector;

/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

-- README: following code is created using DBeaver's "create DDL" feature, then modified to add "IF NOT EXISTS" clauses

-- public.logging_event_id_seq definition

-- DROP SEQUENCE public.logging_event_id_seq;

CREATE SEQUENCE public.logging_event_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;

-- public.logging_event definition

-- Drop table

-- DROP TABLE public.logging_event;

CREATE TABLE if not exists public.logging_event (
	timestmp int8 NOT NULL,
	formatted_message text NOT NULL,
	logger_name varchar(254) NOT NULL,
	level_string varchar(254) NOT NULL,
	thread_name varchar(254) NULL,
	reference_flag int2 NULL,
	arg0 varchar(254) NULL,
	arg1 varchar(254) NULL,
	arg2 varchar(254) NULL,
	arg3 varchar(254) NULL,
	caller_filename varchar(254) NOT NULL,
	caller_class varchar(254) NOT NULL,
	caller_method varchar(254) NOT NULL,
	caller_line bpchar(4) NOT NULL,
	event_id int8 DEFAULT nextval('logging_event_id_seq'::regclass) NOT NULL,
	CONSTRAINT logging_event_pkey PRIMARY KEY (event_id)
);


-- public.logging_event_exception definition

-- Drop table

-- DROP TABLE public.logging_event_exception;

CREATE TABLE if not exists public.logging_event_exception (
	event_id int8 NOT NULL,
	i int2 NOT NULL,
	trace_line TEXT NOT NULL,
	CONSTRAINT logging_event_exception_pkey PRIMARY KEY (event_id, i),
	CONSTRAINT logging_event_exception_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.logging_event(event_id)
);


-- public.logging_event_property definition

-- Drop table

-- DROP TABLE public.logging_event_property;

CREATE TABLE if not exists public.logging_event_property (
	event_id int8 NOT NULL,
	mapped_key varchar(254) NOT NULL,
	mapped_value varchar(1024) NULL,
	CONSTRAINT logging_event_property_pkey PRIMARY KEY (event_id, mapped_key),
	CONSTRAINT logging_event_property_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.logging_event(event_id)
);