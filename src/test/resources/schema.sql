-- テスト環境用のemployeesテーブル作成SQL
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    position VARCHAR(50),
    employment_status VARCHAR(20) NOT NULL,
    hire_date DATE NOT NULL,
    phone_number VARCHAR(15),
    email VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(5),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0
);