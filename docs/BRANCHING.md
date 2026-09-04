# Git branching

Rot Client uses a long-lived integration branch and a stable release branch.
Day-to-day work never lands directly on `main`.

```text
main          --------●-------------------●------  stable, tested releases
                       \                 /
development   ---------●-----●-----●----●-------  integration
                          \     / \   /
feature/*                  ●---●   ●-●           one task per branch
```

## Branches

| Branch | Purpose |
| --- | --- |
| `main` | Stable, playtested code only. Public versioned GitHub Releases are cut from here. |
| `development` | Shared integration line. Feature work merges here. The playtest JAR publishes from here. |
| `feature/<short-name>` | One task, branched from `development`, merged back into `development`. |
| `fix/<short-name>` | Same as a feature branch, for bug fixes. |

`codex/qol-utilities` was the former default. Do not push new commits there.

## Daily work

1. Update `development`: `git fetch origin` and `git checkout development` then `git pull`.
2. Create a task branch: `git checkout -b feature/your-task`.
3. Keep the branch focused. Open a pull request **into `development`**.
4. After review and CI, merge the pull request. Delete the task branch.

Do not open feature pull requests against `main`.

## Stable release

Merge `development` into `main` only when that line is fully stable and tested
(automated tests green, and the intended Minecraft playtest done). Use a pull
request from `development` into `main`, then tag the versioned GitHub Release
from `main`.

Urgent production fixes may use `hotfix/<short-name>` branched from `main`.
Merge the hotfix into `main` and also back into `development` so the fix is not
lost.

## Playtest vs release

| Artifact | Source |
| --- | --- |
| [Latest playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest) pre-release | Green push to `development` |
| Versioned GitHub Release | Tag on `main` |

## First-time clone

```sh
git clone https://github.com/rot-tools/Rot-Client.git
cd Rot-Client
git checkout development
```

Existing clones that still track `codex/qol-utilities`:

```sh
git fetch origin
git checkout development
git branch -u origin/development development
```
