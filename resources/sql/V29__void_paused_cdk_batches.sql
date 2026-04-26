UPDATE cdk_batch
SET status = 'voided'
WHERE status = 'paused';

UPDATE cdk_code c
INNER JOIN cdk_batch b ON b.id = c.batch_id
SET c.status = 'disabled', c.version = c.version + 1
WHERE b.status = 'voided'
  AND c.status = 'active';
