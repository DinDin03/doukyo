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

const LEAVE_HOUSEHOLD = gql`
  mutation LeaveHousehold($householdId: ID!, $confirmDelete: Boolean!) {
    leaveHousehold(householdId: $householdId, confirmDelete: $confirmDelete)
  }
`;
const RESTORABLE_HOUSEHOLDS = gql`
  query RestorableHouseholds {
    restorableHouseholds { id name }
  }
`;
const RESTORE_HOUSEHOLD = gql`
  mutation RestoreHousehold($householdId: ID!) {
    restoreHousehold(householdId: $householdId) { id }
  }
`;

export type RestorableHousehold = { id: string; name: string };

type HouseholdContextValue = {
  households: HouseholdSummary[];
  activeHousehold: HouseholdSummary | null;
  loading: boolean;
  createHousehold: (name: string) => Promise<void>;
  joinHousehold: (code: string) => Promise<void>;
  // confirmDelete must be true when you're the last member; the server refuses otherwise.
  leaveHousehold: (householdId: string, confirmDelete: boolean) => Promise<void>;
  fetchRestorable: () => Promise<RestorableHousehold[]>;
  restoreHousehold: (householdId: string) => Promise<void>;
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

  const leaveHousehold = useCallback(
    async (householdId: string, confirmDelete: boolean) => {
      await apolloClient.mutate({ mutation: LEAVE_HOUSEHOLD, variables: { householdId, confirmDelete } });
      // Drop everything cached for the household you just left, then let the gate
      // route on the fresh list.
      await apolloClient.clearStore();
      await load();
    },
    [load],
  );

  const fetchRestorable = useCallback(async () => {
    const { data } = await apolloClient.query<{ restorableHouseholds: RestorableHousehold[] }>({
      query: RESTORABLE_HOUSEHOLDS,
      fetchPolicy: 'network-only',
    });
    return data?.restorableHouseholds ?? [];
  }, []);

  const restoreHousehold = useCallback(
    async (householdId: string) => {
      await apolloClient.mutate({ mutation: RESTORE_HOUSEHOLD, variables: { householdId } });
      await load();
    },
    [load],
  );

  // No switcher yet (mobile roadmap M1 follow-up) — the first household wins.
  const activeHousehold = households[0] ?? null;

  return (
    <HouseholdContext.Provider value={{
        households,
        activeHousehold,
        loading,
        createHousehold,
        joinHousehold,
        leaveHousehold,
        fetchRestorable,
        restoreHousehold,
      }}>
      {children}
    </HouseholdContext.Provider>
  );
}

export function useHousehold(): HouseholdContextValue {
  const ctx = useContext(HouseholdContext);
  if (!ctx) throw new Error('useHousehold must be used within a HouseholdProvider');
  return ctx;
}
