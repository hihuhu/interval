import { describe, expect, it } from 'vitest';
import indexHtml from '../../index.html?raw';

describe('index.html', () => {
  it('declares the Interval favicon', () => {
    expect(indexHtml).toContain('<link rel="icon" type="image/svg+xml" href="/favicon.svg" />');
  });
});
