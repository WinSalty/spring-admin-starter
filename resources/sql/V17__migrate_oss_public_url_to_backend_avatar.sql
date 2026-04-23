UPDATE sys_user u
    INNER JOIN sys_file f ON u.avatar_url = f.file_url
SET u.avatar_url = CONCAT('/api/file/avatar/', f.id)
WHERE f.storage_type = 'aliyun-oss'
  AND f.file_url <> ''
  AND u.avatar_url <> '';

UPDATE sys_file
SET access_policy = 'private_read',
    file_url = ''
WHERE storage_type = 'aliyun-oss'
  AND deleted = 0;
