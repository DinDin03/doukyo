// Lets the chat screen tell the tab badge that the thread has been read, without
// either one having to know about the other.
type Listener = () => void;
const listeners = new Set<Listener>();

export function notifyUnreadChanged() {
  listeners.forEach((l) => l());
}

export function onUnreadChanged(fn: Listener) {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
}
