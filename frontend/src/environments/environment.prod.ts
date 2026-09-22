export const environment = {
  production: true,
  msalConfig: {
    auth: {
      clientId: '61d2f1d7-99e9-4cf7-83fc-51456a9eb5b6',
      authority: 'https://login.microsoftonline.com/73d72038-30bf-4ab9-85bc-a402de679470',
      redirectUri: 'https://32.193.45.223/'
    },
    cache: {
      cacheLocation: 'localStorage',
      storeAuthStateInCookie: true
    }
  },
  apiConfig: {
    baseUrl: 'https://32.193.45.223/api/v1',
    scope: 'api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation',
    protectedResourceScopes: ['User.Read', 'api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation'],
    protectedResourceMap: new Map<string, Array<string>>([
      ['https://32.193.45.223/api/v1/*', ['api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation']],
      ['https://32.193.45.223/api/v1', ['api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation']],
      ['/api/v1/*', ['api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation']],
      ['/api/v1', ['api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation']]
    ])
  }
};
