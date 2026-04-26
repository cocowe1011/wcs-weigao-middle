-- 用户表结构升级SQL (SQL Server版本)

-- 1. 添加用户角色字段
ALTER TABLE user_info ADD user_role NVARCHAR(20) DEFAULT 'OPERATOR';
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'用户角色（ADMIN-管理员，OPERATOR-操作员）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'user_info', @level2type=N'COLUMN', @level2name=N'user_role';

-- 2. 添加登录失败次数字段
ALTER TABLE user_info ADD login_fail_count INT DEFAULT 0;
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'登录失败次数', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'user_info', @level2type=N'COLUMN', @level2name=N'login_fail_count';

-- 3. 添加是否锁定字段
ALTER TABLE user_info ADD is_locked INT DEFAULT 0;
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'是否锁定（0-否，1-是）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'user_info', @level2type=N'COLUMN', @level2name=N'is_locked';

-- 5. 添加更新时间字段
ALTER TABLE user_info ADD update_time DATETIME DEFAULT GETDATE();
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'更新时间', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'user_info', @level2type=N'COLUMN', @level2name=N'update_time';

-- 6. 添加user_code唯一性约束（如果不存在）
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UK_user_code' AND object_id = OBJECT_ID('user_info'))
BEGIN
ALTER TABLE user_info ADD CONSTRAINT UK_user_code UNIQUE (user_code);
END

-- 7. 插入默认管理员账号（如果不存在）
IF NOT EXISTS (SELECT 1 FROM user_info WHERE user_code = 'admin')
BEGIN
    INSERT INTO user_info (user_id, user_code, user_password, user_name, user_role, login_fail_count, is_locked, create_time, update_time)
    VALUES (1, 'admin', 'wcs-admin', N'系统管理员', 'ADMIN', 0, 0, GETDATE(), GETDATE());
END

-- 8. 更新现有用户为操作员角色
UPDATE user_info SET user_role = 'OPERATOR' WHERE user_code != 'admin' AND user_role IS NULL;


-- =============================================
-- 生产批次三表（produce_batch / produce_pallet / produce_goods）
-- =============================================

-- produce_batch 批次表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'produce_batch')
BEGIN
    CREATE TABLE dbo.produce_batch (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        batch_no      NVARCHAR(64)   NOT NULL,
        status        CHAR(1)        NOT NULL DEFAULT '0',  -- 0待确认 1已确认 2生产中 3完成
        invalid_flag  CHAR(1)        NOT NULL DEFAULT '0',
        confirm_time  DATETIME2      NULL,
        finish_time   DATETIME2      NULL,
        created_at    DATETIME2      NOT NULL DEFAULT SYSDATETIME()
    );
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'生产批次表', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_batch';
END;

-- produce_pallet 托盘表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'produce_pallet')
BEGIN
    CREATE TABLE dbo.produce_pallet (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        batch_id      BIGINT         NOT NULL,
        pallet_no     NVARCHAR(64)   NOT NULL,
        tray_status   CHAR(1)        NOT NULL DEFAULT '0',  -- 0待扫 1部分已扫 2全部已扫
        invalid_flag  CHAR(1)        NOT NULL DEFAULT '0',
        created_at    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_pallet_batch FOREIGN KEY (batch_id) REFERENCES dbo.produce_batch(id)
    );
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'生产托盘表', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_pallet';
END;

-- produce_goods 货物表
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'produce_goods')
BEGIN
    CREATE TABLE dbo.produce_goods (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        batch_id      BIGINT         NOT NULL,
        pallet_id     BIGINT         NOT NULL,
        uid           NVARCHAR(128)  NOT NULL,
        product_name  NVARCHAR(128)  NOT NULL,
        spec          NVARCHAR(128)  NULL,
        remark        NVARCHAR(255)  NULL,
        scan_status   CHAR(1)        NOT NULL DEFAULT '0',  -- 货物扫码状态：0未扫；1已扫（01002/01006 任一位置扫到即置为1，重复扫码保持1并覆盖 scan_location/scan_time）
        scan_time     DATETIME2      NULL,
        scan_location NVARCHAR(64)   NULL,
        invalid_flag  CHAR(1)        NOT NULL DEFAULT '0',
        created_at    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_goods_batch  FOREIGN KEY (batch_id)  REFERENCES dbo.produce_batch(id),
        CONSTRAINT FK_goods_pallet FOREIGN KEY (pallet_id) REFERENCES dbo.produce_pallet(id),
        CONSTRAINT UK_goods_uid    UNIQUE (uid)
    );
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'生产货物表', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_goods';
    -- 索引
    CREATE INDEX IX_produce_goods_batch_id  ON dbo.produce_goods(batch_id);
    CREATE INDEX IX_produce_goods_pallet_id ON dbo.produce_goods(pallet_id);
END;

-- produce_goods.scan_status 字段注释（补充实际业务含义）
IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_goods')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_goods'), 'scan_status', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description',
        @value=N'货物扫码状态：0未扫；1已扫（01002/01006 任一位置扫到即置为1，重复扫码保持1并覆盖 scan_location/scan_time）',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_goods',
        @level2type=N'COLUMN', @level2name=N'scan_status';
END;

-- =============================================
-- 批次目的地设置流水表（produce_batch_destination）
-- 一个批次可多次设置目的地，每次设置生成一条记录
-- status: 0-激活(当前有效) 1-已取消
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'produce_batch_destination')
BEGIN
    CREATE TABLE dbo.produce_batch_destination (
        id               BIGINT        NOT NULL IDENTITY(1,1) PRIMARY KEY,
        batch_id         BIGINT        NOT NULL,
        destination_code NVARCHAR(16)  NOT NULL,  -- 目的地编码，如 3201~3215
        status           CHAR(1)       NOT NULL DEFAULT '0',  -- 0激活 1已取消
        set_time         DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
        cancel_time      DATETIME2     NULL,
        invalid_flag     CHAR(1)       NOT NULL DEFAULT '0',
        created_at       DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_dest_batch FOREIGN KEY (batch_id) REFERENCES dbo.produce_batch(id)
    );
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'批次目的地设置流水表', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_batch_destination';
    CREATE INDEX IX_produce_batch_destination_batch_id ON dbo.produce_batch_destination(batch_id);
END;

-- produce_batch_destination 字段注释（避免仅有表注释）
IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'id', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'主键ID',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'id';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'batch_id', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'批次ID',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'batch_id';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'destination_code', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'目的地编码（3201~3215）',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'destination_code';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'status', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'状态（0激活 1已取消）',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'status';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'set_time', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'设置时间',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'set_time';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'cancel_time', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'取消时间',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'cancel_time';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'invalid_flag', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'作废标识（0正常 1作废）',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'invalid_flag';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.extended_properties
    WHERE major_id = OBJECT_ID('dbo.produce_batch_destination')
      AND minor_id = COLUMNPROPERTY(OBJECT_ID('dbo.produce_batch_destination'), 'created_at', 'ColumnId')
      AND name = 'MS_Description'
)
BEGIN
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'创建时间',
        @level0type=N'SCHEMA', @level0name=N'dbo',
        @level1type=N'TABLE',  @level1name=N'produce_batch_destination',
        @level2type=N'COLUMN', @level2name=N'created_at';
END;

-- =============================================
-- produce_pallet 托盘表新增上货相关字段
-- =============================================
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.produce_pallet') AND name = 'virtual_id')
BEGIN
    ALTER TABLE dbo.produce_pallet ADD virtual_id NVARCHAR(64) NULL;
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'虚拟托盘ID（PC端触发写虚拟ID请求时自动生成）', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_pallet', @level2type=N'COLUMN', @level2name=N'virtual_id';
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.produce_pallet') AND name = 'load_status')
BEGIN
    ALTER TABLE dbo.produce_pallet ADD load_status CHAR(1) NOT NULL DEFAULT '0';
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'上货状态 0-未上货 1-已上货', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_pallet', @level2type=N'COLUMN', @level2name=N'load_status';
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.produce_pallet') AND name = 'load_time')
BEGIN
    ALTER TABLE dbo.produce_pallet ADD load_time DATETIME2 NULL;
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'上货时间', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_pallet', @level2type=N'COLUMN', @level2name=N'load_time';
END;

-- 发送目的地信息（目的地编码，全扫=原始编码，未全扫=编码拼3）
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.produce_pallet') AND name = 'send_destination_code')
BEGIN
    ALTER TABLE dbo.produce_pallet ADD send_destination_code NVARCHAR(32) NULL;
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'发送的目的地编码', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_pallet', @level2type=N'COLUMN', @level2name=N'send_destination_code';
END;

-- 是否已发送目的地 0-未发送 1-已发送
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.produce_pallet') AND name = 'send_status')
BEGIN
    ALTER TABLE dbo.produce_pallet ADD send_status CHAR(1) NOT NULL DEFAULT '0';
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'是否已发送目的地 0-未发送 1-已发送', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_pallet', @level2type=N'COLUMN', @level2name=N'send_status';
END;

-- 发送目的地时间
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.produce_pallet') AND name = 'send_time')
BEGIN
    ALTER TABLE dbo.produce_pallet ADD send_time DATETIME2 NULL;
    EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'发送目的地时间', @level0type=N'SCHEMA', @level0name=N'dbo', @level1type=N'TABLE', @level1name=N'produce_pallet', @level2type=N'COLUMN', @level2name=N'send_time';
END;
