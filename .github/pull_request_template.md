## Summary

<!-- What changed, and why. -->

## Target branch

- Feature and fix work targets **`development`**, not `main`.
- `main` is for a fully stable, tested merge from `development` (or a `hotfix/*`).

See [docs/BRANCHING.md](../docs/BRANCHING.md).

## Test plan

- [ ] `./gradlew test --rerun-tasks`
- [ ] `./gradlew compileClientJava` when client code changed
- [ ] Minecraft playtest if this changes in-game behavior
