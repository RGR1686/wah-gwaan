# wah gwaan 🇧🇸 — Bahamas Events Platform

Monorepo for the **wah gwaan** Android events app and its data pipeline,
covering New Providence, Bahamas (rolling two-year window).

```
comprehensive_bahamas_scraper.py   ETL pipeline: 11 sources -> dedup -> feed
manual_events.json                 Hand-curated events (social-media-only promos)
scrape_and_publish.ps1             Daily scheduled scrape (Windows task, 6 AM)
.github/workflows/                 Cloud scrape + GitHub Pages feed publishing
wah_gwaan/                         Android app (Kotlin, Jetpack Compose)
```

## Data pipeline

11 sources with per-source error isolation: Ticket Flare (embedded Vue JSON),
ETickets Live, Eventbrite (`__SERVER_DATA__`), AllEvents.in, Bandsintown
(Playwright — Cloudflare-guarded), Songkick, Reggaeville, BahaEvents,
Bid Bahamas, BahamasLocal, and curated `manual_events.json`. Cross-source
fuzzy deduplication melts cross-listed events into single records.

```
pip install requests beautifulsoup4 lxml pandas python-dateutil openpyxl rapidfuzz playwright
playwright install chromium
python comprehensive_bahamas_scraper.py
```

Outputs: `New_Providence_Events.xlsx` (+ CSV) for humans,
`New_Providence_Events.json` (schema_version 1) as the app feed —
publish it as `events.json` on any static host.

## Android app

See [`wah_gwaan/README.md`](wah_gwaan/README.md) for the full architecture
map, build instructions, and v1 scope decisions. Highlights: offline-first
Room cache, polymorphic island/postal/admin LocationEngine (offline Bahamian
gazetteer), date + island + category + keyword filtering, calendar export
(Google / Outlook / .ics), saved-event reminders.

## Cloud feed (enable when ready)

Push this repo to GitHub, enable **Settings → Pages → Source: GitHub Actions**,
and `.github/workflows/scrape-and-publish.yml` scrapes every 6 hours and
publishes the feed to `https://<user>.github.io/<repo>/events.json`.
Then point `FEED_BASE_URL` in `wah_gwaan/app/build.gradle.kts` at it.
Note: Pages on a **private** repo requires a paid GitHub plan — either make
the repo public or use another static host.
