import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  TextInput,
  View,
} from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Heading, Kicker } from '../../src/design/ui';
import { Avatar } from '../../src/design/widgets';
import { colors, fonts, ink, radius } from '../../src/design/theme';
import { useAuth } from '../../src/auth/AuthContext';
import { useHousehold } from '../../src/household/HouseholdContext';
import { ChatMessage, useChat } from '../../src/chat/useChat';

function timeOf(iso: string) {
  const d = new Date(iso);
  return isNaN(d.getTime()) ? '' : d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

function dayOf(iso: string) {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return 'Today';
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return d.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' });
}

export default function ChatScreen() {
  const insets = useSafeAreaInsets();
  const { user } = useAuth();
  const { activeHousehold } = useHousehold();
  const householdId = activeHousehold?.id;
  const { messages, loading, loadingMore, hasMore, error, send, loadOlder, markRead } = useChat(householdId);
  const [draft, setDraft] = useState('');
  const listRef = useRef<FlatList<ChatMessage>>(null);

  // Mark the thread read whenever new messages land while it's on screen.
  useEffect(() => {
    if (messages.length) markRead();
  }, [messages.length, markRead]);

  const submit = useCallback(() => {
    const text = draft.trim();
    if (!text) return;
    setDraft('');
    send(text);
  }, [draft, send]);

  if (!activeHousehold) return null;

  const renderItem = ({ item, index }: { item: ChatMessage; index: number }) => {
    const mine = item.sender.id === user?.id || item.pending;
    const prev = messages[index - 1];
    const showDay = !prev || dayOf(prev.createdAt) !== dayOf(item.createdAt);
    // Collapse the avatar/name on consecutive messages from the same person.
    const grouped = !showDay && prev && prev.sender.id === item.sender.id && !prev.pending && !item.pending;

    return (
      <View>
        {showDay ? (
          <View style={styles.dayRow}>
            <View style={styles.dayLine} />
            <Kicker color={ink(0.4)}>{dayOf(item.createdAt)}</Kicker>
            <View style={styles.dayLine} />
          </View>
        ) : null}
        <View style={[styles.row, mine && styles.rowMine]}>
          {!mine ? (
            <View style={styles.avatarSlot}>
              {!grouped ? <Avatar initial={item.sender.name.charAt(0).toUpperCase()} size={28} /> : null}
            </View>
          ) : null}
          <View style={[styles.bubble, mine ? styles.bubbleMine : styles.bubbleTheirs]}>
            {!mine && !grouped ? (
              <Kicker color={colors.accentRamp[700]} style={{ marginBottom: 3 }}>
                {item.sender.name}
              </Kicker>
            ) : null}
            <Body size={14.5} color={colors.text}>
              {item.body}
            </Body>
            <View style={styles.metaRow}>
              {item.failed ? (
                <Body size={10.5} color={colors.accentRamp[700]}>
                  Not sent
                </Body>
              ) : item.pending ? (
                <Body size={10.5} color={ink(0.35)}>
                  Sending…
                </Body>
              ) : (
                <Body size={10.5} color={ink(0.35)}>
                  {timeOf(item.createdAt)}
                </Body>
              )}
            </View>
          </View>
        </View>
      </View>
    );
  };

  return (
    <View style={styles.root}>
      <AppHeader kicker={`同居 · ${activeHousehold.name}`} title="Chat" />
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={insets.top + 60}
      >
        {loading ? (
          <View style={styles.centre}>
            <ActivityIndicator color={colors.accent} />
          </View>
        ) : error ? (
          <View style={styles.centre}>
            <Body size={13} color={colors.accentRamp[700]}>
              {error}
            </Body>
          </View>
        ) : messages.length === 0 ? (
          <View style={styles.centre}>
            <Heading size={22} color={ink(0.35)}>
              No messages yet
            </Heading>
            <Body size={13} color={ink(0.4)} style={{ marginTop: 6, textAlign: 'center' }}>
              Say hello to {activeHousehold.name}.
            </Body>
          </View>
        ) : (
          <FlatList
            ref={listRef}
            data={messages}
            keyExtractor={(m) => m.clientId}
            renderItem={renderItem}
            contentContainerStyle={styles.listContent}
            onContentSizeChange={() => listRef.current?.scrollToEnd({ animated: false })}
            ListHeaderComponent={
              hasMore ? (
                <Pressable onPress={loadOlder} style={styles.loadMore} disabled={loadingMore}>
                  {loadingMore ? (
                    <ActivityIndicator color={colors.accent} size="small" />
                  ) : (
                    <Kicker color={colors.accentRamp[700]}>Load earlier messages</Kicker>
                  )}
                </Pressable>
              ) : null
            }
          />
        )}

        <View style={[styles.composer, { paddingBottom: 10 }]}>
          <TextInput
            value={draft}
            onChangeText={setDraft}
            placeholder={`Message ${activeHousehold.name}…`}
            placeholderTextColor={ink(0.35)}
            style={styles.input}
            multiline
            onSubmitEditing={submit}
            returnKeyType="send"
          />
          <Pressable
            onPress={submit}
            disabled={!draft.trim()}
            style={[styles.sendBtn, !draft.trim() && { opacity: 0.4 }]}
          >
            <Feather name="arrow-up" size={18} color={colors.accent} />
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  centre: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 30 },
  listContent: { padding: 16, paddingBottom: 8 },
  dayRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginVertical: 14 },
  dayLine: { flex: 1, height: 1, backgroundColor: colors.divider },
  row: { flexDirection: 'row', alignItems: 'flex-end', gap: 8, marginTop: 4 },
  rowMine: { justifyContent: 'flex-end' },
  avatarSlot: { width: 28 },
  bubble: { maxWidth: '78%', borderRadius: radius.md, borderWidth: 1, paddingHorizontal: 13, paddingVertical: 9 },
  bubbleTheirs: { borderColor: colors.divider, backgroundColor: 'transparent' },
  bubbleMine: { borderColor: colors.accent, backgroundColor: 'rgba(182,130,53,0.07)' },
  metaRow: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 3 },
  loadMore: { alignItems: 'center', paddingVertical: 12 },
  composer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 8,
    paddingHorizontal: 14,
    paddingTop: 10,
    borderTopWidth: 1,
    borderTopColor: colors.divider,
    backgroundColor: colors.bg,
  },
  input: {
    flex: 1,
    minHeight: 44,
    maxHeight: 120,
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.pill,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 12,
    fontFamily: fonts.body,
    fontSize: 15,
    color: colors.text,
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
