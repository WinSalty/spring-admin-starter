INSERT INTO sys_dict_data (data_code, dict_type_id, dict_type, label, value, sort_no, status, remark, created_at, updated_at)
SELECT 'DD1011', t.id, t.dict_type, '待激活', 'pending', 3, 'active', '注册账号等待邮件激活。', NOW(), NOW()
FROM sys_dict_type t
WHERE t.dict_type = 'sys_status'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_dict_data d
      WHERE d.dict_type = 'sys_status'
        AND d.value = 'pending'
        AND d.deleted = 0
  );
