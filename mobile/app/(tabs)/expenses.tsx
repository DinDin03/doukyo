import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Button, Heading, Kicker, Num, Row } from '../../src/design/ui';
import { Avatar, Segmented } from '../../src/design/widgets';
import { colors, ink } from '../../src/design/theme';
import { useAuth } from '../../src/auth/AuthContext';
import { useHousehold } from '../../src/household/HouseholdContext';
import { formatCents, useExpenses, yourPosition } from '../../src/expense/useExpenses';

function dayLabel(iso: string) {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return 'Today';
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'long' });
}

export default function ExpensesScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const { activeHousehold } = useHousehold();
  const { expenses, loading, error, refresh } = useExpenses(activeHousehold?.id);
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = async () => {
    setRefreshing(true);
    await refresh();
    setRefreshing(false);
  };

  if (!activeHousehold) return null;

  // Group consecutive expenses under a date heading, newest first.
  let lastLabel = '';

  return (
    <View style={styles.root}>
      <AppHeader
        kicker={`同居 · ${activeHousehold.name}`}
        title="Expenses"
        onBell={() => router.push('/activity')}
      />
      <ScrollView
        contentContainerStyle={styles.scroll}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.accent} />}
      >
        <Segmented
          options={[
            { value: 'all', label: 'All expenses' },
            { value: 'balances', label: 'Balances' },
          ]}
          value="all"
          onChange={(v) => v === 'balances' && router.push('/balances')}
        />

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
        ) : expenses.length === 0 ? (
          <View style={styles.centre}>
            <Heading size={22} color={ink(0.35)}>
              No expenses yet
            </Heading>
            <Body size={13} color={ink(0.45)} style={{ marginTop: 6, textAlign: 'center' }}>
              Add the first one and everyone&apos;s share is worked out for you.
            </Body>
          </View>
        ) : (
          expenses.map((e) => {
            const position = yourPosition(e, user?.id);
            const label = dayLabel(e.createdAt);
            const showLabel = label !== lastLabel;
            lastLabel = label;
            return (
              <View key={e.id}>
                {showLabel ? (
                  <Kicker color={ink(0.45)} style={styles.groupLabel}>
                    {label}
                  </Kicker>
                ) : null}
                <Row onPress={() => {}} style={styles.expRow}>
                  <Avatar initial={e.paidBy.name.charAt(0).toUpperCase()} />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Body size={14.5} numberOfLines={1}>
                      {e.description}
                    </Body>
                    <Body size={11.5} color={ink(0.52)} style={{ marginTop: 1 }}>
                      {e.paidBy.id === user?.id ? 'You paid' : `${e.paidBy.name} paid`}
                    </Body>
                  </View>
                  <View style={{ alignItems: 'flex-end' }}>
                    <Num size={15}>{formatCents(e.amountCents)}</Num>
                    <Body
                      size={11}
                      color={position.owed ? colors.accentRamp[700] : ink(0.5)}
                      style={{ marginTop: 2 }}
                    >
                      {position.label}
                    </Body>
                  </View>
                </Row>
              </View>
            );
          })
        )}

        <Button
          label="Add an expense"
          block
          onPress={() => router.push('/add')}
          left={<Feather name="plus" size={16} color={colors.accent} />}
          style={{ marginTop: 24 }}
        />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  scroll: { paddingHorizontal: 22, paddingTop: 6, paddingBottom: 30 },
  centre: { alignItems: 'center', justifyContent: 'center', paddingVertical: 60 },
  groupLabel: { marginTop: 20, marginBottom: 2 },
  expRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
});
