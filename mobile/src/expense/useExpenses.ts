import { useCallback, useState } from 'react';
import { useFocusEffect } from 'expo-router';
import { gql } from '@apollo/client';
import { apolloClient } from '../apollo';

export type ExpenseCategory = 'GROCERIES' | 'BILLS' | 'DINING' | 'HOUSEHOLD' | 'OTHER';
export type SplitMethod = 'EVENLY' | 'EXACT' | 'PERCENTAGE' | 'WEIGHTS';

export type Share = {
  id: string;
  amountCents: number;
  isPaid: boolean;
  user: { id: string; name: string };
};
export type Expense = {
  id: string;
  description: string;
  amountCents: number;
  category: ExpenseCategory;
  createdAt: string;
  paidBy: { id: string; name: string };
  shares: Share[];
};
export type Balance = { userId: string; userName: string; netCents: number };

const EXPENSE_FIELDS = `
  id description amountCents category createdAt
  paidBy { id name }
  shares { id amountCents isPaid user { id name } }
`;

const EXPENSES = gql`query Expenses($householdId: ID!) {
  expenses(householdId: $householdId) { ${EXPENSE_FIELDS} }
  balances(householdId: $householdId) { userId userName netCents }
}`;

const CREATE_EXPENSE = gql`
  mutation CreateExpense(
    $householdId: ID!, $paidById: ID!, $description: String!, $amountCents: Int!,
    $category: ExpenseCategory!, $method: SplitMethod!, $participants: [ParticipantInput!]!
  ) {
    createExpense(
      householdId: $householdId, paidById: $paidById, description: $description,
      amountCents: $amountCents, category: $category, method: $method, participants: $participants
    ) { ${EXPENSE_FIELDS} }
  }
`;

const SETTLE_SHARE = gql`
  mutation SettleShare($shareId: ID!) {
    settleShare(shareId: $shareId) { id isPaid }
  }
`;

// Money is integer cents everywhere, exactly as the backend stores it. Dividing by
// 100 happens ONLY here, for display — never in a calculation.
export function formatCents(cents: number): string {
  return `$${(Math.abs(cents) / 100).toFixed(2)}`;
}

export function useExpenses(householdId: string | undefined) {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [balances, setBalances] = useState<Balance[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!householdId) return;
    setError(null);
    try {
      const { data } = await apolloClient.query<{ expenses: Expense[]; balances: Balance[] }>({
        query: EXPENSES,
        variables: { householdId },
        fetchPolicy: 'network-only',
      });
      setExpenses(data?.expenses ?? []);
      setBalances(data?.balances ?? []);
    } catch {
      setError('Could not load expenses');
    } finally {
      setLoading(false);
    }
  }, [householdId]);

  // Refetch whenever the screen regains focus, not just on mount. Each caller of
  // this hook holds its own state, so the Add screen refreshing its copy does
  // nothing for the Expenses tab's copy — without this, returning from Add shows
  // a list that is missing the expense just created.
  useFocusEffect(
    useCallback(() => {
      refresh();
    }, [refresh]),
  );

  const createExpense = useCallback(
    async (input: {
      paidById: string;
      description: string;
      amountCents: number;
      category: ExpenseCategory;
      method: SplitMethod;
      participants: { userId: string; value?: number }[];
    }) => {
      if (!householdId) return;
      await apolloClient.mutate({
        mutation: CREATE_EXPENSE,
        variables: { householdId, ...input },
      });
      await refresh();
    },
    [householdId, refresh],
  );

  const settleShare = useCallback(
    async (shareId: string) => {
      await apolloClient.mutate({ mutation: SETTLE_SHARE, variables: { shareId } });
      await refresh();
    },
    [refresh],
  );

  return { expenses, balances, loading, error, refresh, createExpense, settleShare };
}

// What this expense means for one person, in words the row can render directly.
export function yourPosition(expense: Expense, userId: string | undefined) {
  const outstanding = expense.shares.filter((s) => !s.isPaid).reduce((sum, s) => sum + s.amountCents, 0);
  if (expense.paidBy.id === userId) {
    return outstanding === 0
      ? { label: 'Settled', owed: false }
      : { label: `You are owed ${formatCents(outstanding)}`, owed: true };
  }
  const yours = expense.shares.find((s) => s.user.id === userId);
  if (!yours) return { label: 'Not your split', owed: false };
  return yours.isPaid
    ? { label: 'Settled', owed: false }
    : { label: `You owe ${formatCents(yours.amountCents)}`, owed: false };
}
