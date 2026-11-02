# ReactFlow Android

ReactFlow is an Android starter project for Facebook Page engagement analytics and a manual reaction workflow.

## Features in this starter
- Kotlin + Jetpack Compose dashboard
- Light/dark theme toggle
- Facebook authorization through a backend OAuth callback
- Backend token exchange (App Secret stays on the server)
- Managed Page list
- Load accessible Page posts
- Open a post in Facebook for a person to react manually
- Starter reactions endpoint for authorized Graph API reads

## Important Meta limitations
- This app does not automate likes/reactions, create fake engagement, or simulate users.
- Personal profile feed access is restricted. The `user_posts` permission is requested as an optional capability; Meta may require App Review and may decline it. If unavailable, use the manual Facebook workflow.
- Page endpoints require the correct Page role/tasks and approved permissions. `pages_show_list` and `pages_read_engagement` are requested here. Meta may require Standard/Advanced Access and App Review depending on who uses the app.
- The reactions edge response is not guaranteed to expose every user's reaction or all aggregate analytics in every context. Handle permission errors and unavailable fields.

## Requirements
- Android Studio with JDK 17
- Node.js 20+
- A Meta Developer account and a Meta app configured for Facebook Login
- A public HTTPS backend URL for the OAuth callback (localhost is not suitable for real Facebook OAuth)

## 1. Configure Meta
1. Create an app at https://developers.facebook.com/apps/
2. Add/configure Facebook Login for the appropriate platform.
3. Add your exact HTTPS callback URL from `META_REDIRECT_URI` to Valid OAuth Redirect URIs.
4. Configure app domains, privacy policy URL, data deletion instructions and allowed domains.
5. Request only the permissions your use case needs: `pages_show_list`, `pages_read_engagement`; `user_posts` is optional for personal-profile posts.
6. Complete App Review and business verification if Meta requires Advanced Access for your intended users.
7. Test first with app roles (admin/developer/tester) and Pages they manage.

## 2. Run the backend
```bash
cd backend
cp .env.example .env
# Edit .env and insert your Meta App ID, App Secret, HTTPS domain and callback URL
npm install
npm start
```
Deploy behind HTTPS. Set `PUBLIC_BASE_URL` and `META_REDIRECT_URI` to the deployed domain.
The in-memory session map is for local development only. For production use encrypted persistent token storage, session expiry, CSRF/state persistence, rate limiting, audit logs, deletion workflows and a secrets manager.

## 3. Configure Android
Open the project root in Android Studio.
In `app/build.gradle.kts`, replace `API_BASE_URL`:
- Android emulator + local backend: `http://10.0.2.2:3000/`
- Physical phone: use your computer's LAN IP for local testing, or your deployed HTTPS backend URL.
For production, use HTTPS only. Disable cleartext traffic in `AndroidManifest.xml`.
Run on an emulator or device.

## 4. OAuth redirect
The backend redirects to `reactflow://auth?session=...`. The Android manifest registers this deep link.
For production, consider Android App Links with verified HTTPS links instead of a custom scheme, and use one-time, short-lived opaque session codes.

## 5. API endpoints
- `GET /health`
- `GET /auth/facebook`
- `GET /auth/callback`
- `GET /api/session/:session/pages`
- `GET /api/session/:session/posts?target=pages`
- `GET /api/session/:session/posts?target=personal`
- `GET /api/session/:session/reactions/:postId`

## Security checklist before production
- Use HTTPS end-to-end; remove cleartext traffic.
- Never ship Meta App Secret or access tokens in the APK.
- Store tokens encrypted at rest, restrict backend access, rotate/revoke on disconnect.
- Add expiry and revocation handling; do not use in-memory sessions.
- Use verified App Links, OAuth state persistence, CSRF protections, rate limits and secure logging.
- Implement Meta data deletion callback and privacy policy.
- Do not log tokens or OAuth codes.
- Request only permissions necessary for the features actually used.

## Notes
This is a starter source project, not a Meta-approved production app. Meta permission approval, app configuration, HTTPS deployment and your own credentials are required for live integration.
