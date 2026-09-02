import { StyleSheet, View } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { useAuth } from '../../src/auth/AuthContext';
import { useHousehold } from '../../src/household/HouseholdContext';
import { AppHeader } from '../../src/design/AppHeader';
import { Body, Divider, Heading, Kicker, Num, Row, Screen } from '../../src/design/ui';
import { Avatar } from '../../src/design/widgets';
import { colors, ink, radius } from '../../src/design/theme';

const LINKS = ['Notifications', 'Receipts & exports', 'Help & feedback', 'Sign out'];

export default function MoreScreen() {
  const { user, signOut } = useAuth();
  const { activeHousehold, households } = useHousehold();

  if (!activeHousehold) return null; // the root gate guarantees this, but keeps TS happy

  const otherHousehold = households.find((h) => h.id !== activeHousehold.id);
  const memberCount = activeHousehold.members.length;
  const createdLabel = new Date(activeHousehold.createdAt).toLocaleDateString(undefined, {
    month: 'long',
    year: 'numeric',
  });

  return (
    <View style={styles.root}>
      <AppHeader kicker={`同居 · ${activeHousehold.name}`} title="More" />
      <Screen>
        <View style={styles.card}>
          <Kicker color={colors.accentRamp[700]}>Active household</Kicker>
          <Heading size={24} style={{ marginTop: 4 }}>
            {activeHousehold.name}
          </Heading>
          <Body size={12} color={ink(0.55)} style={{ marginTop: 2 }}>
            {memberCount} member{memberCount === 1 ? '' : 's'} · created {createdLabel}
          </Body>
          {otherHousehold ? (
            <>
              <Divider style={{ marginVertical: 12 }} />
              <Row onPress={() => {}} style={styles.switchRow}>
                <Body size={13.5}>Switch household</Body>
                <Body size={12} color={ink(0.5)}>
                  {otherHousehold.name}
                </Body>
              </Row>
            </>
          ) : null}
        </View>

        <Kicker color={ink(0.45)} style={styles.section}>
          Members · everyone equal
        </Kicker>
        {activeHousehold.members.map((m) => (
          <View key={m.id} style={styles.memberRow}>
            <Avatar initial={m.name.charAt(0).toUpperCase()} size={34} />
            <View style={{ flex: 1, minWidth: 0 }}>
              <Body size={14.5}>{m.id === user?.id ? `${m.name} (you)` : m.name}</Body>
            </View>
          </View>
        ))}

        <View style={styles.inviteCard}>
          <Kicker color={ink(0.5)}>Invite code</Kicker>
          <Num size={30} style={styles.code}>
            {activeHousehold.inviteCode}
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
