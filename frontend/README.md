# CarCare ASMS Frontend

React 19 + TypeScript + Vite baseline for the automotive service management UI.

Authentication keeps only the short-lived access token in `sessionStorage` under `carcare.accessToken`. Refresh is performed through `/api/auth/refresh` with `withCredentials`, so long-lived refresh tokens can remain in an HTTP-only cookie owned by the backend.
