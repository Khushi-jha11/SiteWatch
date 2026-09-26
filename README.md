# SiteWatch

**Automated website health monitor** built with Selenium, TestNG and Maven. A scheduled GitHub Actions workflow runs the suite, records the results, and publishes a live analytics dashboard to GitHub Pages — no server required.

🔗 **Live dashboard:** `https://YOUR_USERNAME.github.io/SiteWatch/` (enable GitHub Pages, see setup below)

## What it checks

For every site listed in [`src/main/resources/sites.json`](src/main/resources/sites.json), SiteWatch runs three independent checks:

| Check | What it verifies |
|---|---|
| **Page load** | The page loads, the title matches what's expected, and it loads within a time budget |
| **Key elements** | A set of required CSS selectors (nav, search box, login form...) are present |
| **Broken links** | Every link on the page is requested in parallel; too many 4xx/5xx responses fails the check |

Every run appends a row per site to [`docs/data/history.csv`](docs/data/history.csv), and a Python script turns that history into the dashboard (`docs/index.html`): current status per site, a load-time trend line, a failure-rate chart, and a broken-links breakdown. Failed checks also save a screenshot to `docs/screenshots/`.

## Tech stack

- **Selenium 4** — browser automation (navigation, elements, JS executor, screenshots)
- **TestNG** — annotations, `@DataProvider` for data-driven sites, groups (`smoke`, `regression`, `links`, `performance`), group dependencies, parallel execution, a custom `ITestListener`
- **Maven** — `pom.xml`, Surefire plugin (`mvn test` → `testng.xml`), Shade plugin (runnable fat JAR)
- **Java 17** `HttpClient` — concurrent link checking
- **Jackson** — reads `sites.json` into typed config objects
- **Python + pandas + Plotly** — turns the CSV history into the dashboard
- **GitHub Actions** — scheduled runs, headless Chrome, commits the dashboard back, deploys to Pages

## Project structure

```
SiteWatch/
├── .github/workflows/monitor.yml     # scheduled CI: run tests → build dashboard → deploy
├── pom.xml
├── testng.xml
├── src/main/java/com/sitewatch/
│   ├── SiteWatchCli.java             # runnable fat-JAR entry point (no Maven/TestNG needed)
│   ├── base/                         # DriverFactory, BasePage
│   ├── config/                       # ConfigReader, SiteConfig (sites.json model)
│   ├── core/                         # SiteChecker: the 3 checks, shared by tests and the CLI
│   ├── pages/                        # GenericPage (Page Object Model)
│   └── utils/                        # BrokenLinkChecker, ScreenshotUtil, ResultRecorder, SiteResult
├── src/main/resources/
│   ├── config.properties             # browser, timeouts, thresholds
│   └── sites.json                    # the list of sites to monitor (edit this to add your own)
├── src/test/java/com/sitewatch/tests/
│   ├── BaseTest.java                 # shared setup/teardown + @DataProvider
│   ├── PageLoadTest.java / ElementTest.java / LinkTest.java
│   └── ResultsListener.java          # flushes results to CSV when the suite finishes
├── scripts/build_dashboard.py        # CSV → docs/index.html
└── docs/                             # served by GitHub Pages
    ├── index.html
    ├── data/history.csv
    └── screenshots/
```

## Run it locally

**Prerequisites:** Java 17+, Maven, Google Chrome, Python 3.10+.

```bash
git clone https://github.com/YOUR_USERNAME/SiteWatch.git
cd SiteWatch

# Run the Selenium/TestNG suite (headless by default)
mvn clean test

# Turn the results into a dashboard
pip install -r requirements.txt
python scripts/build_dashboard.py
open docs/index.html   # or just double-click it
```

Watch it run with a visible browser:

```bash
mvn clean test -Dheadless=false
```

Run only smoke tests, or switch browsers:

```bash
mvn clean test -Dgroups=smoke
mvn clean test -Dbrowser=firefox
```

### Run the packaged JAR (no Maven/TestNG needed)

```bash
mvn clean package -DskipTests
java -jar target/sitewatch-1.0.0-fat.jar
# or point it at your own site list:
java -jar target/sitewatch-1.0.0-fat.jar path/to/my-sites.json
```

## Add your own sites

Edit `src/main/resources/sites.json`:

```json
{
  "name": "My Portfolio",
  "url": "https://khushi-jha11.github.io/SiteWatch/",
  "expectedTitle": "Your Name",
  "requiredSelectors": ["nav", "footer", "#contact-form"]
}
```

Tune thresholds in `src/main/resources/config.properties` (max load time, link timeout, max broken-link percentage).

## Deploy this yourself

1. **Fork or push this repo** to your own GitHub account.
2. **Settings → Pages** → set Source to **GitHub Actions**.
3. **Settings → Actions → General** → under Workflow permissions, choose **Read and write permissions** (lets the workflow commit the dashboard back and deploy it).
4. Push to `main`, or trigger the workflow manually from the **Actions** tab (`workflow_dispatch`). It also runs automatically every day at 06:00 UTC — edit the `cron` line in `.github/workflows/monitor.yml` to change that.
5. Your dashboard goes live at `https://YOUR_USERNAME.github.io/SiteWatch/`.

## Roadmap ideas

- Slack/email alert when a site fails
- Retry analyzer for flaky checks
- Multi-browser matrix (Chrome + Firefox) in CI
- Docker image for fully self-contained runs
