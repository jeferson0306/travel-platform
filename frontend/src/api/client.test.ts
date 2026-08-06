import { describe, expect, it } from 'vitest';
import { ApiError, NetworkError, friendlyErrorMessage } from './client';

describe('friendlyErrorMessage', () => {
  it('tells the user to check their connection on a network failure', () => {
    expect(friendlyErrorMessage(new NetworkError())).toBe(
      "Can't reach the server. Check your connection and try again.",
    );
  });

  it('tells the user their session expired on NO_TOKEN', () => {
    const err = new ApiError(401, 'NO_TOKEN', 'Authentication required');
    expect(friendlyErrorMessage(err)).toBe('Your session expired - please log in again.');
  });

  it('does not treat a login failure (INVALID_CREDENTIALS) as a session expiry', () => {
    const err = new ApiError(401, 'INVALID_CREDENTIALS', 'Wrong email or password');
    expect(friendlyErrorMessage(err)).toBe('Wrong email or password');
  });

  it('passes through a validation message written for humans', () => {
    const err = new ApiError(400, 'VALIDATION_ERROR', 'currency must be a 3-letter ISO 4217 code');
    expect(friendlyErrorMessage(err)).toBe('currency must be a 3-letter ISO 4217 code');
  });

  it('gives a generic message for server errors, not the raw backend text', () => {
    const err = new ApiError(500, 'UNKNOWN', 'NullPointerException at line 42');
    expect(friendlyErrorMessage(err)).toBe(
      'Something went wrong on our end. Please try again in a moment.',
    );
  });

  it('falls back to a generic message for anything unexpected', () => {
    expect(friendlyErrorMessage(new TypeError('boom'))).toBe(
      'Something unexpected happened. Please try again.',
    );
  });
});
