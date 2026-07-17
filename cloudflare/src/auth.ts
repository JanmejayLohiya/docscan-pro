import { createRemoteJWKSet, jwtVerify } from 'jose'

// Firebase ID tokens are RS256-signed. Google publishes the public keys as a
// JWKS at the securetoken service account endpoint. We cache the key set per
// isolate via jose's remote JWKS helper.
const JWKS = createRemoteJWKSet(
  new URL('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'),
)

/**
 * Verifies a Firebase ID token and returns the user's UID (the `sub` claim).
 * Throws if the token is invalid, expired, or has the wrong issuer/audience.
 */
export async function verifyFirebaseToken(token: string, projectId: string): Promise<string> {
  const { payload } = await jwtVerify(token, JWKS, {
    issuer: `https://securetoken.google.com/${projectId}`,
    audience: projectId,
  })
  if (!payload.sub) throw new Error('token missing sub')
  return payload.sub
}
