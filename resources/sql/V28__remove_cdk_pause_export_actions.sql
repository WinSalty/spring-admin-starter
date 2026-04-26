UPDATE sys_menu
SET description = '默认展示全部CDK，支持筛选、复制和失效'
WHERE code = 'cdk_code';

DELETE FROM sys_role_action
WHERE action_code IN ('cdk:batch:export', 'cdk:batch:pause');
