/** Decodes the JWT payload client-side to read the `sub` claim (the user id) - no verification,
 * this is only ever used to read a claim from a token the app itself just received from
 * identity-service over HTTPS. Never use this pattern to trust a token from an untrusted source. */
export function decodeUserId(token: string): string {
  const payload = token.split('.')[1];
  const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  const claims = JSON.parse(json) as { sub: string };
  return claims.sub;
}
