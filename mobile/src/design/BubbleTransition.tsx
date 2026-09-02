import { useCallback, useRef, useState } from 'react';
import { Animated, Easing, StyleSheet, useWindowDimensions } from 'react-native';
import { colors } from './theme';

// A circular hole that opens outward from the centre, revealing the screen beneath.
// Use `play(swap)`: the screen is covered, `swap` runs while it's hidden, then the
// hole expands past the edges. Starts covered so app launch never flashes content.
export function useBubbleTransition() {
  const { width, height } = useWindowDimensions();
  const maxHole = (Math.sqrt(width * width + height * height) / 2) * 1.05;
  const ring = maxHole * 2; // thick enough that the fill always reaches the corners

  const progress = useRef(new Animated.Value(0)).current; // 0 = covered, 1 = open
  const [visible, setVisible] = useState(true);

  const play = useCallback(
    (swap: () => void) => {
      progress.setValue(0);
      setVisible(true);
      swap();
      requestAnimationFrame(() => {
        Animated.timing(progress, {
          toValue: 1,
          duration: 950,
          easing: Easing.inOut(Easing.cubic),
          // Layout props (width/borderRadius/borderWidth) can't run on the native
          // driver — a circular hole needs them, since RN has no mask primitive.
          useNativeDriver: false,
        }).start(({ finished }) => {
          if (finished) setVisible(false);
        });
      });
    },
    [progress],
  );

  // The "bubble" is a ring: a transparent centre with a very thick coloured border.
  // Growing the hole pushes that border off-screen, so the reveal opens outward.
  const size = progress.interpolate({
    inputRange: [0, 1],
    outputRange: [2 * ring, 2 * (maxHole + ring)],
  });
  const radius = progress.interpolate({
    inputRange: [0, 1],
    outputRange: [ring, maxHole + ring],
  });

  const bubble = visible ? (
    <Animated.View pointerEvents="none" style={styles.overlay}>
      <Animated.View
        style={{
          width: size,
          height: size,
          borderRadius: radius,
          borderWidth: ring,
          borderColor: colors.transition,
          backgroundColor: 'transparent',
        }}
      />
    </Animated.View>
  ) : null;

  return { bubble, play };
}

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 50,
    overflow: 'hidden',
  },
});
