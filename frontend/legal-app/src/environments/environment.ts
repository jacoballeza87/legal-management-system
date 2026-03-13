// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  wsUrl: 'ws://localhost:8080/ws',
  googleClientId: 'YOUR_GOOGLE_CLIENT_ID',
  githubClientId: 'YOUR_GITHUB_CLIENT_ID',
  oauthRedirectBase: 'http://localhost:4200',
  tokenKey: 'legal_access_token',
  refreshTokenKey: 'legal_refresh_token',
  deviceIdKey: 'legal_device_id',
  appName: 'LegalSys',
  appVersion: '1.0.0'
};
