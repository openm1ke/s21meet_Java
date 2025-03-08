-- workplace_import.sql
DROP TABLE IF EXISTS workplace;

CREATE TABLE workplace (
   cluster_id BIGINT NOT NULL,
   row VARCHAR(255) NOT NULL,
   number INTEGER NOT NULL,
   login VARCHAR(255),
   PRIMARY KEY (cluster_id, row, number)
);

INSERT INTO workplace (cluster_id, row, number, login) VALUES
(1, 'A', 101, 'elevante'),
(1, 'B', 102, 'lucankri');