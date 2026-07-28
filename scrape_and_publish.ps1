# =============================================================================
# scrape_and_publish.ps1 — daily automated scrape for the wah gwaan app
#
# Runs the full aggregation pipeline, then publishes the fresh feed:
#   1. -> android-dev\feed\events.json   (local device testing via adb reverse)
#   2. -> optional: push to your CDN/static host (uncomment one option below)
#
# Registered as Windows scheduled task "WahGwaanFeedScrape" (daily 06:00).
# Run manually any time:  pwsh -NoProfile -File scrape_and_publish.ps1
# =============================================================================
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here

$log = Join-Path $here "scrape_and_publish.log"
"=== Scrape started $(Get-Date -Format o) ===" | Add-Content $log

try {
    python "$here\comprehensive_bahamas_scraper.py" --delay 1.5 2>&1 |
        Add-Content $log

    $feedSrc = Join-Path $here "New_Providence_Events.json"
    if (-not (Test-Path $feedSrc)) { throw "Feed JSON not produced" }

    # 1. Local device-testing feed
    $localFeed = "C:\Users\rhanrichardson\android-dev\feed"
    New-Item -ItemType Directory -Force $localFeed | Out-Null
    Copy-Item $feedSrc (Join-Path $localFeed "events.json") -Force
    "Published to $localFeed\events.json" | Add-Content $log

    # 2. OPTIONAL cloud publish — uncomment ONE when hosting is set up:
    # -- GitHub Pages (repo with gh-pages serving /feed):
    # gh api -X PUT "repos/<owner>/<repo>/contents/feed/events.json" `
    #   -f message="feed: $(Get-Date -Format yyyy-MM-dd)" `
    #   -f content="$([Convert]::ToBase64String([IO.File]::ReadAllBytes($feedSrc)))" `
    #   -f sha="$(gh api repos/<owner>/<repo>/contents/feed/events.json --jq .sha)"
    # -- Any host reachable by SCP/rclone/az/aws CLI works equally well here.

    "=== Scrape finished OK $(Get-Date -Format o) ===" | Add-Content $log
} catch {
    "=== Scrape FAILED: $_ ===" | Add-Content $log
    throw
}
