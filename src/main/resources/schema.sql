CREATE TABLE dbo.employee_assignment_history (
    -- ★ 複合主キー（5要素）
    employee_id          NVARCHAR(20)  NOT NULL,
    org_code             NVARCHAR(10)  NOT NULL,
    job_code             NVARCHAR(10)  NOT NULL,
    effective_from       DATE          NOT NULL,
    seq_no               INT           NOT NULL,

    -- ★ 属性（更新候補の多い列を用意）
    effective_to         DATE              NULL,
    status_code          NVARCHAR(10)      NOT NULL DEFAULT 'ACTIVE',
    base_location_code   NVARCHAR(10)      NULL,
    employment_type      NVARCHAR(10)      NULL,
    grade_code           NVARCHAR(10)      NULL,
    salary_band_code     NVARCHAR(10)      NULL,
    manager_emp_id       NVARCHAR(20)      NULL,
    project_code         NVARCHAR(20)      NULL,
    cost_center_code     NVARCHAR(20)      NULL,
    work_pattern_code    NVARCHAR(10)      NULL,
    shift_group_code     NVARCHAR(10)      NULL,
    allow_remote         BIT               NULL,
    fte_ratio            DECIMAL(5,2)      NULL,

    -- ダミー列（更新負荷再現用）
    attr1                NVARCHAR(100)     NULL,
    attr2                NVARCHAR(100)     NULL,
    attr3                NVARCHAR(100)     NULL,
    attr4                NVARCHAR(100)     NULL,
    attr5                NVARCHAR(100)     NULL,
    attr6                NVARCHAR(100)     NULL,
    attr7                NVARCHAR(100)     NULL,
    attr8                NVARCHAR(100)     NULL,
    attr9                NVARCHAR(100)     NULL,
    attr10               NVARCHAR(100)     NULL,
    attr11               NVARCHAR(100)     NULL,
    attr12               NVARCHAR(100)     NULL,

    -- 監査系
    created_at           DATETIME2(3)      NOT NULL DEFAULT SYSUTCDATETIME(),
    created_by           NVARCHAR(50)      NULL,
    updated_at           DATETIME2(3)      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_by           NVARCHAR(50)      NULL,
    rv                   ROWVERSION,   -- 楽観制御・同時更新検知向け

    CONSTRAINT PK_employee_assignment_history
        PRIMARY KEY CLUSTERED (employee_id, org_code, job_code, effective_from, seq_no)
);