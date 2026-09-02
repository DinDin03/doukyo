import { useCallback, useEffect, useState } from 'react';
import { gql } from '@apollo/client';
import { apolloClient } from '../apollo';
import { onUnreadChanged } from './unreadStore';

const UNREAD = gql`
  query UnreadMessageCount($householdId: ID!) {
    unreadMessageCount(householdId: $householdId)
  }
`;
const MESSAGE_ADDED = gql`
  subscription UnreadMessageAdded($householdId: ID!) {
    messageAdded(householdId: $householdId) {
      id
    }
  }
`;

// Unread badge for the tab bar. Seeded by a query, then nudged by the same live
// subscription the chat screen uses, so the badge moves without polling.
export function useUnreadCount(householdId: string | undefined) {
  const [count, setCount] = useState(0);

  const refresh = useCallback(() => {
    if (!householdId) return;
    apolloClient
      .query<{ unreadMessageCount: number }>({
        query: UNREAD,
        variables: { householdId },
        fetchPolicy: 'network-only',
      })
      .then(({ data }) => setCount(data?.unreadMessageCount ?? 0))
      .catch(() => {});
  }, [householdId]);

  useEffect(() => {
    refresh();
    return onUnreadChanged(refresh); // the chat screen marked the thread read
  }, [refresh]);

  useEffect(() => {
    if (!householdId) return;
    const sub = apolloClient
      .subscribe({ query: MESSAGE_ADDED, variables: { householdId } })
      .subscribe({ next: () => refresh(), error: () => {} });
    return () => sub.unsubscribe();
  }, [householdId, refresh]);

  return { count, refresh };
}
