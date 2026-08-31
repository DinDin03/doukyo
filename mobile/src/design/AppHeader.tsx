import { Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather } from '@expo/vector-icons';
import { colors, ink, radius, space } from './theme';
import { Heading, Kicker } from './ui';

// The Classical screen header: optional back row, a small accent kicker over a
// large serif title, and an optional outlined notification bell.
export function AppHeader({
  kicker,
  title,
  onBell,
  onBack,
  backLabel,
}: {
  kicker: string;
  title: string;
  onBell?: () => void;
  onBack?: () => void;
  backLabel?: string;
}) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.header, { paddingTop: insets.top + 10 }]}>
      {onBack ? (
        <Pressable onPress={onBack} style={styles.backRow} hitSlop={8}>
          <Feather name="arrow-left" size={18} color={colors.text} />
          {backLabel ? <Kicker color={ink(0.5)}>{backLabel}</Kicker> : null}
        </Pressable>
      ) : null}
      <View style={styles.titleRow}>
        <View style={styles.titleWrap}>
          <Kicker color={colors.accentRamp[700]}>{kicker}</Kicker>
          <Heading size={29} weight="regular" numberOfLines={1} style={styles.title}>
            {title}
          </Heading>
        </View>
        {onBell ? (
          <Pressable onPress={onBell} style={styles.bell} hitSlop={8}>
            <Feather name="bell" size={18} color={colors.text} />
            <View style={styles.dot} />
          </Pressable>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    paddingHorizontal: space.lg,
    paddingBottom: 12,
    backgroundColor: colors.bg,
    borderBottomWidth: 1,
    borderBottomColor: colors.divider,
  },
  backRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 6, marginLeft: -2 },
  titleRow: { flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 },
  titleWrap: { flex: 1, minWidth: 0 },
  title: { marginTop: 3 },
  bell: {
    width: 38,
    height: 38,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.divider,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dot: { position: 'absolute', top: 8, right: 9, width: 5, height: 5, borderRadius: 3, backgroundColor: colors.accent },
});
