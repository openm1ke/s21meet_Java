-- workplace_import.sql
DROP TABLE IF EXISTS workplace;

CREATE TABLE workplace (
   cluster_id BIGINT NOT NULL,
   row VARCHAR(255) NOT NULL,
   number INTEGER NOT NULL,
   login VARCHAR(255),
   exp_value INTEGER,
   level_code INTEGER,
   stage_group_name VARCHAR(255),
   stage_name VARCHAR(255),
   PRIMARY KEY (cluster_id, row, number)
);

INSERT INTO workplace (cluster_id, row, number, login, exp_value, level_code, stage_group_name, stage_name) VALUES
(1, 'A', 101, 'elevante', 1000, 5, 'Group1', 'Stage1'),
(1, 'B', 102, 'lucankri', 800, 4, 'Group2', 'Stage2');