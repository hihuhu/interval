import { beforeEach, describe, expect, it, vi } from 'vitest';
import { http } from '@/services/http';
import * as adminService from '@/services/adminService';

vi.mock('@/services/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockedHttp = vi.mocked(http);

describe('adminService', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('fetches dashboard from admin endpoint', async () => {
    mockedHttp.get.mockResolvedValue({ totalUsers: 3 });

    await adminService.fetchDashboard();

    expect(mockedHttp.get).toHaveBeenCalledWith('/api/admin/dashboard');
  });

  it('fetches users with keyword and status params', async () => {
    mockedHttp.get.mockResolvedValue([]);

    await adminService.fetchUsers({ keyword: 'alex', status: 'ACTIVE' });

    expect(mockedHttp.get).toHaveBeenCalledWith('/api/admin/users', {
      params: { keyword: 'alex', status: 'ACTIVE' },
    });
  });

  it('calls reset, disable and enable endpoints', async () => {
    mockedHttp.post.mockResolvedValue(undefined);

    await adminService.resetPassword(2, { mode: 'AUTO', temporaryPassword: null });
    await adminService.disableUser(2);
    await adminService.enableUser(2);

    expect(mockedHttp.post).toHaveBeenCalledWith('/api/admin/users/2/reset-password', {
      mode: 'AUTO',
      temporaryPassword: null,
    });
    expect(mockedHttp.post).toHaveBeenCalledWith('/api/admin/users/2/disable');
    expect(mockedHttp.post).toHaveBeenCalledWith('/api/admin/users/2/enable');
  });
});
