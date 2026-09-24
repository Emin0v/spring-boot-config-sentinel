# Engineering Guide

- This repository is a small reusable Spring Boot library/starter.
- Prefer the smallest correct implementation.
- Do not introduce infrastructure or architectural layers without a concrete requirement.
- Keep public APIs small and stable.
- Follow the project's existing Java, build, formatting, and test conventions.
- Use Spring Boot native extension mechanisms before custom machinery.
- Avoid Lombok unless it provides clear value and is already part of the project conventions.
- Comments explain rationale, not obvious code.
- Add tests for behavioral changes.
- Do not mix unrelated refactors with feature work.
- Documentation must describe actual behavior; planned behavior must be labeled explicitly.
- Run the repository's verification command before every commit.
- Keep commits focused and use concise conventional commit messages.
