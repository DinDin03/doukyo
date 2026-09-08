import { StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Button, Card, Divider, Heading, Kicker, Num, Screen } from '../../src/design/ui';
import { Avatar } from '../../src/design/widgets';
import { colors, ink, radius } from '../../src/design/theme';
import { useAuth } from '../../src/auth/AuthContext';
import { useHousehold } from '../../src/household/HouseholdContext';
import { formatCents, useExpenses } from '../../src/expense/useExpenses';

export default function HomeScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const { activeHousehold } = useHousehold();
  const { balances, expenses } = useExpenses(activeHousehold?.id);

  if (!activeHousehold) return null;

  const firstName = user?.name?.split(' ')[0] ?? '';
  const members = activeHousehold.members;
  const isAlone = members.length === 1;
  const myNet = balances.find((b) => b.userId === user?.id)?.netCents ?? 0;

  return (
    <View style={styles.root}>
      <AppHeader kicker={`同居 · ${activeHousehold.name}`} title="Home" onBell={() => router.push('/activity')} />
      <Screen>
        <Heading size={26} weight="regular">
          {firstName ? `Hello, ${firstName}` : 'Hello'}
        </Heading>
        <Body size={13} color={ink(0.55)} style={{ marginTop: 2 }}>
          {members.length} {members.length === 1 ? 'person' : 'people'} in {activeHousehold.name}
        </Body>

        {/* Balances — no expenses backend yet, so this is an honest zero. */}
        <Card onPress={() => router.push('/balances')} style={{ marginTop: 20 }}>
          <View style={styles.rowBetween}>
            <Kicker>Your balance</Kicker>
            <Feather name="chevron-right" size={15} color={ink(0.4)} />
          </View>
          <Num
            size={52}
            weight="light"
            color={myNet > 0 ? colors.accentRamp[700] : colors.text}
            style={styles.bigMoney}
          >
            {myNet < 0 ? `−${formatCents(myNet)}` : formatCents(myNet)}
          </Num>
          <Divider style={{ marginVertical: 13 }} />
          <Body size={13} color={ink(0.55)}>
            {expenses.length === 0
              ? "No expenses yet. Add one and everyone's share is worked out for you."
              : myNet === 0
                ? `All square across ${expenses.length} expense${expenses.length === 1 ? '' : 's'}.`
                : myNet > 0
                  ? 'The house owes you this.'
                  : 'You owe the house this.'}
          </Body>
        </Card>

        {isAlone ? (
          <View style={styles.inviteCard}>
            <Kicker color={colors.accentRamp[700]}>Invite your flatmates</Kicker>
            <Num size={32} style={styles.code}>
              {activeHousehold.inviteCode}
            </Num>
            <Body size={12.5} color={ink(0.55)} style={{ textAlign: 'center', marginTop: 6 }}>
              Share this code so they can join {activeHousehold.name}.
            </Body>
          </View>
        ) : null}

        <Heading size={19} style={{ marginTop: 26, marginBottom: 10 }}>
          Flatmates
        </Heading>
        <View style={styles.list}>
          {members.map((m) => (
            <View key={m.id} style={styles.memberRow}>
              <Avatar initial={m.name.charAt(0).toUpperCase()} size={32} />
              <Body size={14.5} style={{ flex: 1 }}>
                {m.id === user?.id ? `${m.name} (you)` : m.name}
              </Body>
            </View>
          ))}
        </View>

        {/* Chores / meals / shopping have no backend yet — empty states, not fake rows. */}
        <Heading size={19} style={{ marginTop: 26, marginBottom: 10 }}>
          Today in the house
        </Heading>
        <View style={styles.emptyBlock}>
          <Body size={13} color={ink(0.5)}>
            No chores set up yet.
          </Body>
        </View>

        <View style={styles.twoCards}>
          <Card onPress={() => router.push('/meals')} style={styles.miniCard}>
            <Kicker color={colors.accentRamp[700]}>Tonight</Kicker>
            <Heading size={20} color={ink(0.4)} style={{ marginTop: 8 }}>
              Nothing planned
            </Heading>
          </Card>
          <Card onPress={() => router.push('/shopping')} style={styles.miniCard}>
            <Kicker color={colors.accentRamp[700]}>List</Kicker>
            <Heading size={20} color={ink(0.4)} style={{ marginTop: 8 }}>
              Empty
            </Heading>
          </Card>
        </View>

        <Button
          label="Add an expense"
          block
          onPress={() => router.push('/add')}
          left={<Feather name="plus" size={16} color={colors.accent} />}
          style={{ marginTop: 24 }}
        />
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  bigMoney: { marginTop: 8, letterSpacing: -1 },
  list: { borderTopWidth: 1, borderTopColor: colors.divider },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  emptyBlock: {
    borderWidth: 1,
    borderColor: colors.divider,
    borderStyle: 'dashed',
    borderRadius: radius.md,
    padding: 16,
    alignItems: 'center',
  },
  inviteCard: {
    borderWidth: 1,
    borderColor: colors.divider,
    borderStyle: 'dashed',
    borderRadius: radius.md,
    padding: 16,
    marginTop: 20,
    alignItems: 'center',
  },
  code: { letterSpacing: 8, marginTop: 6 },
  twoCards: { flexDirection: 'row', gap: 14, marginTop: 24 },
  miniCard: { flex: 1, minHeight: 110 },
});
