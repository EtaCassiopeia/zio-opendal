# Release Process

This document outlines the release process for ZIO OpenDAL, including versioning strategy, release preparation, and publishing procedures.

## Table of Contents

- [Versioning Strategy](#versioning-strategy)
- [Release Types](#release-types)
- [Pre-Release Checklist](#pre-release-checklist)
- [Release Process](#release-process)
- [Publishing](#publishing)
- [Post-Release](#post-release)
- [Hotfix Releases](#hotfix-releases)
- [Release Automation](#release-automation)

## Versioning Strategy

ZIO OpenDAL follows [Semantic Versioning (SemVer) 2.0.0](https://semver.org/):

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
```

### Version Components

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality in a backwards-compatible manner
- **PATCH**: Backwards-compatible bug fixes
- **PRERELEASE**: Optional pre-release identifiers (alpha, beta, rc)
- **BUILD**: Optional build metadata

### Examples

```
1.0.0          # Initial stable release
1.0.1          # Patch release with bug fixes
1.1.0          # Minor release with new features
2.0.0          # Major release with breaking changes
1.1.0-alpha.1  # Pre-release version
1.1.0-beta.1   # Beta pre-release
1.1.0-rc.1     # Release candidate
```

### Version Compatibility

- **Patch releases** (1.0.0 → 1.0.1): Fully backward compatible
- **Minor releases** (1.0.0 → 1.1.0): Source compatible, may add new APIs
- **Major releases** (1.0.0 → 2.0.0): May introduce breaking changes

### Scala Version Compatibility

ZIO OpenDAL supports multiple Scala versions:

- **Scala 2.13.x**: Latest patch version
- **Scala 3.3.x**: Latest LTS version  
- **Scala 3.4.x**: Latest stable version
- **Scala 3.5.x**: Latest stable version

## Release Types

### 1. Development Releases

- **Purpose**: Regular development snapshots
- **Versioning**: `MAJOR.MINOR.PATCH-SNAPSHOT`
- **Publishing**: Snapshots to Sonatype snapshot repository
- **Frequency**: Automatic on every commit to main branch

### 2. Pre-Releases

- **Alpha**: Early development versions with potential breaking changes
- **Beta**: Feature-complete versions undergoing testing
- **Release Candidate (RC)**: Near-final versions for final validation

### 3. Stable Releases

- **Patch Releases**: Bug fixes and minor improvements
- **Minor Releases**: New features and enhancements
- **Major Releases**: Significant changes and breaking changes

## Pre-Release Checklist

### Code Quality

- [ ] All tests pass on all supported Scala versions
- [ ] Code coverage meets minimum threshold (80%+)
- [ ] No critical security vulnerabilities
- [ ] Performance benchmarks show no regressions
- [ ] All examples compile and run successfully
- [ ] Integration tests pass with real storage backends

### Documentation

- [ ] CHANGELOG.md updated with all changes
- [ ] README.md updated if needed
- [ ] API documentation is current
- [ ] Migration guide prepared for breaking changes
- [ ] Examples updated for new features

### Dependencies

- [ ] All dependencies are up to date
- [ ] No SNAPSHOT dependencies in release
- [ ] Dependency compatibility verified
- [ ] License compatibility checked

### Testing

- [ ] Cross-compilation testing completed
- [ ] Integration testing with real backends
- [ ] Performance testing completed
- [ ] Memory leak testing completed
- [ ] Example applications tested

## Release Process

### 1. Prepare Release Branch

```bash
# Create release branch from main
git checkout main
git pull origin main
git checkout -b release/v1.2.0

# Update version in build.sbt
# version := "1.2.0"
```

### 2. Update Documentation

```bash
# Update CHANGELOG.md
# Add release date and final version
# Review all changes since last release

# Update README.md if needed
# Update version numbers in examples
# Update compatibility matrices

# Generate API documentation
sbt doc
```

### 3. Final Testing

```bash
# Run full test suite
sbt clean +test +it:test

# Test examples
sbt "project examples" "+compile" "+test"

# Run specific test suites
sbt core/test native/test examples/test

# Cross-compile for all supported versions
sbt +publishLocal
```

### 4. Create Release Commit

```bash
# Stage all changes
git add .

# Create release commit
git commit -m "chore: prepare release v1.2.0

- Update version to 1.2.0
- Update CHANGELOG.md
- Update documentation"

# Push release branch
git push origin release/v1.2.0
```

### 5. Create Pull Request

Create a pull request from the release branch to main:

- **Title**: `chore: prepare release v1.2.0`
- **Description**: Include changelog and testing details
- **Reviewers**: All maintainers
- **Labels**: `release`, `chore`

### 6. Review and Merge

- Wait for all CI checks to pass
- Obtain approvals from maintainers
- Merge the pull request using "Merge commit" strategy

### 7. Tag and Release

```bash
# Switch to main and pull latest
git checkout main
git pull origin main

# Create signed tag
git tag -s v1.2.0 -m "Release version 1.2.0

Major changes:
- Added Google Cloud Storage support
- Improved error handling
- Performance optimizations

See CHANGELOG.md for complete details."

# Push tag
git push origin v1.2.0
```

## Publishing

### Prerequisites

Before publishing, ensure the following setup is complete:

#### Required GitHub Secrets

The following secrets must be configured in your GitHub repository:

- **`SONATYPE_USERNAME`**: Your Sonatype username
- **`SONATYPE_PASSWORD`**: Your Sonatype password
- **`PGP_PRIVATE_KEY`**: The content of your private key file (exported as ASCII armor)
- **`PGP_PASSPHRASE`**: The passphrase used when generating the PGP key

#### PGP Key Setup

If you need to generate a new PGP key pair for signing artifacts:

```bash
# Generate a new key pair
gpg --gen-key

# List your keys to find the key ID
gpg --list-secret-keys --keyid-format LONG

# Export your private key (for GitHub secrets)
gpg --export-secret-keys --armor YOUR_KEY_ID > private-key.asc

# Export your public key
gpg --export --armor YOUR_KEY_ID > public-key.asc

# Upload to keyservers for verification
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver pgp.mit.edu --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

#### Sonatype Account Setup

1. Create account at [Sonatype Central Portal](https://central.sonatype.org/)
2. Navigate to "View Account" to get your credentials
3. For the `io.github.etacassiopeia` namespace:
   - Go to "Namespaces" section
   - Click "Add Namespace"
   - Enter `io.github.etacassiopeia`
   - Complete verification process (GitHub repository ownership)

### 1. Sonatype Publishing

```bash
# Publish to Sonatype staging
sbt +publishSigned

# Close and release staging repository
# This can be done via Sonatype web interface or sbt-sonatype plugin
sbt sonatypeBundleRelease
```

#### Manual Publishing (if needed)

```bash
# Set environment variables
export SONATYPE_USERNAME="your-username"
export SONATYPE_PASSWORD="your-password"
export PGP_PASSPHRASE="your-pgp-passphrase"

# Publish snapshots
sbt +publishSigned

# For releases (after creating a git tag)
sbt +publishSigned sonatypeBundleRelease
```

### 2. GitHub Release

Create GitHub release from tag:

1. Go to GitHub releases page
2. Click "Create a new release"
3. Select the tag `v1.2.0`
4. Set release title: `ZIO OpenDAL v1.2.0`
5. Add release notes from CHANGELOG.md
6. Mark as pre-release if applicable
7. Publish release

### 3. Documentation Publishing

```bash
# Build and deploy documentation (if using GitHub Pages)
sbt docs/publishMicrosite

# Or manually deploy to documentation hosting
```

### Release Notes Template

```markdown
## ZIO OpenDAL v1.2.0

### New Features
- Added Google Cloud Storage backend support (#123)
- Introduced syntax extensions for easier API usage (#145)
- Added retry configuration options (#167)

### Improvements
- Performance optimizations for large file operations (#134)
- Better error messages and error handling (#156)
- Improved memory usage in batch operations (#189)

### Bug Fixes
- Fixed metadata handling for Azure Blob Storage (#178)
- Resolved memory leak in long-running operations (#192)
- Fixed path handling edge cases (#203)

### Breaking Changes
- `OpenDALError` hierarchy has been restructured (#145)
  - Migration: Update error handling code to use new error types
- Deprecated `OpenDAL.createLayer` method (#167)
  - Migration: Use `OpenDAL.live()` instead

### Dependencies
- Updated ZIO to 2.0.19
- Updated OpenDAL native libraries to v0.45.1

### Supported Scala Versions
- Scala 2.13.12
- Scala 3.3.1
- Scala 3.4.0

### Installation
```scala
libraryDependencies += "dev.zio" %% "zio-opendal" % "1.2.0"
```

Full changelog: https://github.com/your-org/zio-opendal/compare/v1.1.0...v1.2.0
```

## Post-Release

### 1. Verify Release

```bash
# Wait for artifacts to sync to Maven Central (can take 2+ hours)
# Test installation from Maven Central
sbt clean
sbt update
```

### 2. Update Development Version

```bash
# Create branch for version bump
git checkout -b chore/bump-version

# Update version to next snapshot
# version := "1.2.1-SNAPSHOT"

# Commit and push
git commit -am "chore: bump version to 1.2.1-SNAPSHOT"
git push origin chore/bump-version

# Create PR and merge
```

### 3. Announcements

- [ ] Update project README with latest version
- [ ] Announce on GitHub Discussions
- [ ] Post to relevant community channels (Discord, Gitter, etc.)
- [ ] Update documentation site
- [ ] Tweet announcement (if applicable)

### 4. Monitor Release

- [ ] Check Maven Central for artifact availability
- [ ] Monitor GitHub issues for release-related problems
- [ ] Watch CI builds for any immediate issues
- [ ] Update any downstream projects

## Hotfix Releases

For critical bugs that need immediate fixes:

### 1. Create Hotfix Branch

```bash
# Branch from the release tag, not main
git checkout v1.2.0
git checkout -b hotfix/v1.2.1

# Make minimal fix
# Update version to 1.2.1
```

### 2. Fast-Track Process

- Skip non-essential checks
- Focus on the specific bug fix
- Minimal testing scope
- Expedited review process

### 3. Release Immediately

```bash
# Tag and release
git tag v1.2.1
git push origin v1.2.1

# Publish to Maven Central
sbt +publishSigned sonatypeBundleRelease

# Create GitHub release
```

### 4. Backport to Main

```bash
# Cherry-pick to main branch
git checkout main
git cherry-pick <hotfix-commit>
git push origin main
```

## Release Automation

### GitHub Actions Workflow

The project uses GitHub Actions for automated releases:

```yaml
# .github/workflows/release.yml
name: Release
on:
  push:
    tags: ['v*']
  
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v3
        
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          
      - name: Cache SBT
        uses: actions/cache@v3
        with:
          path: |
            ~/.sbt
            ~/.coursier
          key: ${{ runner.os }}-sbt-${{ hashFiles('**/*.sbt') }}
          
      - name: Run Tests
        run: sbt +test
        
      - name: Publish
        run: sbt +publishSigned sonatypeBundleRelease
        env:
          SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
          SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
          PGP_SECRET: ${{ secrets.PGP_SECRET }}
          
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          generate_release_notes: true
          files: |
            target/scala-*/zio-opendal_*-*.jar
```

### Automated Checks

- [ ] All tests pass
- [ ] Cross-compilation successful
- [ ] No SNAPSHOT dependencies
- [ ] Valid semantic version
- [ ] Signed commits (optional)
- [ ] GPG signature verification

### Release Dashboard

Monitor releases using:
- GitHub Actions dashboard
- Sonatype repository status
- Maven Central sync status
- Download statistics

## Troubleshooting

### Common Release Issues

1. **Maven Central Sync Delays**
   - Wait 2-10 hours for sync
   - Check Sonatype OSSRH status

2. **PGP Key Issues**
   - Ensure the private key is correctly formatted in the secret
   - Verify the passphrase is correct
   - Make sure the public key is uploaded to keyservers
   - Check that GPG key is available in CI environment

3. **Sonatype Authentication**
   - Verify username and password are correct
   - Check that your account has permission for the namespace
   - Ensure namespace verification is complete

4. **Binary Compatibility**
   - Use `sbt mimaReportBinaryIssues` to check compatibility
   - The CI will fail if there are breaking changes without version bump
   - Review breaking changes and update major version if needed

5. **Cross-Compilation Failures**
   - Check Scala version compatibility
   - Verify all dependencies support target versions
   - Review compiler settings
   - Ensure all cross-compilation targets are properly configured

6. **Scala Version Issues**
   - Ensure all cross-compilation targets are properly configured
   - Check that dependencies are available for all Scala versions
   - Verify compatibility matrices

7. **Test Failures**
   - Run tests locally before release
   - Check for flaky tests
   - Verify integration test setup

### Manual Verification

Before releasing, you can verify the build locally:

```bash
# Test cross-compilation
sbt +test

# Check formatting
sbt scalafmtCheckAll

# Check binary compatibility  
sbt mimaReportBinaryIssues

# Test publishing (without actual upload)
sbt +publishLocal
```

### Security Notes

- Never commit PGP keys or credentials to the repository
- Use GitHub's encrypted secrets for all sensitive information
- Regularly rotate your Sonatype password
- Consider using token-based authentication when available
- The PGP key should be dedicated to this project (don't reuse personal keys)

### Emergency Procedures

If a bad release is published:

1. **Yank from Maven Central** (if possible)
2. **Create hotfix release** immediately
3. **Notify community** about the issue
4. **Document the problem** and resolution

Remember: Once published to Maven Central, artifacts cannot be deleted, only marked as deprecated.

## Resources

- [Sonatype Central Portal](https://central.sonatype.org/)
- [sbt-sonatype plugin documentation](https://github.com/xerial/sbt-sonatype)
- [sbt-pgp plugin documentation](https://github.com/sbt/sbt-pgp)
- [Maven Central requirements](https://central.sonatype.org/publish/requirements/)
- [Semantic Versioning specification](https://semver.org/)
