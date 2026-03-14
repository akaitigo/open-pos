# Support

Use this guide to decide where to ask for help or report a problem.

## Before Opening Anything

1. Read [README.md](README.md) and [docs/README.md](docs/README.md).
2. Run `make doctor` to confirm your local prerequisites.
3. If you are working on the demo flow, run `make local-demo` or `make docker-demo` first.

## Where to Report What

- **Confirmed bug**: open a GitHub issue with the bug template.
- **Feature request**: open a GitHub issue with the feature template.
- **Setup or usage problem**: start a [GitHub Discussion](https://github.com/akaitigo/open-pos/discussions) for questions or unclear behavior, and include the output of `make doctor`, the command you ran, and the exact error.
- **Security vulnerability**: do **not** open a public issue. Use [GitHub private vulnerability reporting](https://github.com/akaitigo/open-pos/security/advisories/new) or email `security@openpos.dev`.

## What to Include

- Your OS and version
- `java -version`
- `node -v`
- `pnpm -v`
- The command you ran
- The exact error output
- Whether you used `make local-demo` or `make docker-demo`

## Response Expectations

- This is currently a maintainer-led project with best-effort support.
- Security reports follow the SLA in [SECURITY.md](SECURITY.md).
- Discussions, bug reports, and contribution questions are handled asynchronously through GitHub.
