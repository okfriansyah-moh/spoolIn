UPDATE movie_proposal
SET expires_at = created_at + INTERVAL '24 hours'
WHERE state = 'REVIEW'
  AND expires_at < created_at + INTERVAL '24 hours';
