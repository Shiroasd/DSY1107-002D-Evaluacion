export const environment = {
  production: true,
  msalConfig: {
    auth: {
      clientId: '57c1db2d-484b-463a-b993-45c3ef349e3c',
      authority: 'https://login.microsoftonline.com/73d72038-30bf-4ab9-85bc-a402de679470',
      redirectUri: 'https://32.193.45.223'
    }
  },
  apiConfig: {
    baseUrl: 'http://localhost:8080/api/v1',
    scope: 'api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation',
    protectedResourceMap: new Map<string, Array<string>>([
      ['http://localhost:8080/api/v1/*', ['api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation']]
    ])
  }
};
