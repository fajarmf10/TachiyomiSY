# Git Workflow & Branch Protection Guide

## Current Repository Status

```
Remote Branches:
✅ master       - Production stable (exists)
❌ develop      - Development integration (needs to be created)
✅ release      - Triggers production builds (exists)
✅ preview      - Triggers preview builds (exists)

Feature Branches:
- feat/* - Feature development
- fix/* - Bug fixes
- chore/* - Maintenance
- hotfix/* - Critical fixes
```

---

## Recommended Git Workflow (Gitflow-style)

### Visual Flow

```
Contributors       Maintainer          Release Manager
     │                 │                      │
     ├─ Create branch  │                      │
     │   from develop  │                      │
     │       │         │                      │
     ├─ Make commits   │                      │
     │       │         │                      │
     ├─ Create PR ────▶│                      │
     │     target:    │                       │
     │   develop       │                      │
     │       │         │                      │
     │       ├─ Review │                      │
     │       │ & Test  │                      │
     │       │         │                      │
     │       ├─ Approve                       │
     │       │         │                      │
     └─ Merge ────────▶│                      │
           to develop   │                      │
                        │                      │
                        ├─ Prepare Release ──▶│
                        │ (update version)     │
                        │                      │
                        ├─ Create PR ────────▶│
                        │ develop → release    │
                        │                      │
                        │         ├─ Review & Approve
                        │         │
                        │         └─ Merge to release
                        │              │
                        │              ├─ build_push.yml triggers
                        │              ├─ Auto-generates changelog
                        │              ├─ Creates GitHub release
                        │              ├─ Auto-publishes APKs
                        │              │
                        │         ├─ Create PR ──┐
                        │         │ release → master
                        │         │              │
                        │         └─ Merge ─────▶│
                        │              to master  │
                        │                        │
                        └────────────────────────┘
```

---

## Detailed Workflow Steps

### **For Contributors**

#### 1. Create Feature Branch
```bash
# Always start from develop (or feature/*)
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/my-new-feature
# OR for bug fixes:
git checkout -b fix/bug-description
# OR for maintenance:
git checkout -b chore/update-something
```

#### 2. Make Changes & Commit
```bash
# Make your changes...
git add .
git commit -m "feat: add new feature

- Component X added
- Service Y updated
- Tests added"

# Or shorter form:
git commit -m "fix: resolve crash when loading X"
git commit -m "chore: update dependencies"
```

#### 3. Push & Create PR
```bash
git push -u origin feature/my-new-feature

# Via GitHub CLI:
gh pr create --title "Add new feature" --base develop

# Via GitHub UI:
# Visit: https://github.com/tachiyomisx/TachiyomiSY/pull/new/feature/my-new-feature
```

#### 4. What Happens Automatically
- ✅ `build_check.yml` runs (verify compile)
- ✅ `test.yml` runs (unit tests)
- ✅ Results appear as PR comments
- ✅ Branch protection rules check if enabled

#### 5. Address Feedback
```bash
# Fix issues locally
git add .
git commit -m "fix: address review feedback"
git push

# CI re-runs automatically on new commit
```

#### 6. Merge (Done by Maintainer)
- PR approved
- All checks pass
- Click "Squash and merge" button
- PR merged to `develop`

---

### **For Maintainers (Creating a Release)**

#### Step 1: Prepare Release Branch
```bash
# Ensure develop is up to date
git checkout develop
git pull origin develop

# Create or update release branch from develop
git checkout release
git pull origin release
git merge develop  # Bring latest develop into release
# or: git reset --hard origin/develop
```

#### Step 2: Update Version
```bash
# Edit app/build.gradle.kts
# Line 35: versionCode = 76 (increment by 1)
# Line 36: versionName = "1.12.1" (update version)

git add app/build.gradle.kts
git commit -m "bump: version to 1.12.1"
git push origin release
```

#### Step 3: Automated Release Process
- ✅ `build_push.yml` triggers
- ✅ Extracts version → v1.12.1 tag
- ✅ Validates tag doesn't exist
- ✅ Builds, tests, signs APKs
- ✅ Generates changelog from commits since last release
- ✅ Creates GitHub release
- ✅ Auto-publishes (not draft!)
- ✅ Watchers notified

#### Step 4: Sync Master with Release (IMPORTANT!)
```bash
# After release is published, bring release back to master
git checkout master
git pull origin master
git merge release  # Merge release into master
git push origin master

# This keeps master in sync with latest release
```

**Why?** Ensures master always points to latest production release.

---

## Branch Protection Rules

### **For `master` Branch**

**Require pull request before merging:**
- ✅ Require a pull request before merging
- ✅ Require approvals: **1 or 2 approvals**
- ✅ Require status checks to pass:
  - ❌ None needed (coming from release which already tested)
- ✅ Require branches to be up to date: **Yes**
- ✅ Require conversation resolution before merging: **Yes**
- ✅ Require signed commits: **No** (optional)
- ✅ Dismiss stale PR approvals: **Yes**
- ✅ Restrict who can push: **No** (maintainers only, trust them)
- ✅ Require status checks: **No** (master receives from tested release)

**Who can merge:**
- Release manager / Core maintainer only

---

### **For `develop` Branch**

**Require pull request before merging:**
- ✅ Require a pull request before merging
- ✅ Require approvals: **1 approval** (for large repos, 2)
- ✅ Require status checks to pass:
  - ✅ `build_check` - must pass
  - ✅ `test` - must pass
- ✅ Require branches to be up to date: **Yes**
- ✅ Require conversation resolution before merging: **Yes**
- ✅ Require signed commits: **No**
- ✅ Dismiss stale PR approvals: **Yes**
- ✅ Restrict who can push: **No** (developers can push if not PR)
- ✅ Require status checks: **Yes**

**Who can merge:**
- Any maintainer

**Merge strategy:**
- Allow squash merging: **Yes** (for cleaner history)
- Allow merge commits: **Yes** (for flexibility)
- Allow rebase merging: **Yes** (for linear history)
- Default: **Squash and merge** (recommended)

---

### **For `release` Branch**

**Require pull request before merging:**
- ✅ Require a pull request before merging
- ✅ Require approvals: **1 approval** (code owner only)
- ✅ Require status checks to pass:
  - ❌ None (will build after push, not before)
- ✅ Require branches to be up to date: **No** (controlled push)
- ✅ Require conversation resolution before merging: **Yes**
- ✅ Require signed commits: **No**
- ✅ Dismiss stale PR approvals: **Yes**
- ✅ Restrict who can push: **Yes** - Only maintainers/release manager
- ✅ Require status checks: **No**

**Who can push:**
- Release manager only
- Alternative: Allow push directly (trigger build)

**Merge strategy:**
- Allow merge commits: **Yes**
- Squash: **No** (preserve commit history)
- Rebase: **No**

---

### **For `preview` Branch**

**Require pull request before merging:**
- ⚠️ Optional (can be more relaxed)

**If you want to protect it:**
- ✅ Require a pull request before merging
- ✅ Require approvals: **0 approvals** (auto-test)
- ✅ Require status checks: **No** (will test after push)
- ✅ Require branches to be up to date: **No** (preview can force)

**Who can push:**
- Developers testing new features
- Can push directly or via PR

---

## Merge Strategies Explained

### **Squash and Merge** (Recommended for develop)
```
Before:
feature/my-feature commits:
  - WIP: started feature
  - Add component
  - Fix typo
  - Address review

After merge to develop:
  - [develop] Add my feature

✅ Pros: Clean history, one logical commit per feature
❌ Cons: Loses intermediate commit history
```

**Use for:** Feature branches → develop (cleaner history)

---

### **Merge Commit** (Recommended for release → master)
```
Before:
release branch has:
  - bump: version to 1.12.1
  - All prior feature commits

After merge to master:
  - [master] Merge pull request #123 from release
    - bump: version to 1.12.1
    - All commits included

✅ Pros: Preserves branch history, clear merge points, easy to revert
❌ Cons: Slightly more commits in history
```

**Use for:** release → master (preserve release points)

---

### **Rebase and Merge** (Alternative for develop)
```
Rebases all commits on top of develop, then fast-forwards

✅ Pros: Linear history, no merge commits
❌ Cons: Rewrites history, can be confusing for teams

Not recommended if using squash elsewhere (inconsistent)
```

---

## Setting Up Branch Protection on GitHub

### Method 1: GitHub Web UI

1. Go to: **Settings → Branches**
2. Click **"Add rule"** under Branch protection rules
3. For branch name pattern: enter `master`, `develop`, or `release`
4. Configure settings as recommended above
5. Click **"Create"**

### Method 2: GitHub CLI

```bash
# Protect master
gh api repos/tachiyomisx/TachiyomiSY/branches/master/protection \
  -X PUT \
  -f required_status_checks=null \
  -f enforce_admins=true \
  -f required_pull_request_reviews='{"dismissal_restrictions": null, "dismiss_stale_reviews": true, "require_code_owner_reviews": false, "required_approving_review_count": 1}' \
  -f restrictions=null

# Protect develop
gh api repos/tachiyomisx/TachiyomiSY/branches/develop/protection \
  -X PUT \
  -f required_status_checks='{"strict": true, "contexts": ["build_check", "test"]}' \
  -f enforce_admins=true \
  -f required_pull_request_reviews='{"dismissal_restrictions": null, "dismiss_stale_reviews": true, "require_code_owner_reviews": false, "required_approving_review_count": 1}' \
  -f restrictions=null

# Protect release
gh api repos/tachiyomisx/TachiyomiSY/branches/release/protection \
  -X PUT \
  -f required_status_checks=null \
  -f enforce_admins=true \
  -f required_pull_request_reviews='{"dismissal_restrictions": null, "dismiss_stale_reviews": true, "require_code_owner_reviews": false, "required_approving_review_count": 1}' \
  -f restrictions=null
```

---

## Create `develop` Branch

Since develop doesn't exist yet, create it:

```bash
# Create develop from master
git checkout master
git pull origin master
git checkout -b develop
git push -u origin develop

# Now you have:
# - master (production)
# - develop (integration)
# - release (testing, then tag)
# - preview (preview builds)
```

---

## Summary: 3 Main Branches → 4 Branch Workflow

### **Before (Current)**
```
master → release → preview
```

### **After (Recommended)**
```
feature/* ────┐
fix/*         ├─→ develop ──→ release ──→ master
chore/*       │
hotfix/* ─────┘
               └─→ preview (separate, can be updated independently)
```

### **The Flow**
1. **Contributors** create feature/fix branches from `develop`
2. **PR targets `develop`** (CI runs, approved, squash-merged)
3. **Maintainer** prepares release: merges develop → release
4. **Release manager** updates version, pushes to release
5. **build_push.yml** triggers: builds, tests, signs, publishes
6. **After release** published: merge release → master
7. **Master** always = latest production release

### **PR Merge Strategy**
- **develop** ← feature/fix: **Squash and merge** (clean history)
- **release** ← develop: **Merge commit** (preserve release point)
- **master** ← release: **Merge commit** (preserve release point)

---

## Questions to Decide

1. **Approval Requirements**
   - master: 1 or 2 approvals?
   - develop: 1 approval?
   - release: 1 approval?

2. **Who Can Merge**
   - Everyone?
   - Only admins?
   - Code owners?

3. **Who Can Push to Release**
   - Only release manager?
   - Any maintainer?
   - Direct push or PR only?

4. **Code Owners** (Optional)
   - Should certain files require specific person's approval?
   - E.g., CI files need CI expert approval?

5. **Signed Commits**
   - Require GPG signatures?
   - Recommended but not required?

---

## Checklist

- [ ] Create `develop` branch from `master`
- [ ] Set up branch protection for `master`
- [ ] Set up branch protection for `develop`
- [ ] Set up branch protection for `release`
- [ ] Configure default merge strategy (squash for develop)
- [ ] Add code owners (optional, via CODEOWNERS file)
- [ ] Update contributor documentation
- [ ] Announce workflow to team
- [ ] Set up branch deletion rules (auto-delete merged branches)
- [ ] Test workflow with first PR

---

## Next Steps

1. **Tell me your preferences** for approval requirements and merge strategies
2. **I'll create/update the `develop` branch**
3. **I'll set up branch protection** via GitHub CLI
4. **I'll update documentation** with your team's specific workflow
