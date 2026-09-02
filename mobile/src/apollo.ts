import { ApolloClient, CombinedGraphQLErrors, HttpLink, InMemoryCache, from, split } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { createClient } from 'graphql-ws';
import Constants from 'expo-constants';
import { clearTokens, getAccessToken, getRefreshToken, isAccessStale, saveTokens } from './auth/tokens';

// The backend URL — derived from Metro's host so it follows your Mac's LAN IP.
const metroHost = Constants.expoConfig?.hostUri?.split(':')[0] ?? 'localhost';
export const API_URL = `http://${metroHost}:8080/graphql`;
// Subscriptions have their own path: /graphql only accepts POST, so it rejects the
// WebSocket upgrade (a GET) with 405.
export const WS_URL = `ws://${metroHost}:8080/graphql-ws`;

// AuthContext registers a handler here so a failed refresh can reset the app to
// its signed-out state (clearing React state, not just tokens).
let onUnauthenticated: () => void = () => {};
export function setUnauthenticatedHandler(fn: () => void) {
  onUnauthenticated = fn;
}

const AUTH_OPS = ['SignIn', 'SignUp', 'Refresh', 'GoogleSignIn', 'SignOut'];

// Exchange the refresh token for a fresh access token, via a RAW fetch (not Apollo,
// which would recurse through these links). Dedupes concurrent refreshes with a
// shared promise. On failure it clears tokens and signals sign-out.
let refreshing: Promise<string | null> | null = null;
function refreshAccessToken(): Promise<string | null> {
  if (refreshing) return refreshing;
  refreshing = (async () => {
    const refreshToken = await getRefreshToken();
    if (!refreshToken) return null;
    try {
      const res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: 'mutation Refresh($rt:String!){ refresh(refreshToken:$rt){ accessToken refreshToken } }',
          variables: { rt: refreshToken },
        }),
      });
      const json = await res.json();
      const data = json?.data?.refresh;
      if (!data) throw new Error('refresh rejected');
      await saveTokens(data.accessToken, data.refreshToken);
      return data.accessToken as string;
    } catch {
      await clearTokens();
      onUnauthenticated();
      return null;
    }
  })().finally(() => {
    refreshing = null;
  });
  return refreshing;
}

// Attach the access token to every non-auth request; refresh first if it's stale.
const authLink = setContext(async (operation, prevContext) => {
  const headers = prevContext.headers ?? {};
  if (AUTH_OPS.includes(operation.operationName ?? '')) return { headers };

  let token = await getAccessToken();
  if (token && (await isAccessStale())) {
    token = await refreshAccessToken();
  }
  return { headers: { ...headers, ...(token ? { Authorization: `Bearer ${token}` } : {}) } };
});

// If a request still comes back UNAUTHORIZED (e.g. token revoked server-side), the
// session is truly dead — sign out. Auth operations are exempt (their errors are
// handled by the screens / refresh logic).
const errorLink = onError(({ error, operation }) => {
  // In Apollo v4, GraphQL errors arrive wrapped in a CombinedGraphQLErrors object.
  let unauthorized = false;
  if (CombinedGraphQLErrors.is(error)) {
    unauthorized = error.errors.some(
      (e) => (e.extensions as { classification?: string } | undefined)?.classification === 'UNAUTHORIZED',
    );
  }
  if (unauthorized && !AUTH_OPS.includes(operation.operationName ?? '')) {
    clearTokens();
    onUnauthenticated();
  }
});

// A WebSocket can't carry an Authorization header, so the token goes in the
// connection_init payload. connectionParams is a function so every (re)connect
// picks up the current token rather than one captured at startup.
const wsLink = new GraphQLWsLink(
  createClient({
    url: WS_URL,
    lazy: true,
    retryAttempts: Infinity,
    connectionParams: async () => {
      const token = await getAccessToken();
      return token ? { Authorization: `Bearer ${token}` } : {};
    },
  }),
);

// Subscriptions go over the socket; queries and mutations stay on HTTP.
const transportLink = split(
  ({ query }) => {
    const def = getMainDefinition(query);
    return def.kind === 'OperationDefinition' && def.operation === 'subscription';
  },
  wsLink,
  from([authLink, new HttpLink({ uri: API_URL })]),
);

export const apolloClient = new ApolloClient({
  link: from([errorLink, transportLink]),
  cache: new InMemoryCache(),
});
