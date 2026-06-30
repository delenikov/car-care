import '@testing-library/jest-dom/vitest';

// Node 25+ ships a built-in `localStorage` global (Web Storage API) that is exposed
// without a working backing store unless `--localstorage-file` is set. It shadows
// jsdom's own Storage implementation, leaving `localStorage.setItem`/`.clear` undefined
// in tests (https://github.com/vitest-dev/vitest/issues/8757). Installing our own
// deterministic in-memory Storage removes the dependency on Node's webstorage feature
// entirely, so tests behave the same on every Node version.
class MemoryStorage implements Storage {
  private readonly store = new Map<string, string>();

  get length(): number {
    return this.store.size;
  }

  clear(): void {
    this.store.clear();
  }

  getItem(key: string): string | null {
    return this.store.has(key) ? this.store.get(key)! : null;
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null;
  }

  removeItem(key: string): void {
    this.store.delete(key);
  }

  setItem(key: string, value: string): void {
    this.store.set(key, String(value));
  }
}

const installMemoryStorage = (key: 'localStorage' | 'sessionStorage') => {
  Object.defineProperty(globalThis, key, {
    value: new MemoryStorage(),
    configurable: true,
    writable: true
  });
};

installMemoryStorage('localStorage');
installMemoryStorage('sessionStorage');
