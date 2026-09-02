import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { gql } from '@apollo/client';
import { apolloClient } from '../apollo';
import { useAuth } from '../auth/AuthContext';

export type HouseholdMember = { id: string; name: string };
export type HouseholdSummary = {
  id: string;
  name: string;
  inviteCode: string;
  createdAt: string;
  members: HouseholdMember[];
};

const MY_HOUSEHOLDS = gql`
  query MyHouseholds {
    myHouseholds { id name inviteCode createdAt members { id name } }
  }
`;
const CREATE_HOUSEHOLD = gql`
  mutation CreateHousehold($name: String!) {
    createHousehold(name: $name) { id name inviteCode }
  }
`;
const JOIN_HOUSEHOLD = gql`
  mutation JoinHousehold($code: String!) {
    joinHousehold(code: $code) { id name inviteCode }
  }
`;

type HouseholdContextValue = {
  households: HouseholdSummary[];
  activeHousehold: HouseholdSummary | null;
  loading: boolean;
  createHousehold: (name: string) => Promise<void>;
  joinHousehold: (code: string) => Promise<void>;
};

const HouseholdContext = createContext<HouseholdContextValue | undefined>(undefined);

export function HouseholdProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [households, setHouseholds] = useState<HouseholdSummary[]>([]);
  const [loading, setLoading] = useState(true);

  // Reruns whenever `user` changes identity — i.e. right after sign-in, sign-out,
  // or session restore. No user -> no households, no network call.
  const load = useCallback(async () => {
    if (!user) {
      setHouseholds([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const { data } = await apolloClient.query<{ myHouseholds: HouseholdSummary[] }>({
        query: MY_HOUSEHOLDS,
        fetchPolicy: 'network-only',
      });
      setHouseholds(data?.myHouseholds ?? []);
    } catch {
      setHouseholds([]);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    load();
  }, [load]);

  const createHousehold = useCallback(
    async (name: string) => {
      const { data } = await apolloClient.mutate<{ createHousehold: HouseholdSummary }>({
        mutation: CREATE_HOUSEHOLD,
        variables: { name },
      });
      if (!data) throw new Error('Could not create household');
      await load();
    },
    [load],
  );

  const joinHousehold = useCallback(
    async (code: string) => {
      const { data } = await apolloClient.mutate<{ joinHousehold: HouseholdSummary }>({
        mutation: JOIN_HOUSEHOLD,
        variables: { code },
      });
      if (!data) throw new Error('Could not join household');
      await load();
    },
    [load],
  );

  // No switcher yet (mobile roadmap M1 follow-up) — the first household wins.
  const activeHousehold = households[0] ?? null;

  return (
    <HouseholdContext.Provider value={{ households, activeHousehold, loading, createHousehold, joinHousehold }}>
      {children}
    </HouseholdContext.Provider>
  );
}

export function useHousehold(): HouseholdContextValue {
  const ctx = useContext(HouseholdContext);
  if (!ctx) throw new Error('useHousehold must be used within a HouseholdProvider');
  return ctx;
}
