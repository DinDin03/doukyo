import { ReactNode } from 'react';
import { Pressable, StyleProp, StyleSheet, View, ViewStyle } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { colors, fonts, ink, radius } from './theme';
import { Body, Kicker, Num } from './ui';

// A small outlined status label (the design draws status as a stroked pill).
export function Tag({ label, color = colors.accentRamp[700] }: { label: string; color?: string }) {
  return (
    <View style={[styles.tag, { borderColor: color }]}>
      <Kicker color={color}>{label}</Kicker>
    </View>
  );
}

// An initialled circle (members, expense payers, activity actors).
export function Avatar({ initial, size = 32 }: { initial: string; size?: number }) {
  return (
    <View style={[styles.avatar, { width: size, height: size, borderRadius: size / 2 }]}>
      <Num size={size * 0.42} color={colors.accentRamp[700]}>
        {initial}
      </Num>
    </View>
  );
}

// Round (chores) or square (shopping) checkbox that shows a tick when done.
export function Checkbox({
  done,
  onPress,
  shape = 'circle',
  size = 24,
}: {
  done: boolean;
  onPress: () => void;
  shape?: 'circle' | 'square';
  size?: number;
}) {
  return (
    <Pressable
      onPress={onPress}
      hitSlop={8}
      style={[
        styles.check,
        { width: size, height: size, borderRadius: shape === 'circle' ? size / 2 : radius.sm, borderColor: done ? colors.accent : colors.divider },
      ]}
    >
      {done ? <Feather name="check" size={size * 0.55} color={colors.accent} /> : null}
    </Pressable>
  );
}

// Segmented control — options in a bordered strip; the active one is accent-stroked.
export function Segmented<T extends string>({
  options,
  value,
  onChange,
  style,
}: {
  options: { value: T; label: string }[];
  value: T;
  onChange: (v: T) => void;
  style?: StyleProp<ViewStyle>;
}) {
  return (
    <View style={[styles.seg, style]}>
      {options.map((opt, i) => {
        const active = opt.value === value;
        return (
          <Pressable
            key={opt.value}
            onPress={() => onChange(opt.value)}
            style={[styles.segOpt, i > 0 && styles.segSep, active && styles.segActive]}
          >
            <Body size={13} color={active ? colors.accent : ink(0.7)} style={styles.segLabel}>
              {opt.label}
            </Body>
          </Pressable>
        );
      })}
    </View>
  );
}

// A −/+ stepper (servings, share units).
export function Stepper({ onDec, onInc, value }: { onDec: () => void; onInc: () => void; value?: ReactNode }) {
  return (
    <View style={styles.stepper}>
      <Pressable onPress={onDec} hitSlop={6} style={styles.stepBtn}>
        <Num size={17}>−</Num>
      </Pressable>
      {value != null ? (
        <Num size={14} style={styles.stepVal}>
          {value}
        </Num>
      ) : null}
      <Pressable onPress={onInc} hitSlop={6} style={styles.stepBtn}>
        <Num size={17}>+</Num>
      </Pressable>
    </View>
  );
}

// A left-accent-bordered pill "chip" used for categories / payer choices.
export function Chip({ label, active, onPress }: { label: string; active?: boolean; onPress?: () => void }) {
  const color = active ? colors.accent : ink(0.6);
  return (
    <Pressable onPress={onPress} style={[styles.chip, { borderColor: active ? colors.accent : colors.divider }]}>
      <Body size={12.5} color={color} style={styles.chipLabel}>
        {label}
      </Body>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  tag: { borderWidth: 1, borderRadius: radius.sm, paddingHorizontal: 7, paddingVertical: 3, alignSelf: 'flex-start' },
  avatar: {
    borderWidth: 1,
    borderColor: colors.divider,
    alignItems: 'center',
    justifyContent: 'center',
  },
  check: { borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  seg: { flexDirection: 'row', borderWidth: 1, borderColor: colors.divider, borderRadius: radius.md, overflow: 'hidden' },
  segOpt: { flex: 1, alignItems: 'center', paddingVertical: 10 },
  segSep: { borderLeftWidth: 1, borderLeftColor: colors.divider },
  segActive: { borderWidth: 1, borderColor: colors.accent, margin: -1, borderRadius: radius.md },
  segLabel: { fontFamily: fonts.heading },
  stepper: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  stepBtn: {
    width: 40,
    height: 40,
    borderWidth: 1,
    borderColor: colors.divider,
    borderRadius: radius.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepVal: { width: 40, textAlign: 'center' },
  chip: { borderWidth: 1, borderRadius: radius.sm, paddingHorizontal: 10, paddingVertical: 6, alignSelf: 'flex-start' },
  chipLabel: { fontFamily: fonts.heading },
});
