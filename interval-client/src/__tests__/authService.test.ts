import { beforeEach, describe, expect, it, vi } from 'vitest';
import { http } from '@/services/http';
import * as authService from '@/services/authService';

vi.mock('@/services/http', () => ({
  http: {
    post: vi.fn(),
  },
}));

const mockedHttp = vi.mocked(http);

describe('authService', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('posts change password payload to auth endpoint', async () => {
    mockedHttp.post.mockResolvedValue(undefined);

    await authService.changePassword({
      currentPassword: 'OldPassword123',
      newPassword: 'NewPassword123',
    });

    expect(mockedHttp.post).toHaveBeenCalledWith('/api/auth/change-password', {
      currentPassword: 'OldPassword123',
      newPassword: 'NewPassword123',
    });
  });
});
