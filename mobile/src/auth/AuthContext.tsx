import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { gql } from '@apollo/client';
import { apolloClient, setUnauthenticatedHandler } from '../apollo';
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from './tokens';
import { getGoogleIdToken, signOutOfGoogle } from './google';

export type AuthUser = { id: string; name: string; email: string };

// Named operations (query Me / mutation SignIn / mutation SignUp) — the names let
// the Apollo links treat auth calls specially (no auto-refresh on them).
const ME = gql`
  query Me {
    me { id name email }
  }
`;
const SIGN_IN = gql`
  mutation SignIn($email: String!, $password: String!) {
    signIn(email: $email, password: $password) {
      accessToken
      refreshToken
      user { id name email }
    }
  }
`;
const SIGN_UP = gql`
  mutation SignUp($name: String!, $email: String!, $password: String!) {
    signUp(name: $name, email: $email, password: $password) {
      accessToken
      refreshToken
      user { id name email }
    }
  }
`;
const GOOGLE_SIGN_IN = gql`
  mutation GoogleSignIn($idToken: String!) {
    googleSignIn(idToken: $idToken) {
      accessToken
      refreshToken
      user { id name email }
    }
  }
`;
const SIGN_OUT = gql`
  mutation SignOut($refreshToken: String!) {
    signOut(refreshToken: $refreshToken)
  }
`;

type AuthPayload = { accessToken: string; refreshToken: string; user: AuthUser };

type AuthContextValue = {
  user: AuthUser | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (name: string, email: string, password: string) => Promise<void>;
  googleSignIn: () => Promise<'signed-in' | 'cancelled'>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  // A failed token refresh (deep in the Apollo layer) resets us to signed-out.
  useEffect(() => {
    setUnauthenticatedHandler(() => setUser(null));
  }, []);

  // On launch: if a token exists, ask the backend who we are. The authLink refreshes
  // it first if stale; if everything fails, we clear and land signed-out.
  useEffect(() => {
    let active = true;
    (async () => {
      const token = await getAccessToken();
      if (!token) {
        if (active) setLoading(false);
        return;
      }
      try {
        const { data } = await apolloClient.query<{ me: AuthUser }>({ query: ME, fetchPolicy: 'network-only' });
        if (active) setUser(data?.me ?? null);
      } catch {
        await clearTokens();
        if (active) setUser(null);
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const { data } = await apolloClient.mutate<{ signIn: AuthPayload }>({ mutation: SIGN_IN, variables: { email, password } });
    if (!data) throw new Error('Sign in failed');
    await saveTokens(data.signIn.accessToken, data.signIn.refreshToken);
    setUser(data.signIn.user);
  }, []);

  const signUp = useCallback(async (name: string, email: string, password: string) => {
    const { data } = await apolloClient.mutate<{ signUp: AuthPayload }>({ mutation: SIGN_UP, variables: { name, email, password } });
    if (!data) throw new Error('Sign up failed');
    await saveTokens(data.signUp.accessToken, data.signUp.refreshToken);
    setUser(data.signUp.user);
  }, []);

  const googleSignIn = useCallback(async (): Promise<'signed-in' | 'cancelled'> => {
    const idToken = await getGoogleIdToken();
    if (!idToken) return 'cancelled';
    const { data } = await apolloClient.mutate<{ googleSignIn: AuthPayload }>({
      mutation: GOOGLE_SIGN_IN,
      variables: { idToken },
    });
    if (!data) throw new Error('Google sign in failed');
    await saveTokens(data.googleSignIn.accessToken, data.googleSignIn.refreshToken);
    setUser(data.googleSignIn.user);
    return 'signed-in';
  }, []);

  const signOut = useCallback(async () => {
    const refreshToken = await getRefreshToken();
    if (refreshToken) {
      // Best-effort: revoke server-side so a leaked refresh token stops working.
      // Never block local sign-out on this — offline sign-out must still work.
      try {
        await apolloClient.mutate({ mutation: SIGN_OUT, variables: { refreshToken } });
      } catch {
        // ignore — the token will just sit unused until it expires
      }
    }
    await signOutOfGoogle();
    await clearTokens();
    await apolloClient.clearStore(); // drop any cached data belonging to the old user
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, signIn, signUp, googleSignIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
