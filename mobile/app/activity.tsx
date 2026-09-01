import { StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AppHeader } from '../src/design/AppHeader';
import { Body, Kicker, Screen } from '../src/design/ui';
import { Avatar } from '../src/design/widgets';
import { colors, ink } from '../src/design/theme';

const ACTIVITY = [
  { id: '1', initial: 'R', text: 'Ravi added Groceries — Woolworths, $84.20, split four ways.', when: '2h ago' },
  { id: '2', initial: 'S', text: 'Sam marked Milk as bought.', when: '4h ago' },
  { id: '3', initial: 'Y', text: 'You settled up $24.00 with Ravi.', when: 'Yesterday' },
  { id: '4', initial: 'J', text: 'Jules did Vacuum the lounge.', when: 'Yesterday' },
  { id: '5', initial: 'R', text: 'Ravi planned Miso salmon for Thursday.', when: 'Tue' },
  { id: '6', initial: 'S', text: 'Sam added Internet, $79.00, to expenses.', when: 'Mon' },
  { id: '7', initial: 'Y', text: 'You created the household Flat 7.', when: 'Last week' },
];

export default function ActivityScreen() {
  const router = useRouter();
  return (
    <View style={styles.root}>
      <AppHeader kicker="同居 · Flat 7" title="Activity" onBack={() => router.back()} backLabel="Back" />
      <Screen>
        {ACTIVITY.map((a) => (
          <View key={a.id} style={styles.row}>
            <Avatar initial={a.initial} size={30} />
            <View style={{ flex: 1, minWidth: 0 }}>
              <Body size={14}>{a.text}</Body>
              <Kicker color={ink(0.42)} style={{ marginTop: 3 }}>
                {a.when}
              </Kicker>
            </View>
          </View>
        ))}
        <Body size={11.5} color={ink(0.4)} style={styles.footer}>
          Loading earlier activity…
        </Body>
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  row: { flexDirection: 'row', gap: 13, paddingVertical: 13, borderBottomWidth: 1, borderBottomColor: colors.divider },
  footer: { textAlign: 'center', paddingVertical: 18 },
});
