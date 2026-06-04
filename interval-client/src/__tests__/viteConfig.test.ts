import { describe, expect, it } from 'vitest';
import config from '../../vite.config.ts?raw';

describe('vite dev server config', () => {
  it('proxies API requests to the local backend in development', () => {
    expect(config).toContain("proxy: {");
    expect(config).toContain("'/api': {");
    expect(config).toContain("target: 'http://localhost:8088'");
    expect(config).toContain('changeOrigin: true');
  });
});
