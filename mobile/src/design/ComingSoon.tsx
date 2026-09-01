import { StyleSheet, View } from 'react-native';
import { AppHeader } from './AppHeader';
import { Body, Heading } from './ui';
import { colors, ink } from './theme';

// Placeholder for screens whose backend/design isn't wired yet — keeps the app
// visually coherent in the Classical style until the real screen lands.
export function ComingSoon({ kicker, title }: { kicker: string; title: string }) {
  return (
    <View style={styles.root}>
      <AppHeader kicker={kicker} title={title} />
      <View style={styles.center}>
        <Heading size={26} weight="regular" color={ink(0.32)}>
          Coming soon
        </Heading>
        <Body size={13} color={ink(0.4)} style={{ marginTop: 6 }}>
          Designed — wiring in progress
        </Body>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
