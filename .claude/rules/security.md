# Security Guidelines

## Overview

This file covers security practices that apply across all languages and technologies. Security is a baseline requirement, not an enhancement.

## Principles

### Principle 1: Secret Management

**Why**: Exposed secrets lead to data breaches and system compromise.

**Rule**:
- NEVER commit secrets to source control
- Use environment variables or secure secret managers
- Apply least-privilege principles for credentials and access
- Rotate secrets regularly

**Examples**:
- TypeScript:
  ```typescript
  // NEVER: Hardcoded secrets
  const apiKey = "sk-proj-xxxxx"

  // ALWAYS: Environment variables
  const apiKey = process.env.API_KEY
  if (!apiKey) {
    throw new Error('API_KEY not configured')
  }
  ```
- C#:
  ```csharp
  // ALWAYS: Configuration
  var apiKey = configuration["ApiKey"]
    ?? throw new InvalidOperationException("API key not configured");
  ```

### Principle 2: Input Validation

**Why**: Unvalidated input is the source of most security vulnerabilities.

**Rule**:
- Validate all external input at system boundaries
- Never trust user input, API responses, or file content
- Use allowlists over denylists
- Sanitize data before use

### Principle 3: OWASP Top 10

**Why**: These are the most critical security risks to web applications.

**Rule**: Mitigate all OWASP Top 10 risks:
1. Injection (SQL, NoSQL, OS, LDAP)
2. Broken Authentication
3. Sensitive Data Exposure
4. XML External Entities (XXE)
5. Broken Access Control
6. Security Misconfiguration
7. Cross-Site Scripting (XSS)
8. Insecure Deserialization
9. Using Components with Known Vulnerabilities
10. Insufficient Logging & Monitoring

### Principle 4: Data Protection

**Why**: Sensitive data requires special handling to protect privacy.

**Rule**:
- Never log sensitive data (passwords, API keys, PII)
- Encrypt sensitive data at rest and in transit
- Minimize data collection and retention
- Mask sensitive data in error messages

### Principle 5: Authentication & Authorization

**Why**: Proper access control prevents unauthorized access.

**Rule**:
- Implement proper authentication for all endpoints
- Verify authorization on every request
- Use established auth frameworks (OAuth2, OIDC)
- Implement rate limiting on all endpoints

### Principle 6: Cryptography

**Why**: Weak crypto provides false sense of security.

**Rule**:
- Use platform-provided, well-reviewed crypto APIs only
- Never implement custom cryptography
- Use current standards (AES-256, SHA-256+, etc.)
- Manage keys securely

## Security Checklist

Before ANY commit:
- [ ] No hardcoded secrets (API keys, passwords, tokens)
- [ ] All user inputs validated and sanitized
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (sanitized HTML output)
- [ ] CSRF protection enabled
- [ ] Authentication/authorization verified
- [ ] Rate limiting on all endpoints
- [ ] Error messages don't leak sensitive data
- [ ] No PII in logs or telemetry
- [ ] Dependencies checked for vulnerabilities

## Security Response Protocol

If a security issue is found:
1. **STOP** immediately
2. Use **security-reviewer** agent for assessment
3. Fix CRITICAL issues before continuing
4. Rotate any exposed secrets
5. Review entire codebase for similar issues
6. Document the incident and remediation
