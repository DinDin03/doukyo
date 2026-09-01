import { ReactNode } from 'react';
import {
  Pressable,
  ScrollView,
  StyleProp,
  StyleSheet,
  Text,
  TextInput,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';
import { colors, fonts, ink, radius, space } from './theme';

const headingFamily = {
  light: fonts.displayLight,
  regular: fonts.displayRegular,
  medium: fonts.displayMedium,
  semi: fonts.heading,
} as const;

// ── Text primitives ─────────────────────────────────────────────────────────

// Small uppercase label (the design's .dk-kick): Lora, letterspaced, tabular.
export function Kicker({
  children,
  color = ink(0.55),
  style,
}: {
  children: ReactNode;
  color?: string;
  style?: StyleProp<TextStyle>;
}) {
  return <Text style={[styles.kicker, { color }, style]}>{children}</Text>;
}

// Cormorant Garamond heading. Bigger text sets lighter (per the design).
export function Heading({
  children,
  size = 24,
  weight = 'regular',
  color = colors.text,
  numberOfLines,
  style,
}: {
  children: ReactNode;
  size?: number;
  weight?: keyof typeof headingFamily;
  color?: string;
  numberOfLines?: number;
  style?: StyleProp<TextStyle>;
}) {
  return (
    <Text
      numberOfLines={numberOfLines}
      style={[
        { fontFamily: headingFamily[weight], fontSize: size, color, lineHeight: size * 1.14, letterSpacing: -0.2 },
        style,
      ]}
    >
      {children}
    </Text>
  );
}

// Lora body text.
export function Body({
  children,
  size = 14,
  color = colors.text,
  weight = 'regular',
  numberOfLines,
  style,
}: {
  children: ReactNode;
  size?: number;
  color?: string;
  weight?: 'regular' | 'medium' | 'semi';
  numberOfLines?: number;
  style?: StyleProp<TextStyle>;
}) {
  const fam = weight === 'semi' ? fonts.bodySemi : weight === 'medium' ? fonts.bodyMedium : fonts.body;
  return (
    <Text numberOfLines={numberOfLines} style={[{ fontFamily: fam, fontSize: size, color, lineHeight: size * 1.5 }, style]}>
      {children}
    </Text>
  );
}

// Tabular numerals in the heading face (the design's .dk-num) — for money/figures.
export function Num({
  children,
  size = 16,
  color = colors.text,
  weight = 'regular',
  style,
}: {
  children: ReactNode;
  size?: number;
  color?: string;
  weight?: keyof typeof headingFamily;
  style?: StyleProp<TextStyle>;
}) {
  return (
    <Text
      style={[
        { fontFamily: headingFamily[weight], fontSize: size, color, lineHeight: size * 1.08 },
        styles.tnum,
        style,
      ]}
    >
      {children}
    </Text>
  );
}

// ── Layout ──────────────────────────────────────────────────────────────────

export function Divider({ style }: { style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.divider, style]} />;
}

// A themed screen container (scrolling by default), painted on the Classical ground.
export function Screen({
  children,
  scroll = true,
  style,
}: {
  children: ReactNode;
  scroll?: boolean;
  style?: StyleProp<ViewStyle>;
}) {
  if (scroll) {
    return (
      <ScrollView
        style={styles.screen}
        contentContainerStyle={[styles.screenContent, style]}
        showsVerticalScrollIndicator={false}
      >
        {children}
      </ScrollView>
    );
  }
  return <View style={[styles.screen, styles.screenContent, style]}>{children}</View>;
}

// Bordered, unfilled surface (color is stroke, never fill).
export function Card({
  children,
  onPress,
  style,
}: {
  children: ReactNode;
  onPress?: () => void;
  style?: StyleProp<ViewStyle>;
}) {
  if (onPress) {
    return (
      <Pressable onPress={onPress} style={({ pressed }) => [styles.card, pressed && { opacity: 0.7 }, style]}>
        {children}
      </Pressable>
    );
  }
  return <View style={[styles.card, style]}>{children}</View>;
}

// A tappable list row (dims on press).
export function Row({
  children,
  onPress,
  style,
}: {
  children: ReactNode;
  onPress?: () => void;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.row, pressed && onPress ? { opacity: 0.6 } : null, style]}>
      {children}
    </Pressable>
  );
}

// ── Button (outlined, pill — never a solid fill) ────────────────────────────

export function Button({
  label,
  onPress,
  variant = 'primary',
  block,
  left,
  style,
}: {
  label: string;
  onPress?: () => void;
  variant?: 'primary' | 'secondary';
  block?: boolean;
  left?: ReactNode;
  style?: StyleProp<ViewStyle>;
}) {
  const borderColor = variant === 'primary' ? colors.accent : colors.divider;
  const labelColor = variant === 'primary' ? colors.accent : colors.text;
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.btn,
        { borderColor },
        block && styles.btnBlock,
        pressed && { backgroundColor: variant === 'primary' ? 'rgba(182,130,53,0.12)' : 'rgba(32,31,29,0.06)' },
        style,
      ]}
    >
      {left}
      <Text style={[styles.btnLabel, { color: labelColor }]}>{label}</Text>
    </Pressable>
  );
}

// ── Field (label + text input) ──────────────────────────────────────────────

export function Field({
  label,
  value,
  onChangeText,
  placeholder,
  autoCapitalize,
  keyboardType,
  secureTextEntry,
  style,
  inputStyle,
}: {
  label?: string;
  value?: string;
  onChangeText?: (t: string) => void;
  placeholder?: string;
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  keyboardType?: 'default' | 'email-address' | 'numeric';
  secureTextEntry?: boolean;
  style?: StyleProp<ViewStyle>;
  inputStyle?: StyleProp<TextStyle>;
}) {
  return (
    <View style={style}>
      {label ? <Text style={styles.fieldLabel}>{label}</Text> : null}
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={ink(0.35)}
        autoCapitalize={autoCapitalize}
        keyboardType={keyboardType}
        secureTextEntry={secureTextEntry}
        style={[styles.input, inputStyle]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  kicker: {
    fontFamily: fonts.body,
    fontSize: 9.5,
    letterSpacing: 1.5,
    textTransform: 'uppercase',
    fontVariant: ['tabular-nums'],
  },
  tnum: { fontVariant: ['tabular-nums'] },
  divider: { height: 1, backgroundColor: colors.divider },
  screen: { flex: 1, backgroundColor: colors.bg },
  screenContent: { padding: space.lg, paddingBottom: space.xxl },
  card: {
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.md,
    padding: space.md,
    backgroundColor: 'transparent',
  },
  row: { paddingVertical: space.md },
  btn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    borderWidth: 1,
    borderRadius: radius.pill,
    paddingVertical: 13,
    paddingHorizontal: space.xl,
  },
  btnBlock: { alignSelf: 'stretch' },
  btnLabel: { fontFamily: fonts.heading, fontSize: 15 },
  fieldLabel: {
    fontFamily: fonts.body,
    fontSize: 12,
    color: ink(0.7),
    marginBottom: 6,
  },
  input: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.pill,
    paddingHorizontal: 16,
    fontFamily: fonts.body,
    fontSize: 15,
    color: colors.text,
  },
});
