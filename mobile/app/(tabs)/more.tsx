import { StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useAuth } from '../../src/auth/AuthContext';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Divider, Heading, Kicker, Num, Row, Screen } from '../../src/design/ui';
import { Avatar } from '../../src/design/widgets';
import { colors, ink, radius } from '../../src/design/theme';

const MEMBERS = [
  { initial: 'Y', name: 'You (Dineth)', note: 'Joined August', balance: '+$124', owed: true },
  { initial: 'S', name: 'Sam', note: 'Joined August', balance: '−$48', owed: false },
  { initial: 'J', name: 'Jules', note: 'Joined August', balance: '−$52', owed: false },
  { initial: 'R', name: 'Ravi', note: 'Joined September', balance: '+$24', owed: true },
];

const LINKS = ['Notifications', 'Receipts & exports', 'Help & feedback', 'Sign out'];

export default function MoreScreen() {
  const { signOut } = useAuth();
  return (
    <View style={styles.root}>
      <AppHeader kicker="同居 · Flat 7" title="More" />
      <Screen>
        <View style={styles.card}>
          <Kicker color={colors.accentRamp[700]}>Active household</Kicker>
          <Heading size={24} style={{ marginTop: 4 }}>
            Flat 7
          </Heading>
          <Body size={12} color={ink(0.55)} style={{ marginTop: 2 }}>
            4 members · created August
          </Body>
          <Divider style={{ marginVertical: 12 }} />
          <Row onPress={() => {}} style={styles.switchRow}>
            <Body size={13.5}>Switch household</Body>
            <Body size={12} color={ink(0.5)}>
              The Beach House
            </Body>
          </Row>
        </View>

        <Kicker color={ink(0.45)} style={styles.section}>
          Members · everyone equal
        </Kicker>
        {MEMBERS.map((m) => (
          <View key={m.name} style={styles.memberRow}>
            <Avatar initial={m.initial} size={34} />
            <View style={{ flex: 1, minWidth: 0 }}>
              <Body size={14.5}>{m.name}</Body>
              <Body size={11.5} color={ink(0.5)} style={{ marginTop: 1 }}>
                {m.note}
              </Body>
            </View>
            <Num size={13.5} color={m.owed ? colors.accentRamp[700] : ink(0.6)}>
              {m.balance}
            </Num>
          </View>
        ))}

        <View style={styles.inviteCard}>
          <Kicker color={ink(0.5)}>Invite code</Kicker>
          <Num size={30} style={styles.code}>
            7GQ2MX
          </Num>
          <Body size={12} color={colors.accentRamp[700]} style={styles.link}>
            Share the link
          </Body>
        </View>

        <View style={{ marginTop: 22 }}>
          {LINKS.map((l) => (
            <Row
              key={l}
              onPress={() => (l === 'Sign out' ? signOut() : undefined)}
              style={styles.linkRow}
            >
              <Body size={14} color={l === 'Sign out' ? colors.accentRamp[700] : colors.text}>
                {l}
              </Body>
              <Feather name="chevron-right" size={15} color={ink(0.35)} />
            </Row>
          ))}
        </View>
      </Screen>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  card: { borderWidth: 1, borderColor: colors.divider, borderRadius: radius.md, padding: 16 },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 0 },
  section: { marginTop: 22, marginBottom: 2 },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 13,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  inviteCard: {
    borderWidth: 1,
    borderColor: colors.divider,
    borderStyle: 'dashed',
    borderRadius: radius.md,
    padding: 16,
    marginTop: 22,
    alignItems: 'center',
  },
  code: { letterSpacing: 8, marginTop: 6 },
  link: { marginTop: 6, textDecorationLine: 'underline', fontFamily: 'CormorantGaramond_600SemiBold' },
  linkRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
});
