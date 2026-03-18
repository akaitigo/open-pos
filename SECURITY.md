# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| 0.1.x   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in open-pos, please report it responsibly.

**DO NOT open a public GitHub issue for security vulnerabilities.**

Use [GitHub's private vulnerability reporting](https://github.com/akaitigo/open-pos/security/advisories/new).

This is currently the only supported private reporting channel for security issues.

### What to include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### Response timeline

- **Acknowledgment**: Within 48 hours
- **Initial assessment**: Within 7 days
- **Fix or mitigation**: Depends on severity, typically within 30 days

### Disclosure policy

- We follow [coordinated disclosure](https://en.wikipedia.org/wiki/Coordinated_vulnerability_disclosure).
- We will credit reporters unless they prefer to remain anonymous.
- We will publish a security advisory once a fix is available.

## Current Security Status

Authentication and authorization are implemented via ORY Hydra v2.2 (OIDC/PKCE) with role-based access control (Owner / Manager / Cashier). REST API endpoints enforce JWT validation and tenant isolation through the api-gateway.

See [README.md](README.md) for the current development status.
