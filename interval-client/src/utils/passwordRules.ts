export const PASSWORD_RULE_TEXT = '至少 8 位，必须同时包含字母和数字';

export function getPasswordValidationMessage(password: string) {
  if (password.length < 8) return '密码至少需要 8 位';
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) return '密码必须同时包含字母和数字';
  return '';
}

export function getPasswordConfirmationMessage(password: string, confirmation: string) {
  if (password !== confirmation) return '两次输入的密码不一致';
  return '';
}
