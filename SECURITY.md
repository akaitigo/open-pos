# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly.

**DO NOT open a public GitHub issue for security vulnerabilities.**

Instead, please send a report via [GitHub Security Advisories](https://github.com/akaitigo/open-pos/security/advisories/new).

Include the following in your report:

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

We will acknowledge receipt within 48 hours and aim to provide a fix within 7 days for critical issues.

## Supported Versions

| Version | Supported |
|---------|-----------|
| 0.x     | Yes       |

## Security Best Practices

This project follows these security practices:

- No hardcoded credentials in source code
- Environment variables for all secrets
- Dependency updates via Dependabot
- GitHub Actions pinned to commit SHAs
- Multi-tenant isolation via Hibernate Filters and gRPC metadata
