# Branch Workflow Comparison

## Your Questions Answered

### ❓ Question 1: Repository Branches
**"Does this repository have 3 branches: master, develop, release?"**

```
Current Status:
✅ master    - Yes, exists (production)
❌ develop   - NO, doesn't exist (needs to be created)
✅ release   - Yes, exists (triggers builds)
✅ preview   - Yes, exists (triggers preview builds)

Current Setup: master → release → preview
Recommended Setup: develop → release → master
```

---

### ❓ Question 2: Contributor Workflow
**"Contributors create branch from master and PR target to develop?"**

```
❌ WRONG:
 Contributor
      ↓
   Create from master
      ↓
   PR to develop

✅ RIGHT:
 Contributor
      ↓
   Create from develop
      ↓
   PR target to develop
      ↓
   [build_check.yml + test.yml run]
      ↓
   Reviewed by maintainer
      ↓
   Squash and merge to develop
```

**Why?**
- master = production (only releases go there)
- develop = integration (where features come together)
- Feature branches = temporary development work

---

### ❓ Question 3: Release Workflow
**"Author creates PR from develop to release to build APKs?"**

```
✅ CORRECT:

Maintainer (Release Manager):
1. Updates version in app/build.gradle.kts
2. Commits: "bump: version to 1.12.1"
3. Pushes to release branch

   git checkout develop
   git pull origin develop
   git checkout release
   git merge develop (or git reset --hard develop)
   git push origin release

Alternative (with PR):
   git checkout -b release-prep/1.12.1
   git merge develop
   [create PR develop → release]
   [merge commit, not squash]
   [push triggers build_push.yml]

Then:
   ✅ build_push.yml triggers automatically
   ✅ Extracts version from build.gradle.kts
   ✅ Validates tag doesn't exist
   ✅ Builds all APK variants
   ✅ Signs APKs with release key
   ✅ Generates changelog
   ✅ Creates GitHub release
   ✅ Publishes automatically
```

---

### ❓ Question 4: Merge Release Back to Master
**"Then merge release back to master?"**

```
✅ YES, THIS IS IMPORTANT:

After release is published:
   git checkout master
   git pull origin master
   git merge release
   git push origin master

This ensures:
   ✅ master = latest production release
   ✅ Easy to see release points
   ✅ Can revert entire releases if needed
   ✅ master and release stay in sync
```

---

### ❓ Question 5: Merge Commit vs Squash & Merge
**"Squash & merge or create merge commit?"**

```
RECOMMENDATION: Use different strategies for different branches

develop ← feature/fix branches:
   Use: SQUASH AND MERGE

   Before:
   feature branch has:
      - WIP: started
      - Add component
      - Fix typo
      - Address review

   After merge:
   develop has:
      - Add my new feature (one commit)

   ✅ Pros: Clean history, one logical unit per feature
   ✅ Easy to understand what changed
   ❌ Loses intermediate commit history

release ← develop:
   Use: MERGE COMMIT (or PR with merge commit)

   Before:
   develop has:
      - commit A (feature X)
      - commit B (feature Y)
      - commit C (bugfix Z)

   After merge:
   release has:
      - Merge pull request #456 from develop
        - commit A, B, C included
        - bump: version to 1.12.1

   ✅ Pros: Preserves all commit history
   ✅ Clear release merge point
   ✅ Easy to revert entire release
   ❌ Slightly more commits

master ← release:
   Use: MERGE COMMIT

   Same as release ← develop

   ✅ Pros: Clean separation of releases
   ✅ Easy to see release timeline
   ✅ Can revert one release without affecting others

FINAL STRATEGY:
┌─────────────────────────────────────────────┐
│ feature/* ──[squash]──→ develop             │
│ develop ───[merge]───→ release ──[merge]──→ master
└─────────────────────────────────────────────┘

Result: Clean feature history + clear release points
```

---

### ❓ Question 6: Branch Protection Settings
**"Help protect branches with best settings?"**

```
PROTECTED BRANCHES SETUP:

┌─ MASTER ───────────────────────────────────┐
│ ✅ Require PR before merge                  │
│ ✅ Require 1 approval                       │
│ ✅ Status checks: (none - from tested release)
│ ✅ Require up to date branches              │
│ ✅ Dismiss stale reviews                    │
│ Who can merge: Release manager only         │
│ Merge strategy: Merge commit                │
└─────────────────────────────────────────────┘

┌─ DEVELOP ──────────────────────────────────┐
│ ✅ Require PR before merge                  │
│ ✅ Require 1 approval                       │
│ ✅ Status checks REQUIRED:                  │
│    ├─ build_check.yml                      │
│    └─ test.yml                             │
│ ✅ Require up to date branches              │
│ ✅ Dismiss stale reviews                    │
│ Who can merge: Any maintainer               │
│ Merge strategy: Squash and merge (default)  │
└─────────────────────────────────────────────┘

┌─ RELEASE ──────────────────────────────────┐
│ ✅ Require PR before merge (optional)        │
│ ✅ Require 1 approval                       │
│ ✅ Status checks: (none - will test after)  │
│ ✅ Require up to date: No (controlled)       │
│ ✅ Restrict push: Only release manager      │
│ Who can push: Release manager only           │
│ Merge strategy: Merge commit                │
└─────────────────────────────────────────────┘

┌─ PREVIEW (Optional) ───────────────────────┐
│ ⚠️ Less strict, for testing                 │
│ Can allow direct pushes for quick preview   │
│ No PR required                              │
│ No approval required                        │
└─────────────────────────────────────────────┘
```

---

## Complete Workflow Diagram

```
Developer creates feature:

  git checkout develop
  git pull
  git checkout -b feature/my-feature
  [make changes]
  git commit -m "feat: add feature"
  git push -u origin feature/my-feature
           ↓
[GitHub] Create PR (target: develop)
           ↓
[CI] build_check.yml runs
[CI] test.yml runs
           ↓
[Maintainer] Reviews PR
[Maintainer] Approves
           ↓
[Maintainer] Clicks "Squash and Merge"
           ↓
Feature merged to develop ✅
           ↓

---

Release Manager creates release:

  git checkout develop
  git pull
  git checkout release
  git merge develop
  [update app/build.gradle.kts]
  git commit -m "bump: version to 1.12.1"
  git push origin release
           ↓
[GitHub Actions] build_push.yml triggers
           ↓
[build_push.yml]
  1. Extract version v1.12.1
  2. Validate tag doesn't exist
  3. Build all variants
  4. Sign APKs
  5. Generate changelog
  6. Create GitHub release
  7. Publish automatically
           ↓
Release published ✅
Download APKs from GitHub ✅
           ↓
[Release Manager] Sync with master:

  git checkout master
  git pull
  git merge release
  git push origin master
           ↓
master updated ✅
Version tagged (v1.12.1) ✅
```

---

## Key Decisions (Needs Your Input)

### Approval Requirements
- [ ] master: 1 approval (default) or 2 approvals (stricter)?
- [ ] develop: 1 approval (default) or 2 approvals (stricter)?
- [ ] release: 1 approval (default) or 0 (auto-release)?

### Release Manager
- [ ] Who is the release manager?
- [ ] Can only release manager push to `release`?
- [ ] Or any maintainer can release?

### Code Owners (Optional)
- [ ] Should specific files require specific person's approval?
- [ ] E.g., CI files, build config files?
- [ ] If yes: create CODEOWNERS file

### Merge Strategy Confirmation
- [ ] Use squash for develop PRs? ✅
- [ ] Use merge commit for release/master? ✅
- [ ] Allow rebase merging? (not recommended if using squash)

### Auto-delete Merged Branches
- [ ] Auto-delete head branches after merge? ✅ (recommended)
- [ ] Keep head branches? ❌ (messy)

---

## Action Items

1. **Create `develop` branch** (if ready)
   ```bash
   git checkout master
   git pull origin master
   git checkout -b develop
   git push -u origin develop
   ```

2. **Set up branch protection** (I can do via GitHub CLI)
   - Requires GitHub CLI auth: `gh auth login`

3. **Update contributor guide**
   - Point to develop, not master

4. **Notify team**
   - New workflow
   - New branch
   - How to contribute

---

## Related Files

- `docs/index.md` - Complete developer documentation
- `docs/WORKFLOW-AND-BRANCH-PROTECTION.md` - Detailed workflow guide
- `.github/workflows/build_push.yml` - Automated release workflow
- `.github/changelog-config.json` - Changelog generation config

---

**Ready to set this up? Let me know your preferences for the key decisions above!** 🚀
