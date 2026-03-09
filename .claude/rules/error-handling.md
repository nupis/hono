# Error Handling Guidelines

## Overview

This file covers error handling principles that apply across all languages and technologies. Consistent error handling improves debuggability, user experience, and system reliability.

## Principles

### Principle 1: Explicit Error Boundaries

**Why**: Errors should be caught and handled at defined boundaries, not scattered throughout the code.

**Rule**: Define explicit error boundaries at:
- System entry points (API endpoints, CLI commands)
- External service calls (databases, APIs, file systems)
- User input validation points

### Principle 2: Fail Fast, Fail Loud

**Why**: Silent failures mask problems and make debugging difficult.

**Rule**:
- Validate inputs early and reject invalid data immediately
- Never swallow exceptions without logging
- Propagate errors to appropriate handlers rather than hiding them

### Principle 3: Structured Error Information

**Why**: Consistent error structure enables automated processing and better debugging.

**Rule**: Errors must include:
- Error code or type for programmatic handling
- Human-readable message for display
- Context information for debugging (without sensitive data)
- Stack trace or correlation ID for tracing

### Principle 4: Graceful Degradation

**Why**: Partial failures should not cause complete system failure.

**Rule**:
- Implement fallback behaviors where appropriate
- Isolate failures to affected components
- Maintain core functionality when non-critical features fail

### Principle 5: User-Friendly Error Messages

**Why**: Users need to understand what went wrong and how to fix it.

**Rule**:
- Provide actionable guidance when possible
- Hide technical details from end users
- Log detailed information for developers separately

### Principle 6: Retry with Backoff

**Why**: Transient failures often resolve themselves with proper retry logic.

**Rule**: For recoverable external failures:
- Implement exponential backoff
- Set maximum retry limits
- Log retry attempts
- Distinguish transient from permanent failures

## Examples

**Structured Error Response** (language-agnostic pattern):
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input provided",
    "details": [
      { "field": "email", "issue": "Invalid format" }
    ],
    "correlationId": "abc-123"
  }
}
```

**Language-Specific Examples**:

- TypeScript:
  ```typescript
  try {
    const result = await externalService.call()
    return result
  } catch (error) {
    logger.error('External service failed', { error, correlationId })
    throw new ServiceError('Service unavailable', 'SERVICE_ERROR', error)
  }
  ```

- C#:
  ```csharp
  try {
    return await externalService.CallAsync(cancellationToken);
  } catch (HttpRequestException ex) {
    logger.LogError(ex, "External service failed for {CorrelationId}", correlationId);
    throw new ServiceException("Service unavailable", ex);
  }
  ```

- Java:
  ```java
  try {
    return externalService.call();
  } catch (IOException ex) {
    logger.error("External service failed", ex);
    throw new ServiceException("Service unavailable", ex);
  }
  ```

## Checklist

- [ ] Error boundaries defined at system entry points
- [ ] No silent exception swallowing
- [ ] Errors include sufficient context for debugging
- [ ] Sensitive data excluded from error messages
- [ ] Retry logic implemented for transient failures
- [ ] User-facing messages are actionable
