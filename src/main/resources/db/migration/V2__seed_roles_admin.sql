INSERT INTO roles (created_at, updated_at, name) VALUES
  (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ROLE_ADMIN'),
  (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ROLE_MANAGER'),
  (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ROLE_EMPLOYEE');

INSERT INTO app_users (created_at, updated_at, email, full_name, password_hash, enabled, failed_login_attempts)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin@carcare.local', 'System Admin', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', TRUE, 0);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM app_users u CROSS JOIN roles r
WHERE u.email = 'admin@carcare.local' AND r.name = 'ROLE_ADMIN';

INSERT INTO loyalty_rules (created_at, updated_at, name, points_per_currency_unit, discount_percent, active)
VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Default spend reward', 1.00, 5.00, TRUE);

