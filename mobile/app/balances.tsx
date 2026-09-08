import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppHeader } from '../src/design/AppHeader';
import { Body, Heading, Kicker, Num, Screen } from '../src/design/ui';
import { colors, ink, radius } from '../src/design/theme';
import { useAuth } from '../src/auth/AuthContext';
import { useHousehold } from '../src/household/HouseholdContext';
import { formatCents, useExpenses } from '../src/expense/useExpenses';

export default function BalancesScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const { activeHousehold } = useHousehold();
  const { expenses, balances, loading, settleShare } = useExpenses(activeHousehold?.id);
  const [settling, setSettling] = useState<string | null>(null);

  if (!activeHousehold) return null;

  const mine = balances.find((b) => b.userId === user?.id);
  const net = mine?.netCents ?? 0;

  // Everything you personally still owe, one row per expense — settling is
  // per-share, so this is the list of things that can actually be settled.
  const youOwe = expenses
    .map((e) => ({ expense: e, share: e.shares.find((s) => s.user.id === user?.id && !s.isPaid) }))
    .filter((row) => row.share && row.expense.paidBy.id !== user?.id);

  const onSettle = async (shareId: string) => {
    setSettling(shareId);
    try {
      await settleShare(shareId);
    } finally {
      setSettling(null);
    }
  };

  return (
    <View style={styles.root}>
      <AppHeader kicker="Expenses" title="Balances" onBack={() => router.back()} backLabel="Back" />
      <Screen>
        {loading ? (
          <View style={styles.centre}>
            <ActivityIndicator color={colors.accent} />
          </View>
        ) : (
          <>
            <View style={styles.hero}>
              <Kicker color={ink(0.5)}>Your net position</Kicker>
              <Num
                size={56}
                weight="light"
                color={net > 0 ? colors.accentRamp[700] : colors.text}
                style={{ marginTop: 6, letterSpacing: -1 }}
              >
                {net < 0 ? `−${formatCents(net)}` : formatCents(net)}
              </Num>
              <Body size={12.5} color={ink(0.55)} style={{ marginTop: 4 }}>
                {net === 0
                  ? 'All square'
                  : net > 0
                    ? 'the house owes you'
                    : 'you owe the house'}
              </Body>
            </View>

            <Kicker color={ink(0.45)} style={styles.section}>
              Everyone&apos;s position
            </Kicker>
            {balances.map((b) => (
              <View key={b.userId} style={styles.row}>
                <View style={{ flex: 1, minWidth: 0 }}>
                  <Heading size={17}>{b.userId === user?.id ? `${b.userName} (you)` : b.userName}</Heading>
                  <Body size={11.5} color={ink(0.52)} style={{ marginTop: 2 }}>
                    {b.netCents === 0 ? 'Settled up' : b.netCents > 0 ? 'is owed' : 'owes the house'}
                  </Body>
                </View>
                <Num size={18} color={b.netCents > 0 ? colors.accentRamp[700] : ink(0.6)}>
                  {b.netCents < 0 ? `−${formatCents(b.netCents)}` : formatCents(b.netCents)}
                </Num>
              </View>
            ))}

            {youOwe.length > 0 ? (
              <>
                <Kicker color={ink(0.45)} style={styles.section}>
                  What you owe
                </Kicker>
                {youOwe.map(({ expense, share }) => (
                  <View key={share!.id} style={styles.row}>
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Heading size={16}>{expense.description}</Heading>
                      <Body size={11.5} color={ink(0.52)} style={{ marginTop: 2 }}>
                        {expense.paidBy.name} paid
                      </Body>
                    </View>
                    <Num size={16} style={{ marginRight: 12 }}>
                      {formatCents(share!.amountCents)}
                    </Num>
                    <Pressable
                      onPress={() => onSettle(share!.id)}
                      disabled={settling === share!.id}
                      style={styles.settle}
                    >
                      {settling === share!.id ? (
                        <ActivityIndicator size="small" color={colors.accent} />
                      ) : (
                        <Body size={12} color={colors.accent}>
                          Settle
                        </Body>
                      )}
                    </Pressable>
                  </View>
                ))}
              </>
            ) : null}

            <Body size={11.5} color={ink(0.45)} style={styles.foot}>
              Balances are worked out from unsettled shares. Settling never deletes an
              expense — the record stays.
            </Body>
          </>
        )}
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  centre: { alignItems: 'center', justifyContent: 'center', paddingVertical: 60 },
  hero: { alignItems: 'center', paddingVertical: 18 },
  section: { marginTop: 20, marginBottom: 4 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  settle: {
    minWidth: 62,
    alignItems: 'center',
    paddingVertical: 7,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: colors.accent,
    borderRadius: radius.pill,
  },
  foot: { textAlign: 'center', marginTop: 24 },
});
