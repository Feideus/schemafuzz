DROP TABLE actual_test_table;

CREATE TABLE actual_test_table (
    address_id integer DEFAULT nextval('address_address_id_seq'::regclass) NOT NULL,
    address character varying(50) NOT NULL,
    address2 character varying(50),
    district character varying(20) NOT NULL,
    city_id smallint NOT NULL,
    postal_code character varying(10),
    phone character varying(20) NOT NULL
    );

INSERT INTO actual_test_table VALUES (0	,23, 'Workhaven Lane' ,'Alberta' ,300,93360 ,14033335567);
INSERT INTO actual_test_table VALUES (4	,1411, 'Lillydale Drive' ,'QLD' ,576 ,88888,6172235589);
INSERT INTO actual_test_table VALUES (5	,1913, 'Hanoi Way' ,'Nagasaki' ,463 ,42420,28303384290);
