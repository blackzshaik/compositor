import '@testing-library/jest-dom';

if (typeof window !== 'undefined') {
  class MockResizeObserver {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  }
  window.ResizeObserver = MockResizeObserver;
  global.ResizeObserver = MockResizeObserver;
}
