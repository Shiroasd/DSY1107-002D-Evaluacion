export const environment = {
  production: false,
  msalConfig: {
    auth: {
      clientId: '57c1db2d-484b-463a-b993-45c3ef349e3c',
      authority: 'https://login.microsoftonline.com/73d72038-30bf-4ab9-85bc-a402de679470',
      redirectUri: 'http://localhost:4200'
    },
    cache: {
      cacheLocation: 'localStorage',
      storeAuthStateInCookie: true
    }
  },
  apiConfig: {
    baseUrl: 'http://32.192.168.114:8081/api/v1',
    scope: 'api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30/user_impersonation',
    protectedResourceScopes: [
      'User.Read'
    ],
    protectedResourceMap: new Map<string, Array<string>>([
      ['http://32.192.168.114:8081/api/v1/*', ['User.Read']],
      ['http://localhost:8081/api/v1/*', ['User.Read']]
    ])
  }
};
