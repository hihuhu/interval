export interface ApiResponse<T> {
  result: 'SUCCESS' | 'ERROR';
  message: string;
  data: T | null;
}
