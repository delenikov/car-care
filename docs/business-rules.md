# Authentication Rules

## JWT Authentication

The application uses JWT access and refresh tokens.

### Login Flow

1. User submits username and password.
2. Backend validates credentials.
3. Backend returns:

    * Access Token
    * Refresh Token

### Access Token

* Lifetime: 15 minutes
* Sent in Authorization header:

Bearer <access-token>

### Refresh Token

* Lifetime: 30 days
* Stored securely
* Used only to obtain a new access token

### Token Refresh Flow

1. Frontend sends request with access token.
2. Backend returns 401 Unauthorized if token expired.
3. Frontend automatically sends refresh token.
4. Backend validates refresh token.
5. Backend issues new access token.
6. Original request is retried automatically.
7. User must not be redirected to login.

### Logout

Logout invalidates refresh token.
