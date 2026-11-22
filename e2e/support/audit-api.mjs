// Only the fixed, isolated audit stack. No request or exception prints credentials.
export const base = 'http://127.0.0.1:4301';
export async function call(path, body, token, method = 'POST', expected = 200, extra = {}) {
  if (!path.startsWith('/api/')) throw new Error('Ruta audit no válida.');
  const response = await fetch(base + path, {
    method, redirect: 'manual', signal: AbortSignal.timeout(30_000),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}), ...extra },
    ...(body !== null ? { body: JSON.stringify(body) } : {}),
  });
  if (response.status !== expected) throw new Error(method + ' ' + path + ': HTTP ' + response.status + ', esperado ' + expected);
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
export function organizationProfile(name, email, suffix) {
  return {
    legalName: name, country: 'BO', region: 'Santa Cruz',
    address: 'Dirección ficticia para pruebas', phone: '70000000', contactEmail: email,
    website: '', domain: '', taxId: suffix, educationLicense: 'TEST-' + suffix,
    ministryRegistration: '', accreditation: '', institutionType: 'INSTITUTO', educationLevels: [],
    representative: { fullName: 'Representante ficticio', documentType: 'CI', documentNumber: suffix,
      position: 'Administrador', email, phone: '70000001' },
  };
}
export async function verifyEmail(email) {
  if (!/@[^@]+\.test$/.test(email)) throw new Error('Solo se permiten destinatarios ficticios .test.');
  const mailpit = 'http://127.0.0.1:18125';
  const deadline = Date.now() + 25_000;
  while (Date.now() < deadline) {
    const list = await fetch(mailpit + '/api/v1/messages?limit=200', { signal: AbortSignal.timeout(5000), redirect: 'error' });
    if (!list.ok) throw new Error('Mailpit audit no está disponible.');
    const message = (await list.json()).messages?.find(item =>
      item.Subject === 'Verifica tu correo' && item.To?.some(to => to.Address === email));
    if (message) {
      const detail = await fetch(mailpit + '/api/v1/message/' + encodeURIComponent(message.ID),
        { signal: AbortSignal.timeout(5000), redirect: 'error' });
      if (!detail.ok) throw new Error('No se pudo leer el correo ficticio.');
      const token = (await detail.json()).Text?.match(/token=([A-Za-z0-9_-]+)/)?.[1];
      if (token) return call('/api/auth/student/verify-email', { token }, null, 'POST', 204);
    }
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw new Error('No llegó la confirmación al buzón audit dentro del plazo.');
}
