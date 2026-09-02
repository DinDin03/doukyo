import { useCallback, useEffect, useRef, useState } from 'react';
import { gql } from '@apollo/client';
import { apolloClient } from '../apollo';
import { notifyUnreadChanged } from './unreadStore';

export type ChatMessage = {
  id: string;
  body: string;
  clientId: string;
  createdAt: string;
  sender: { id: string; name: string };
  pending?: boolean;
  failed?: boolean;
};

const FIELDS = `id body clientId createdAt sender { id name }`;

const MESSAGES = gql`query Messages($householdId: ID!, $before: ID, $limit: Int) {
  messages(householdId: $householdId, before: $before, limit: $limit) { ${FIELDS} }
}`;
const MESSAGES_SINCE = gql`query MessagesSince($householdId: ID!, $after: ID!) {
  messagesSince(householdId: $householdId, after: $after) { ${FIELDS} }
}`;
const SEND_MESSAGE = gql`mutation SendMessage($householdId: ID!, $body: String!, $clientId: String!) {
  sendMessage(householdId: $householdId, body: $body, clientId: $clientId) { ${FIELDS} }
}`;
const MESSAGE_ADDED = gql`subscription MessageAdded($householdId: ID!) {
  messageAdded(householdId: $householdId) { ${FIELDS} }
}`;
const MARK_READ = gql`mutation MarkMessagesRead($householdId: ID!, $messageId: ID!) {
  markMessagesRead(householdId: $householdId, messageId: $messageId)
}`;

const PAGE = 50;

// Messages are held oldest-first. Merging is by clientId first (so a pending
// bubble is replaced by its server echo rather than duplicated), then by id.
function merge(existing: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const byClientId = new Map(existing.map((m) => [m.clientId, m]));
  for (const m of incoming) byClientId.set(m.clientId, m);
  return [...byClientId.values()]
    .filter((m, i, all) => m.pending || all.findIndex((o) => !o.pending && o.id === m.id) === i)
    .sort((a, b) => {
      if (a.pending) return 1;
      if (b.pending) return -1;
      return Number(a.id) - Number(b.id);
    });
}

export function useChat(householdId: string | undefined) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // Ref, not state: the subscription callback needs the current value without
  // being torn down and resubscribed on every message.
  const latestIdRef = useRef<number>(0);

  const rememberLatest = useCallback((list: ChatMessage[]) => {
    for (const m of list) {
      if (!m.pending) latestIdRef.current = Math.max(latestIdRef.current, Number(m.id));
    }
  }, []);

  // Initial load.
  useEffect(() => {
    if (!householdId) return;
    let active = true;
    setLoading(true);
    setError(null);
    apolloClient
      .query<{ messages: ChatMessage[] }>({
        query: MESSAGES,
        variables: { householdId, limit: PAGE },
        fetchPolicy: 'network-only',
      })
      .then(({ data }) => {
        if (!active) return;
        const asc = [...(data?.messages ?? [])].reverse(); // server returns newest first
        setMessages(asc);
        rememberLatest(asc);
        setHasMore((data?.messages?.length ?? 0) === PAGE);
      })
      .catch(() => active && setError('Could not load messages'))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [householdId, rememberLatest]);

  // Live delivery + reconnect backfill. A subscription only delivers while its
  // socket is open, so on every (re)start we ask for everything missed since the
  // last id we hold — otherwise messages sent during a drop are lost silently.
  useEffect(() => {
    if (!householdId) return;

    const backfill = () => {
      if (!latestIdRef.current) return;
      apolloClient
        .query<{ messagesSince: ChatMessage[] }>({
          query: MESSAGES_SINCE,
          variables: { householdId, after: String(latestIdRef.current) },
          fetchPolicy: 'network-only',
        })
        .then(({ data }) => {
          const missed = data?.messagesSince ?? [];
          if (missed.length) {
            setMessages((cur) => merge(cur, missed));
            rememberLatest(missed);
          }
        })
        .catch(() => {});
    };

    const sub = apolloClient
      .subscribe<{ messageAdded: ChatMessage }>({ query: MESSAGE_ADDED, variables: { householdId } })
      .subscribe({
        next: ({ data }) => {
          const m = data?.messageAdded;
          if (!m) return;
          setMessages((cur) => merge(cur, [m]));
          rememberLatest([m]);
        },
        // A dropped socket surfaces as an error; graphql-ws reconnects on its own,
        // and this resubscribe triggers a fresh backfill.
        error: () => setTimeout(backfill, 1500),
      });

    backfill();
    return () => sub.unsubscribe();
  }, [householdId, rememberLatest]);

  const send = useCallback(
    async (body: string) => {
      if (!householdId) return;
      const text = body.trim();
      if (!text) return;
      const clientId = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
      const optimistic: ChatMessage = {
        id: `pending-${clientId}`,
        body: text,
        clientId,
        createdAt: new Date().toISOString(),
        sender: { id: 'me', name: '' },
        pending: true,
      };
      setMessages((cur) => merge(cur, [optimistic]));
      try {
        const { data } = await apolloClient.mutate<{ sendMessage: ChatMessage }>({
          mutation: SEND_MESSAGE,
          variables: { householdId, body: text, clientId },
        });
        if (data?.sendMessage) {
          // Replaces the pending bubble in place — matched on clientId.
          setMessages((cur) => merge(cur.filter((m) => m.clientId !== clientId), [data.sendMessage]));
          rememberLatest([data.sendMessage]);
        }
      } catch {
        setMessages((cur) =>
          cur.map((m) => (m.clientId === clientId ? { ...m, pending: false, failed: true } : m)),
        );
      }
    },
    [householdId, rememberLatest],
  );

  const loadOlder = useCallback(async () => {
    if (!householdId || loadingMore || !hasMore || messages.length === 0) return;
    const oldest = messages.find((m) => !m.pending);
    if (!oldest) return;
    setLoadingMore(true);
    try {
      const { data } = await apolloClient.query<{ messages: ChatMessage[] }>({
        query: MESSAGES,
        variables: { householdId, before: oldest.id, limit: PAGE },
        fetchPolicy: 'network-only',
      });
      const older = data?.messages ?? [];
      setHasMore(older.length === PAGE);
      if (older.length) setMessages((cur) => merge(cur, older));
    } catch {
      // leave the thread as-is; the user can pull again
    } finally {
      setLoadingMore(false);
    }
  }, [householdId, loadingMore, hasMore, messages]);

  const markRead = useCallback(async () => {
    if (!householdId || !latestIdRef.current) return;
    try {
      await apolloClient.mutate({
        mutation: MARK_READ,
        variables: { householdId, messageId: String(latestIdRef.current) },
      });
      notifyUnreadChanged();
    } catch {
      // a missed bookmark just means the badge lingers; not worth surfacing
    }
  }, [householdId]);

  return { messages, loading, loadingMore, hasMore, error, send, loadOlder, markRead };
}
